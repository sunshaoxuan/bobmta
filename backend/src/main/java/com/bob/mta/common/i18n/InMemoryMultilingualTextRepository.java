package com.bob.mta.common.i18n;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryMultilingualTextRepository implements MultilingualTextRepository {

    private final Map<String, MultilingualTextRecord> storage = new ConcurrentHashMap<>();

    @Override
    public void save(MultilingualTextRecord record) {
        storage.put(key(record.getScope(), record.getEntityId(), record.getField()), record);
    }

    @Override
    public Optional<MultilingualTextRecord> find(MultilingualTextScope scope, String entityId, String field) {
        return Optional.ofNullable(storage.get(key(scope, entityId, field)));
    }

    private String key(MultilingualTextScope scope, String entityId, String field) {
        return scope.name() + ":" + entityId + ":" + field;
    }
}
