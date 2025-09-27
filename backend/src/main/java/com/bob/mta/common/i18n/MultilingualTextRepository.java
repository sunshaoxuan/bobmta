package com.bob.mta.common.i18n;

import java.util.Optional;

public interface MultilingualTextRepository {

    void save(MultilingualTextRecord record);

    Optional<MultilingualTextRecord> find(MultilingualTextScope scope, String entityId, String field);
}
