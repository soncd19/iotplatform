package org.iotp.analytics.ruleengine.core.processor;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.script.Bindings;
import javax.script.ScriptException;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.iotp.analytics.ruleengine.annotation.Processor;
import org.iotp.analytics.ruleengine.api.rules.RuleContext;
import org.iotp.analytics.ruleengine.api.rules.RuleException;
import org.iotp.analytics.ruleengine.api.rules.RuleProcessingMetaData;
import org.iotp.analytics.ruleengine.api.rules.RuleProcessor;
import org.iotp.analytics.ruleengine.common.msg.core.TelemetryUploadRequest;
import org.iotp.analytics.ruleengine.common.msg.core.UpdateAttributesRequest;
import org.iotp.analytics.ruleengine.common.msg.device.ToDeviceActorMsg;
import org.iotp.analytics.ruleengine.common.msg.session.FromDeviceMsg;
import org.iotp.analytics.ruleengine.core.filter.NashornJsEvaluator;
import org.iotp.analytics.ruleengine.core.utils.VelocityUtils;
import org.iotp.infomgt.data.alarm.Alarm;
import org.iotp.infomgt.data.alarm.AlarmSeverity;
import org.iotp.infomgt.data.alarm.AlarmStatus;
import org.iotp.infomgt.data.kv.KvEntry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 */
@Processor(name = "Alarm Processor", descriptor = "AlarmProcessorDescriptor.json", configuration = AlarmProcessorConfiguration.class)
@Slf4j
public class AlarmProcessor implements RuleProcessor<AlarmProcessorConfiguration> {

  static final String IS_NEW_ALARM = "isNewAlarm";
  static final String IS_EXISTING_ALARM = "isExistingAlarm";
  static final String IS_CLEARED_ALARM = "isClearedAlarm";

  protected NashornJsEvaluator newAlarmEvaluator;
  protected NashornJsEvaluator clearAlarmEvaluator;

  private ObjectMapper mapper = new ObjectMapper();
  private AlarmProcessorConfiguration configuration;
  private AlarmStatus status;
  private AlarmSeverity severity;
  private Template alarmTypeTemplate;
  private Template alarmDetailsTemplate;

  @Override
  public void init(AlarmProcessorConfiguration configuration) {
    this.configuration = configuration;
    try {
      this.alarmTypeTemplate = VelocityUtils.create(configuration.getAlarmTypeTemplate(), "Alarm Type Template");
      this.alarmDetailsTemplate = VelocityUtils.create(configuration.getAlarmDetailsTemplate(),
          "Alarm Details Template");
      this.status = AlarmStatus.valueOf(configuration.getAlarmStatus());
      this.severity = AlarmSeverity.valueOf(configuration.getAlarmSeverity());
      initEvaluators();
    } catch (Exception e) {
      log.error("Failed to create templates based on provided configuration!", e);
      throw new RuntimeException("Failed to create templates based on provided configuration!", e);
    }
  }

  @Override
  public void resume() {
    initEvaluators();
    log.debug("Resume method was called, but no impl provided!");
  }

  @Override
  public void suspend() {
    destroyEvaluators();
    log.debug("Suspend method was called, but no impl provided!");
  }

  @Override
  public void stop() {
    destroyEvaluators();
    log.debug("Stop method was called, but no impl provided!");
  }

  @Override
  public RuleProcessingMetaData process(RuleContext ctx, ToDeviceActorMsg wrapper) throws RuleException {
    RuleProcessingMetaData md = new RuleProcessingMetaData();

    FromDeviceMsg msg = wrapper.getPayload();
    Bindings bindings = buildBindings(ctx, msg);

    boolean isActiveAlarm;
    boolean isClearedAlarm;

    try {
      isActiveAlarm = newAlarmEvaluator.execute(bindings);
      isClearedAlarm = clearAlarmEvaluator.execute(bindings);
    } catch (ScriptException e) {
      log.debug("[{}] Failed to evaluate alarm expressions!", ctx.getRuleId(), e);
      throw new RuleException("Failed to evaluate alarm expressions!", e);
    }

    if (!isActiveAlarm && !isClearedAlarm) {
      log.debug("[{}] Incoming message do not trigger alarm", ctx.getRuleId());
      return md;
    }

    Alarm existing = null;
    if (isActiveAlarm) {
      Alarm alarm = buildAlarm(ctx, msg);
      existing = ctx.createOrUpdateAlarm(alarm);
      if (existing.getStartTs() == alarm.getStartTs()) {
        log.debug("[{}][{}] New Active Alarm detected", ctx.getRuleId(), existing.getId());
        md.put(IS_NEW_ALARM, Boolean.TRUE);
      } else {
        log.debug("[{}][{}] Existing Active Alarm detected", ctx.getRuleId(), existing.getId());
        md.put(IS_EXISTING_ALARM, Boolean.TRUE);
      }
    } else if (isClearedAlarm) {
      VelocityContext context = VelocityUtils.createContext(ctx.getDeviceMetaData(), msg);
      String alarmType = VelocityUtils.merge(alarmTypeTemplate, context);
      Optional<Alarm> alarm = ctx.findLatestAlarm(ctx.getDeviceMetaData().getDeviceId(), alarmType);
      if (alarm.isPresent()) {
        ctx.clearAlarm(alarm.get().getId(), System.currentTimeMillis());
        log.debug("[{}][{}] Existing Active Alarm cleared");
        md.put(IS_CLEARED_ALARM, Boolean.TRUE);
        existing = alarm.get();
      }
    }
    // TODO: handle cleared alarms

    if (existing != null) {
      md.put("alarmId", existing.getId().getId());
      md.put("alarmType", existing.getType());
      md.put("alarmSeverity", existing.getSeverity());
      try {
        md.put("alarmDetails", mapper.writeValueAsString(existing.getDetails()));
      } catch (JsonProcessingException e) {
        throw new RuleException("Failed to serialize alarm details", e);
      }
    }

    return md;
  }

  private Alarm buildAlarm(RuleContext ctx, FromDeviceMsg msg) throws RuleException {
    VelocityContext context = VelocityUtils.createContext(ctx.getDeviceMetaData(), msg);
    String alarmType = VelocityUtils.merge(alarmTypeTemplate, context);
    String alarmDetails = VelocityUtils.merge(alarmDetailsTemplate, context);

    Alarm alarm = new Alarm();
    alarm.setOriginator(ctx.getDeviceMetaData().getDeviceId());
    alarm.setType(alarmType);

    alarm.setStatus(status);
    alarm.setSeverity(severity);
    alarm.setPropagate(configuration.isAlarmPropagateFlag());

    try {
      alarm.setDetails(mapper.readTree(alarmDetails));
    } catch (IOException e) {
      log.debug("[{}] Failed to parse alarm details {} as json string after evaluation.", ctx.getRuleId(), e);
      throw new RuleException("Failed to parse alarm details as json string after evaluation!", e);
    }
    return alarm;
  }

  private Bindings buildBindings(RuleContext ctx, FromDeviceMsg msg) {
    Bindings bindings = NashornJsEvaluator.getAttributeBindings(ctx.getDeviceMetaData().getDeviceAttributes());
    if (msg != null) {
      switch (msg.getMsgType()) {
      case POST_ATTRIBUTES_REQUEST:
        bindings = NashornJsEvaluator.updateBindings(bindings, (UpdateAttributesRequest) msg);
        break;
      case POST_TELEMETRY_REQUEST:
        TelemetryUploadRequest telemetryMsg = (TelemetryUploadRequest) msg;
        for (List<KvEntry> entries : telemetryMsg.getData().values()) {
          bindings = NashornJsEvaluator.toBindings(bindings, entries);
        }
      }
    }
    return bindings;
  }

  private void initEvaluators() {
    newAlarmEvaluator = new NashornJsEvaluator(configuration.getNewAlarmExpression());
    clearAlarmEvaluator = new NashornJsEvaluator(configuration.getClearAlarmExpression());
  }

  private void destroyEvaluators() {
    if (newAlarmEvaluator != null) {
      newAlarmEvaluator.destroy();
    }
    if (clearAlarmEvaluator != null) {
      clearAlarmEvaluator.destroy();
    }
  }
}
