package com.bob.mta.i18n.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersistenceLocaleSettingsRepositoryTest {

    @Mock
    private LocaleSettingsMapper mapper;

    private PersistenceLocaleSettingsRepository repository;

    @BeforeEach
    void setUp() {
        repository = new PersistenceLocaleSettingsRepository(mapper);
    }

    @Test
    void getDefaultLocaleShouldReturnNullWhenMissing() {
        when(mapper.find(PersistenceLocaleSettingsRepository.DEFAULT_LOCALE_KEY)).thenReturn(null);

        String locale = repository.getDefaultLocale();

        assertThat(locale).isNull();
    }

    @Test
    void getDefaultLocaleShouldReturnStoredValue() {
        LocaleSettingEntity entity = new LocaleSettingEntity(
                PersistenceLocaleSettingsRepository.DEFAULT_LOCALE_KEY,
                "ja-JP",
                OffsetDateTime.now()
        );
        when(mapper.find(PersistenceLocaleSettingsRepository.DEFAULT_LOCALE_KEY)).thenReturn(entity);

        String locale = repository.getDefaultLocale();

        assertThat(locale).isEqualTo("ja-JP");
    }

    @Test
    void updateDefaultLocaleShouldInsertWhenUpdateReturnsZero() {
        when(mapper.update(any(LocaleSettingEntity.class))).thenReturn(0);

        repository.updateDefaultLocale("zh-cn");

        ArgumentCaptor<LocaleSettingEntity> captor = ArgumentCaptor.forClass(LocaleSettingEntity.class);
        verify(mapper).update(captor.capture());
        verify(mapper).insert(captor.getValue());
        LocaleSettingEntity entity = captor.getValue();
        assertThat(entity.key()).isEqualTo(PersistenceLocaleSettingsRepository.DEFAULT_LOCALE_KEY);
        assertThat(entity.value()).isEqualTo("zh-CN");
        assertThat(entity.updatedAt()).isNotNull();
    }

    @Test
    void updateDefaultLocaleShouldOnlyUpdateWhenRowExists() {
        when(mapper.update(any(LocaleSettingEntity.class))).thenReturn(1);

        repository.updateDefaultLocale("ja-jp");

        verify(mapper).update(any(LocaleSettingEntity.class));
        verify(mapper, never()).insert(any(LocaleSettingEntity.class));
    }

    @Test
    void updateDefaultLocaleShouldIgnoreBlankInput() {
        repository.updateDefaultLocale("   ");

        verify(mapper, never()).update(any(LocaleSettingEntity.class));
        verify(mapper, never()).insert(any(LocaleSettingEntity.class));
    }
}

