package com.bob.mta.modules.customfield.service;

import com.bob.mta.modules.customfield.domain.CustomFieldDefinition;
import com.bob.mta.modules.customfield.domain.CustomFieldType;
import com.bob.mta.modules.customfield.domain.CustomFieldValue;

import java.util.List;
import java.util.Map;

public interface CustomFieldService {

    List<CustomFieldDefinition> listDefinitions();

    CustomFieldDefinition getDefinition(long id);

    CustomFieldDefinition createDefinition(String code, String label, CustomFieldType type, boolean required,
                                           List<String> options, String description);

    CustomFieldDefinition updateDefinition(long id, String label, CustomFieldType type, boolean required,
                                           List<String> options, String description);

    void deleteDefinition(long id);

    List<CustomFieldValue> listValues(String entityId);

    List<CustomFieldValue> updateValues(String entityId, Map<Long, String> values);
}
