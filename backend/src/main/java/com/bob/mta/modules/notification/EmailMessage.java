package com.bob.mta.modules.notification;

import java.util.List;

public class EmailMessage {

    private final List<String> to;
    private final List<String> cc;
    private final String subject;
    private final String content;

    public EmailMessage(List<String> to, List<String> cc, String subject, String content) {
        this.to = to == null ? List.of() : List.copyOf(to);
        this.cc = cc == null ? List.of() : List.copyOf(cc);
        this.subject = subject == null ? "" : subject;
        this.content = content;
    }

    public List<String> getTo() {
        return to;
    }

    public List<String> getCc() {
        return cc;
    }

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }
}
