package com.bob.mta.modules.plan.service;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

public class PlanFilterDescriptor {

    private final String statusLabel;
    private final List<Option> statuses;
    private final String ownerLabel;
    private final List<Option> owners;
    private final String customerLabel;
    private final List<Option> customers;
    private final String windowLabel;
    private final String windowHint;
    private final DateRange plannedWindow;

    public PlanFilterDescriptor(String statusLabel,
                                List<Option> statuses,
                                String ownerLabel,
                                List<Option> owners,
                                String customerLabel,
                                List<Option> customers,
                                String windowLabel,
                                String windowHint,
                                DateRange plannedWindow) {
        this.statusLabel = statusLabel;
        this.statuses = statuses == null ? List.of() : List.copyOf(statuses);
        this.ownerLabel = ownerLabel;
        this.owners = owners == null ? List.of() : List.copyOf(owners);
        this.customerLabel = customerLabel;
        this.customers = customers == null ? List.of() : List.copyOf(customers);
        this.windowLabel = windowLabel;
        this.windowHint = windowHint;
        this.plannedWindow = plannedWindow;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public List<Option> getStatuses() {
        return Collections.unmodifiableList(statuses);
    }

    public String getOwnerLabel() {
        return ownerLabel;
    }

    public List<Option> getOwners() {
        return Collections.unmodifiableList(owners);
    }

    public String getCustomerLabel() {
        return customerLabel;
    }

    public List<Option> getCustomers() {
        return Collections.unmodifiableList(customers);
    }

    public String getWindowLabel() {
        return windowLabel;
    }

    public String getWindowHint() {
        return windowHint;
    }

    public DateRange getPlannedWindow() {
        return plannedWindow;
    }

    public static class Option {
        private final String value;
        private final String label;
        private final long count;

        public Option(String value, String label, long count) {
            this.value = value;
            this.label = label;
            this.count = count;
        }

        public String getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }

        public long getCount() {
            return count;
        }
    }

    public static class DateRange {
        private final OffsetDateTime start;
        private final OffsetDateTime end;

        public DateRange(OffsetDateTime start, OffsetDateTime end) {
            this.start = start;
            this.end = end;
        }

        public OffsetDateTime getStart() {
            return start;
        }

        public OffsetDateTime getEnd() {
            return end;
        }
    }
}
