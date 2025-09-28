package com.bob.mta.common.i18n.persistence;

import com.bob.mta.common.i18n.MultilingualText;
import com.bob.mta.common.i18n.MultilingualTextRecord;
import com.bob.mta.common.i18n.MultilingualTextRepository;
import com.bob.mta.common.i18n.MultilingualTextScope;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
@ConditionalOnBean(MultilingualTextMapper.class)
public class PersistenceMultilingualTextRepository implements MultilingualTextRepository {

    private final MultilingualTextMapper mapper;

    public PersistenceMultilingualTextRepository(MultilingualTextMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(MultilingualTextRecord record) {
        MultilingualTextEntity entity = toEntity(record, OffsetDateTime.now());
        int updated = mapper.update(entity);
        if (updated == 0) {
            mapper.insert(entity);
        }
    }

    @Override
    public Optional<MultilingualTextRecord> find(MultilingualTextScope scope, String entityId, String field) {
        MultilingualTextEntity entity = mapper.find(scope, entityId, field);
        if (entity == null) {
            return Optional.empty();
        }
        MultilingualText text = MultilingualText.of(entity.defaultLocale(), entity.translations());
        return Optional.of(new MultilingualTextRecord(entity.scope(), entity.entityId(), entity.field(), text));
    }

    private MultilingualTextEntity toEntity(MultilingualTextRecord record, OffsetDateTime updatedAt) {
        MultilingualText text = record.getText();
        return new MultilingualTextEntity(
                record.getScope(),
                record.getEntityId(),
                record.getField(),
                text.getDefaultLocale(),
                text.getTranslations(),
                updatedAt
        );
    }
}
