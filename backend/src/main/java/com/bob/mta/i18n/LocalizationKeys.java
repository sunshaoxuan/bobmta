package com.bob.mta.i18n;

public final class LocalizationKeys {

    private LocalizationKeys() {
    }

    public static final class App {
        public static final String NAME = "app.name";
        public static final String GREETING = "app.greeting";
        private App() {
        }
    }

    public static final class Frontend {
        public static final String BUNDLE_VERSION = "frontend.bundle.version";
        public static final String APP_TITLE = "frontend.app.title";
        public static final String APP_DESCRIPTION = "frontend.app.description";
        public static final String BACKEND_STATUS = "frontend.app.backend.status";
        public static final String BACKEND_SUCCESS = "frontend.app.backend.success";
        public static final String BACKEND_ERROR = "frontend.app.backend.error";
        public static final String BACKEND_ERROR_STATUS = "frontend.app.backend.error.status";
        public static final String BACKEND_ERROR_NETWORK = "frontend.app.backend.error.network";
        public static final String BACKEND_PENDING = "frontend.app.backend.pending";
        public static final String LOCALE_LABEL = "frontend.app.locale.label";
        public static final String AUTH_SECTION_TITLE = "frontend.auth.section.title";
        public static final String AUTH_USERNAME_LABEL = "frontend.auth.username.label";
        public static final String AUTH_PASSWORD_LABEL = "frontend.auth.password.label";
        public static final String AUTH_SUBMIT = "frontend.auth.submit";
        public static final String AUTH_LOGGING_IN = "frontend.auth.loggingIn";
        public static final String AUTH_LOGOUT = "frontend.auth.logout";
        public static final String AUTH_WELCOME = "frontend.auth.welcome";
        public static final String AUTH_ERROR = "frontend.auth.error";
        public static final String AUTH_TOKEN_EXPIRY = "frontend.auth.token.expiry";
        public static final String PLAN_SECTION_TITLE = "frontend.plan.section.title";
        public static final String PLAN_REFRESH = "frontend.plan.refresh";
        public static final String PLAN_TABLE_HEADER_ID = "frontend.plan.table.header.id";
        public static final String PLAN_TABLE_HEADER_TITLE = "frontend.plan.table.header.title";
        public static final String PLAN_TABLE_HEADER_OWNER = "frontend.plan.table.header.owner";
        public static final String PLAN_TABLE_HEADER_STATUS = "frontend.plan.table.header.status";
        public static final String PLAN_TABLE_HEADER_WINDOW = "frontend.plan.table.header.window";
        public static final String PLAN_TABLE_HEADER_PROGRESS = "frontend.plan.table.header.progress";
        public static final String PLAN_TABLE_HEADER_PARTICIPANTS = "frontend.plan.table.header.participants";
        public static final String PLAN_WINDOW_RANGE = "frontend.plan.table.window.range";
        public static final String PLAN_WINDOW_MISSING = "frontend.plan.table.window.missing";
        public static final String PLAN_LOADING = "frontend.plan.table.loading";
        public static final String PLAN_EMPTY = "frontend.plan.table.empty";
        public static final String PLAN_LOGIN_REQUIRED = "frontend.plan.loginRequired";
        public static final String PLAN_ERROR = "frontend.plan.table.error";
        public static final String PLAN_STATUS_DESIGN = "frontend.plan.status.design";
        public static final String PLAN_STATUS_SCHEDULED = "frontend.plan.status.scheduled";
        public static final String PLAN_STATUS_IN_PROGRESS = "frontend.plan.status.inProgress";
        public static final String PLAN_STATUS_COMPLETED = "frontend.plan.status.completed";
        public static final String PLAN_STATUS_CANCELLED = "frontend.plan.status.cancelled";
        private Frontend() {
        }
    }

    public static final class PlanFilter {
        public static final String STATUS_LABEL = "plan.filter.status.label";
        public static final String OWNER_LABEL = "plan.filter.owner.label";
        public static final String CUSTOMER_LABEL = "plan.filter.customer.label";
        public static final String WINDOW_LABEL = "plan.filter.window.label";
        public static final String WINDOW_HINT = "plan.filter.window.hint";

        private PlanFilter() {
        }
    }

    public static final class Validation {
        public static final String MULTILINGUAL_DEFAULT_LOCALE_REQUIRED =
                "validation.multilingual.defaultLocaleRequired";
        public static final String MULTILINGUAL_DEFAULT_VALUE_REQUIRED =
                "validation.multilingual.defaultValueRequired";
        public static final String MULTILINGUAL_LOCALE_REQUIRED =
                "validation.multilingual.localeRequired";
        public static final String MULTILINGUAL_MERGE_DEFAULT_MISSING =
                "validation.multilingual.mergeDefaultMissing";

        private Validation() {
        }
    }

    public static final class Errors {
        public static final String TAG_SCOPE_UNSUPPORTED = "error.tag.scope.unsupported";
        public static final String CUSTOM_FIELD_CODE_EXISTS = "error.customField.codeExists";
        public static final String CUSTOM_FIELD_REQUIRED_EMPTY = "error.customField.requiredEmpty";
        public static final String CUSTOM_FIELD_VALUE_REQUIRED = "error.customField.valueRequired";
        public static final String CUSTOM_FIELD_VALUE_INVALID_OPTION = "error.customField.valueInvalidOption";
        public static final String CUSTOM_FIELD_VALUE_INVALID_FORMAT = "error.customField.valueInvalidFormat";
        public static final String CUSTOM_FIELD_BOOLEAN_EXPECTED = "error.customField.booleanExpected";
        public static final String TEMPLATE_DISABLED = "error.template.disabled";
        public static final String TEMPLATE_ENDPOINT_INVALID = "error.template.endpointInvalid";
        public static final String PLAN_UPDATE_DESIGN_ONLY = "error.plan.update.designOnly";
        public static final String PLAN_DELETE_DESIGN_ONLY = "error.plan.delete.designOnly";
        public static final String PLAN_ALREADY_PUBLISHED = "error.plan.alreadyPublished";
        public static final String PLAN_INACTIVE = "error.plan.inactive";
        public static final String PLAN_NODE_REQUIRES_START = "error.plan.node.requiresStart";
        public static final String PLAN_HANDOVER_OWNER_REQUIRED = "error.plan.handover.ownerRequired";
        public static final String PLAN_EXECUTE_REQUIRES_PUBLISH = "error.plan.execute.requiresPublish";
        public static final String PLAN_REMINDER_TEMPLATE_REQUIRED = "error.plan.reminder.templateRequired";
        public static final String PLAN_REMINDER_TRIGGER_REQUIRED = "error.plan.reminder.triggerRequired";
        public static final String PLAN_REMINDER_OFFSET_NEGATIVE = "error.plan.reminder.offsetNegative";
        public static final String PLAN_REMINDER_CHANNELS_REQUIRED = "error.plan.reminder.channelsRequired";
        public static final String PLAN_REMINDER_ID_REQUIRED = "error.plan.reminder.idRequired";
        public static final String LOCALE_UNSUPPORTED = "error.locale.unsupported";

        private Errors() {
        }
    }

    public static final class Audit {
        public static final String CUSTOM_FIELD_CREATE = "audit.customField.create";
        public static final String CUSTOM_FIELD_UPDATE = "audit.customField.update";
        public static final String CUSTOM_FIELD_DELETE = "audit.customField.delete";
        public static final String CUSTOM_FIELD_VALUE_UPSERT = "audit.customField.value.upsert";

        public static final String PLAN_CREATE = "audit.plan.create";
        public static final String PLAN_UPDATE = "audit.plan.update";
        public static final String PLAN_DELETE = "audit.plan.delete";
        public static final String PLAN_PUBLISH = "audit.plan.publish";
        public static final String PLAN_CANCEL = "audit.plan.cancel";
        public static final String PLAN_HANDOVER = "audit.plan.handover";
        public static final String PLAN_REMINDER_UPDATE = "audit.plan.reminder.update";
        public static final String PLAN_NODE_START = "audit.plan.node.start";
        public static final String PLAN_NODE_COMPLETE = "audit.plan.node.complete";

        public static final String TEMPLATE_CREATE = "audit.template.create";
        public static final String TEMPLATE_UPDATE = "audit.template.update";
        public static final String TEMPLATE_DELETE = "audit.template.delete";
        public static final String TEMPLATE_RENDER = "audit.template.render";

        public static final String TAG_CREATE = "audit.tag.create";
        public static final String TAG_UPDATE = "audit.tag.update";
        public static final String TAG_DELETE = "audit.tag.delete";
        public static final String TAG_ASSIGN = "audit.tag.assign";
        public static final String TAG_REMOVE = "audit.tag.remove";

        public static final String USER_CREATE = "audit.user.create";
        public static final String USER_ACTIVATE = "audit.user.activate";
        public static final String USER_RESEND_ACTIVATION = "audit.user.resendActivation";
        public static final String USER_ASSIGN_ROLES = "audit.user.assignRoles";

        public static final String FILE_REGISTER = "audit.file.register";
        public static final String FILE_DELETE = "audit.file.delete";

        private Audit() {
        }
    }

    public static final class Seeds {
        public static final String TEMPLATE_EMAIL_NAME = "seed.template.email.name";
        public static final String TEMPLATE_EMAIL_SUBJECT = "seed.template.email.subject";
        public static final String TEMPLATE_EMAIL_CONTENT = "seed.template.email.content";
        public static final String TEMPLATE_EMAIL_DESCRIPTION = "seed.template.email.description";
        public static final String TEMPLATE_REMOTE_NAME = "seed.template.remote.name";
        public static final String TEMPLATE_REMOTE_CONTENT = "seed.template.remote.content";
        public static final String TEMPLATE_REMOTE_DESCRIPTION = "seed.template.remote.description";

        public static final String PLAN_NODE_BACKUP_TITLE = "seed.plan.node.backup.title";
        public static final String PLAN_NODE_BACKUP_DESCRIPTION = "seed.plan.node.backup.description";
        public static final String PLAN_NODE_NOTIFY_TITLE = "seed.plan.node.notify.title";
        public static final String PLAN_NODE_NOTIFY_DESCRIPTION = "seed.plan.node.notify.description";
        public static final String PLAN_PRIMARY_TITLE = "seed.plan.primary.title";
        public static final String PLAN_PRIMARY_DESCRIPTION = "seed.plan.primary.description";
        public static final String PLAN_SECONDARY_TITLE = "seed.plan.secondary.title";
        public static final String PLAN_SECONDARY_DESCRIPTION = "seed.plan.secondary.description";
        public static final String PLAN_SECONDARY_NODE_TITLE = "seed.plan.secondary.node.title";
        public static final String PLAN_SECONDARY_NODE_DESCRIPTION = "seed.plan.secondary.node.description";

        public static final String CUSTOM_FIELD_ERP_VERSION_NAME = "seed.customField.erpVersion.name";
        public static final String CUSTOM_FIELD_ERP_VERSION_DESCRIPTION = "seed.customField.erpVersion.description";
        public static final String CUSTOM_FIELD_CRITICAL_SYSTEM_NAME = "seed.customField.criticalSystem.name";
        public static final String CUSTOM_FIELD_CRITICAL_SYSTEM_DESCRIPTION = "seed.customField.criticalSystem.description";

        public static final String USER_ADMIN_DISPLAY_NAME = "seed.user.admin.displayName";
        public static final String USER_OPERATOR_DISPLAY_NAME = "seed.user.operator.displayName";

        public static final String PLAN_ACTIVITY_DEFINITION_UPDATED = "seed.plan.activity.definitionUpdated";
        public static final String PLAN_ACTIVITY_PUBLISHED = "seed.plan.activity.published";
        public static final String PLAN_ACTIVITY_CANCELLED = "seed.plan.activity.cancelled";
        public static final String PLAN_ACTIVITY_COMPLETED = "seed.plan.activity.completed";
        public static final String PLAN_ACTIVITY_NODE_STARTED = "seed.plan.activity.nodeStarted";
        public static final String PLAN_ACTIVITY_NODE_COMPLETED = "seed.plan.activity.nodeCompleted";
        public static final String PLAN_ACTIVITY_HANDOVER = "seed.plan.activity.handover";
        public static final String PLAN_ACTIVITY_REMINDER_UPDATED = "seed.plan.activity.reminderUpdated";
        public static final String PLAN_ACTIVITY_CREATED = "seed.plan.activity.created";

        public static final String PLAN_REMINDER_FIRST = "seed.plan.reminder.first";
        public static final String PLAN_REMINDER_SECOND = "seed.plan.reminder.second";
        public static final String PLAN_REMINDER_THIRD = "seed.plan.reminder.third";

        public static final String CUSTOMER_HOKKAIDO_NAME = "seed.customer.hokkaido.name";
        public static final String CUSTOMER_HOKKAIDO_ABBREVIATION = "seed.customer.hokkaido.abbreviation";
        public static final String CUSTOMER_HOKKAIDO_INDUSTRY = "seed.customer.hokkaido.industry";
        public static final String CUSTOMER_HOKKAIDO_REGION = "seed.customer.hokkaido.region";
        public static final String CUSTOMER_HOKKAIDO_TAG_PRIMARY = "seed.customer.hokkaido.tag.primary";
        public static final String CUSTOMER_HOKKAIDO_TAG_SECONDARY = "seed.customer.hokkaido.tag.secondary";
        public static final String CUSTOMER_HOKKAIDO_CONNECTIVITY_TYPE = "seed.customer.hokkaido.connectivityType";
        public static final String CUSTOMER_HOKKAIDO_CONNECTIVITY_VALUE = "seed.customer.hokkaido.connectivityValue";
        public static final String CUSTOMER_HOKKAIDO_IP_ADDRESS = "seed.customer.hokkaido.ipAddress";
        public static final String CUSTOMER_HOKKAIDO_TOOL = "seed.customer.hokkaido.tool";
        public static final String CUSTOMER_HOKKAIDO_NOTE = "seed.customer.hokkaido.note";

        public static final String CUSTOMER_KITAMI_NAME = "seed.customer.kitami.name";
        public static final String CUSTOMER_KITAMI_ABBREVIATION = "seed.customer.kitami.abbreviation";
        public static final String CUSTOMER_KITAMI_INDUSTRY = "seed.customer.kitami.industry";
        public static final String CUSTOMER_KITAMI_REGION = "seed.customer.kitami.region";
        public static final String CUSTOMER_KITAMI_TAG_PRIMARY = "seed.customer.kitami.tag.primary";
        public static final String CUSTOMER_KITAMI_CONNECTIVITY_TYPE = "seed.customer.kitami.connectivityType";
        public static final String CUSTOMER_KITAMI_CONNECTIVITY_VALUE = "seed.customer.kitami.connectivityValue";
        public static final String CUSTOMER_KITAMI_TOOL = "seed.customer.kitami.tool";
        public static final String CUSTOMER_KITAMI_NOTE = "seed.customer.kitami.note";

        public static final String CUSTOMER_TOKYO_METRO_NAME = "seed.customer.tokyoMetro.name";
        public static final String CUSTOMER_TOKYO_METRO_ABBREVIATION = "seed.customer.tokyoMetro.abbreviation";
        public static final String CUSTOMER_TOKYO_METRO_INDUSTRY = "seed.customer.tokyoMetro.industry";
        public static final String CUSTOMER_TOKYO_METRO_REGION = "seed.customer.tokyoMetro.region";
        public static final String CUSTOMER_TOKYO_METRO_TAG_PRIMARY = "seed.customer.tokyoMetro.tag.primary";
        public static final String CUSTOMER_TOKYO_METRO_CONNECTIVITY_TYPE = "seed.customer.tokyoMetro.connectivityType";
        public static final String CUSTOMER_TOKYO_METRO_CONNECTIVITY_VALUE = "seed.customer.tokyoMetro.connectivityValue";
        public static final String CUSTOMER_TOKYO_METRO_TOOL = "seed.customer.tokyoMetro.tool";
        public static final String CUSTOMER_TOKYO_METRO_NOTE = "seed.customer.tokyoMetro.note";

        public static final String TAG_PRIORITY_NAME = "seed.tag.priority.name";
        public static final String TAG_PLAN_ANNUAL_NAME = "seed.tag.planAnnual.name";

        public static final String CUSTOMER_FIELD_CONNECTIVITY_TYPE_LABEL = "seed.customer.field.connectivityType";
        public static final String CUSTOMER_FIELD_IP_ADDRESS_LABEL = "seed.customer.field.ipAddress";
        public static final String CUSTOMER_FIELD_TOOL_LABEL = "seed.customer.field.tool";
        public static final String CUSTOMER_FIELD_NOTE_LABEL = "seed.customer.field.note";

        private Seeds() {
        }
    }

    public static final class PlanReminder {
        public static final String TRIGGER_BEFORE_START = "plan.reminder.trigger.beforeStart";
        public static final String TRIGGER_BEFORE_START_DESC = "plan.reminder.trigger.beforeStart.description";
        public static final String TRIGGER_BEFORE_END = "plan.reminder.trigger.beforeEnd";
        public static final String TRIGGER_BEFORE_END_DESC = "plan.reminder.trigger.beforeEnd.description";
        public static final String CHANNEL_EMAIL = "plan.reminder.channel.email";
        public static final String CHANNEL_EMAIL_DESC = "plan.reminder.channel.email.description";
        public static final String CHANNEL_IM = "plan.reminder.channel.im";
        public static final String CHANNEL_IM_DESC = "plan.reminder.channel.im.description";
        public static final String CHANNEL_SMS = "plan.reminder.channel.sms";
        public static final String CHANNEL_SMS_DESC = "plan.reminder.channel.sms.description";
        public static final String RECIPIENT_OWNER = "plan.reminder.recipient.owner";
        public static final String RECIPIENT_OWNER_DESC = "plan.reminder.recipient.owner.description";
        public static final String RECIPIENT_PARTICIPANTS = "plan.reminder.recipient.participants";
        public static final String RECIPIENT_PARTICIPANTS_DESC = "plan.reminder.recipient.participants.description";
        public static final String RECIPIENT_CUSTOM = "plan.reminder.recipient.custom";
        public static final String RECIPIENT_CUSTOM_DESC = "plan.reminder.recipient.custom.description";

        private PlanReminder() {
        }
    }

    public static final class PlanSummary {
        public static final String RESPONSIBLE_LABEL = "plan.summary.responsible";
        public static final String STATUS_LABEL = "plan.summary.status";
        public static final String CANCEL_REASON_LABEL = "plan.summary.cancelReason";
        public static final String CANCEL_OPERATOR_LABEL = "plan.summary.cancelOperator";
        public static final String CANCEL_TIME_LABEL = "plan.summary.cancelTime";
        private PlanSummary() {
        }
    }
}
