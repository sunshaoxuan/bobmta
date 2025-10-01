package com.bob.mta.modules.plan.repository;

import java.util.List;
import java.util.Map;

public interface PlanAttachmentRepository {

    Map<String, List<String>> findAttachments(String planId);

    void replaceAttachments(String planId, Map<String, List<String>> attachments);
}
