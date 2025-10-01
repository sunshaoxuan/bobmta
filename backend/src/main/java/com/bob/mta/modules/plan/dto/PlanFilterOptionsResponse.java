package com.bob.mta.modules.plan.dto;

import com.bob.mta.modules.plan.service.PlanFilterDescriptor;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

public class PlanFilterOptionsResponse {

    private final String statusLabel;
    private final List<Option> statuses;
    private final String ownerLabel;
    private final List<Option> owners;
    private final String customerLabel;
    private final List<Option> customers;
    private final Window plannedWindow;

    private PlanFilterOptionsResponse(String statusLabel,
                                      List<Option> statuses,
                                      String ownerLabel,
                                      List<Option> owners,
                                      String customerLabel,
                                      List<Option> customers,
                                      Window plannedWindow) {
        this.statusLabel = statusLabel;
        this.statuses = statuses == null ? List.of() : List.copyOf(statuses);
        this.ownerLabel = ownerLabel;
        this.owners = owners == null ? List.of() : List.copyOf(owners);
        this.customerLabel = customerLabel;
        this.customers = customers == null ? List.of() : List.copyOf(customers);
        this.plannedWindow = plannedWindow;
    }

    public static PlanFilterOptionsResponse from(PlanFilterDescriptor descriptor) {
        Window window = null;
        if (descriptor.getPlannedWindow() != null) {
            window = new Window(
                    descriptor.getWindowLabel(),
                    descriptor.getWindowHint(),
                    descriptor.getPlannedWindow().getStart(),
                    descriptor.getPlannedWindow().getEnd()
            );
        }
        return new PlanFilterOptionsResponse(
                descriptor.getStatusLabel(),
                descriptor.getStatuses().stream().map(PlanFilterOptionsResponse::toOption).toList(),
                descriptor.getOwnerLabel(),
                descriptor.getOwners().stream().map(PlanFilterOptionsResponse::toOption).toList(),
                descriptor.getCustomerLabel(),
                descriptor.getCustomers().stream().map(PlanFilterOptionsResponse::toOption).toList(),
                window
        );
    }

    private static Option toOption(PlanFilterDescriptor.Option option) {
        return new Option(option.getValue(), option.getLabel(), option.getCount());
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

    public Window getPlannedWindow() {
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

    public static class Window {
        private final String label;
        private final String hint;
        private final OffsetDateTime start;
        private final OffsetDateTime end;

        public Window(String label, String hint, OffsetDateTime start, OffsetDateTime end) {
            this.label = label;
            this.hint = hint;
            this.start = start;
            this.end = end;
        }

        public String getLabel() {
            return label;
        }

        public String getHint() {
            return hint;
        }

        public OffsetDateTime getStart() {
            return start;
        }

        public OffsetDateTime getEnd() {
            return end;
        }
    }
}
