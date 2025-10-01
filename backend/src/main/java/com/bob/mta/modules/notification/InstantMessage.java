package com.bob.mta.modules.notification;

import java.util.List;
import java.util.Objects;

public class InstantMessage {

    private final List<String> recipients;
    private final String content;

    public InstantMessage(List<String> recipients, String content) {
        this.recipients = recipients == null ? List.of() : List.copyOf(recipients);
        this.content = Objects.requireNonNull(content, "content");
    }

    public List<String> getRecipients() {
        return recipients;
    }

    public String getContent() {
        return content;
    }
}
