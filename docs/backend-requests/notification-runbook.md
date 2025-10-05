# Notification Channel Runbook

## Overview
The notification module exposes three pluggable adapters that deliver reminders or plan related alerts:

| Channel | Adapter Bean | Property Prefix | Notes |
| --- | --- | --- | --- |
| Email | SmtpEmailNotificationAdapter | 
otification.email | Wraps Spring JavaMailSender; falls back to LoggingEmailNotificationAdapter when disabled or misconfigured. |
| Instant Message / Webhook | WebhookInstantMessageNotificationAdapter | 
otification.instant-message | Posts JSON payloads to a configured webhook endpoint. When disabled the system logs messages via LoggingInstantMessageNotificationAdapter. |
| HTTP API | HttpApiNotificationAdapter | 
otification.api | Generic REST invoker for custom endpoints. Enabled by default but will log-only when 
otification.api.enabled=false. |

All adapters share the common NotificationProperties class that is registered in BobMtaApplication via @EnableConfigurationProperties. If a property group is disabled or missing, Spring will auto-configure the corresponding logging-only adapter so business flows still succeed.

## Configuration Cheatsheet
Add the following snippet (already seeded in ackend/src/main/resources/application.yml) to your environment specific profile or pass as environment variables:

`yaml
notification:
  email:
    enabled: true
    from: noreply@example.com
    reply-to: support@example.com
  instant-message:
    enabled: true
    webhook-url: https://hooks.example.com/notify
    connect-timeout: 5s
    read-timeout: 10s
  api:
    enabled: true
    connect-timeout: 5s
    read-timeout: 15s
`

### Email (SMTP)
1. Ensure spring.mail.host, spring.mail.port, and credentials are provided by the target environment.
2. Optional: set 
otification.email.reply-to for ticketing integrations.
3. Disable the channel (
otification.email.enabled=false) to revert to logging-only mode.

### Webhook / Instant Message
1. Supply a HTTPS webhook endpoint (Slack/Teams/etc.) using 
otification.instant-message.webhook-url.
2. Optional timeouts default to 5s/10s and can be overridden per environment.
3. Disable the channel to avoid outbound HTTP traffic; the logging adapter will capture payloads in application logs.

### HTTP API Adapter
1. Designed for bespoke REST integrations; defaults to enabled.
2. Override 
otification.api.enabled=false when no upstream is present.
3. Request/response metadata is stored in the returned NotificationResult and logged at WARN level when non-2xx codes are received.

## Operational Playbook
| Scenario | Action |
| --- | --- |
| Adapter throws RestClientException | The notification service automatically records a failure result and continues without blocking the business flow. Review application logs to inspect NotificationResult.metadata. |
| SMTP credentials invalid | Spring Mail raises MailAuthenticationException -> adapter returns failure; disable the channel to fall back to logging while credentials are rotated. |
| Webhook slow / unreachable | Tune 
otification.instant-message.{connect,read}-timeout or temporarily disable the channel. |
| Integration testing | Keep channels disabled; use the logging adapters and inspect JSON payloads in logs. |

## Verification Steps
1. Configure channels and restart the backend.
2. Trigger a reminder preview (/api/v1/plans/{id}/reminders/preview) or execute node completion flow (uses Email/IM adapters in InMemoryPlanService).
3. Confirm adapter specific logs (com.bob.mta.modules.notification.*) show success/failure metadata.
4. Restore disabled channels to logging-only mode after tests to avoid unintended outbound traffic.

## References
- ackend/src/main/java/com/bob/mta/modules/notification package for adapter implementations.
- pplication.yml for default example configuration.
- README.md «最新更新» section for recent changes.
