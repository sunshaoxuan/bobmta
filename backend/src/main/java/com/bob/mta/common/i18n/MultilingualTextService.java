package com.bob.mta.common.i18n;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class MultilingualTextService {

    private final MultilingualTextRepository repository;

    public MultilingualTextService(MultilingualTextRepository repository) {
        this.repository = repository;
    }

    public void upsert(MultilingualTextScope scope, String entityId, String field, MultilingualText text) {
        repository.save(new MultilingualTextRecord(scope, entityId, field, text));
    }

    public void upsert(MultilingualTextScope scope, String entityId, String field, String defaultLocale, Map<String, String> translations) {
        upsert(scope, entityId, field, MultilingualText.of(defaultLocale, translations));
    }

    public Optional<MultilingualText> find(MultilingualTextScope scope, String entityId, String field) {
        return repository.find(scope, entityId, field).map(MultilingualTextRecord::getText);
    }

    public String resolveText(MultilingualTextScope scope, String entityId, String field, String locale, String fallback) {
        return find(scope, entityId, field)
                .map(text -> text.getValueOrDefault(locale))
                .orElse(fallback);
    }
}
