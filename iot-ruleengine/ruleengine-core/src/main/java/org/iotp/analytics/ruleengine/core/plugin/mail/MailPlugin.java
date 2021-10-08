package org.iotp.analytics.ruleengine.core.plugin.mail;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.iotp.analytics.ruleengine.annotation.Plugin;
import org.iotp.analytics.ruleengine.api.plugins.AbstractPlugin;
import org.iotp.analytics.ruleengine.api.plugins.PluginContext;
import org.iotp.analytics.ruleengine.api.plugins.handlers.RuleMsgHandler;
import org.iotp.analytics.ruleengine.api.rules.RuleException;
import org.iotp.analytics.ruleengine.core.action.mail.SendMailAction;
import org.iotp.analytics.ruleengine.core.action.mail.SendMailActionMsg;
import org.iotp.analytics.ruleengine.plugins.msg.RuleToPluginMsg;
import org.iotp.infomgt.data.id.RuleId;
import org.iotp.infomgt.data.id.TenantId;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 */
@Plugin(name = "Mail Plugin", actions = {
    SendMailAction.class }, descriptor = "MailPluginDescriptor.json", configuration = MailPluginConfiguration.class)
@Slf4j
public class MailPlugin extends AbstractPlugin<MailPluginConfiguration> implements RuleMsgHandler {

  // TODO: Add logic to close this executor on shutdown.
  private static final ExecutorService executor = Executors.newSingleThreadExecutor();

  private MailPluginConfiguration configuration;
  private JavaMailSenderImpl mailSender;

  @Override
  public void init(MailPluginConfiguration configuration) {
    log.info("Initializing plugin using configuration {}", configuration);
    this.configuration = configuration;
    initMailSender(configuration);
  }

  @Override
  public void resume(PluginContext ctx) {
    initMailSender(configuration);
  }

  @Override
  public void suspend(PluginContext ctx) {
    mailSender = null;
  }

  @Override
  public void stop(PluginContext ctx) {
    mailSender = null;
  }

  private void initMailSender(MailPluginConfiguration configuration) {
    JavaMailSenderImpl mail = new JavaMailSenderImpl();
    mail.setHost(configuration.getHost());
    mail.setPort(configuration.getPort());
    mail.setUsername(configuration.getUsername());
    mail.setPassword(configuration.getPassword());
    if (configuration.getOtherProperties() != null) {
      Properties mailProperties = new Properties();
      configuration.getOtherProperties().forEach(p -> mailProperties.put(p.getKey(), p.getValue()));
      mail.setJavaMailProperties(mailProperties);
    }
    mailSender = mail;
  }

  @Override
  public void process(PluginContext ctx, TenantId tenantId, RuleId ruleId, RuleToPluginMsg<?> msg)
      throws RuleException {
    if (msg.getPayload() instanceof SendMailActionMsg) {
      executor.submit(() -> {
        try {
          sendMail((SendMailActionMsg) msg.getPayload());
        } catch (Exception e) {
          log.warn("[{}] Failed to send email", ctx.getPluginId(), e);
          ctx.persistError("Failed to send email", e);
        }
      });
    } else {
      throw new RuntimeException("Not supported msg type: " + msg.getPayload().getClass() + "!");
    }
  }

  private void sendMail(SendMailActionMsg msg) throws MessagingException {
    log.debug("Sending mail {}", msg);
    MimeMessage mailMsg = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(mailMsg, "UTF-8");
    helper.setFrom(msg.getFrom());
    helper.setTo(msg.getTo());
    if (!StringUtils.isEmpty(msg.getCc())) {
      helper.setCc(msg.getCc());
    }
    if (!StringUtils.isEmpty(msg.getBcc())) {
      helper.setBcc(msg.getBcc());
    }
    helper.setSubject(msg.getSubject());
    helper.setText(msg.getBody());
    mailSender.send(helper.getMimeMessage());
    log.debug("Mail sent {}", msg);
  }

  @Override
  protected RuleMsgHandler getRuleMsgHandler() {
    return this;
  }

}
