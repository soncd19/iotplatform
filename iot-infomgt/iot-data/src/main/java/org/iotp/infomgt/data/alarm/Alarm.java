package org.iotp.infomgt.data.alarm;

import org.iotp.infomgt.data.common.BaseData;
import org.iotp.infomgt.data.common.NamingThing;
import org.iotp.infomgt.data.id.AlarmId;
import org.iotp.infomgt.data.id.EntityId;
import org.iotp.infomgt.data.id.TenantId;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class Alarm extends BaseData<AlarmId> implements NamingThing {

  private TenantId tenantId;
  private String type;
  private EntityId originator;
  private AlarmSeverity severity;
  private AlarmStatus status;
  private long startTs;
  private long endTs;
  private long ackTs;
  private long clearTs;
  private JsonNode details;
  private boolean propagate;

  public Alarm() {
    super();
  }

  public Alarm(AlarmId id) {
    super(id);
  }

  public Alarm(Alarm alarm) {
    super(alarm.getId());
    this.createdTime = alarm.getCreatedTime();
    this.tenantId = alarm.getTenantId();
    this.type = alarm.getType();
    this.originator = alarm.getOriginator();
    this.severity = alarm.getSeverity();
    this.status = alarm.getStatus();
    this.startTs = alarm.getStartTs();
    this.endTs = alarm.getEndTs();
    this.ackTs = alarm.getAckTs();
    this.clearTs = alarm.getClearTs();
    this.details = alarm.getDetails();
    this.propagate = alarm.isPropagate();
  }

  @Override
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public String getName() {
    return type;
  }
}
