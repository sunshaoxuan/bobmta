package com.bob.mta.modules.template.dto;

import java.util.Map;

public class RenderTemplateRequest {

    private Map<String, String> context;

    public Map<String, String> getContext() {
        return context;
    }

    public void setContext(Map<String, String> context) {
        this.context = context;
    }
}
