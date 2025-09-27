package com.bob.mta.modules.template.controller;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.modules.audit.service.AuditRecorder;
import com.bob.mta.modules.template.domain.RenderedTemplate;
import com.bob.mta.modules.template.domain.TemplateDefinition;
import com.bob.mta.modules.template.domain.TemplateType;
import com.bob.mta.modules.template.dto.CreateTemplateRequest;
import com.bob.mta.modules.template.dto.RenderTemplateRequest;
import com.bob.mta.modules.template.dto.RenderedTemplateResponse;
import com.bob.mta.modules.template.dto.TemplateResponse;
import com.bob.mta.modules.template.dto.UpdateTemplateRequest;
import com.bob.mta.i18n.LocalePreferenceService;
import com.bob.mta.i18n.Localization;
import com.bob.mta.i18n.LocalizationKeys;
import com.bob.mta.modules.template.service.TemplateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController {

    private final TemplateService templateService;
    private final AuditRecorder auditRecorder;
    private final LocalePreferenceService localePreferenceService;

    public TemplateController(TemplateService templateService, AuditRecorder auditRecorder,
                              LocalePreferenceService localePreferenceService) {
        this.templateService = templateService;
        this.auditRecorder = auditRecorder;
        this.localePreferenceService = localePreferenceService;
    }

    @GetMapping
    public ApiResponse<List<TemplateResponse>> list(@RequestParam(required = false) TemplateType type,
                                                    @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false)
                                                    String acceptLanguage) {
        Locale locale = localePreferenceService.resolveLocale(acceptLanguage);
        List<TemplateResponse> responses = templateService.list(type, locale).stream()
                .map(definition -> TemplateResponse.from(definition, locale))
                .toList();
        return ApiResponse.success(responses);
    }

    @GetMapping("/{id}")
    public ApiResponse<TemplateResponse> get(@PathVariable long id,
                                             @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false)
                                             String acceptLanguage) {
        Locale locale = localePreferenceService.resolveLocale(acceptLanguage);
        return ApiResponse.success(TemplateResponse.from(templateService.get(id, locale), locale));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TemplateResponse> create(@Valid @RequestBody CreateTemplateRequest request,
                                                @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false)
                                                String acceptLanguage) {
        Locale locale = localePreferenceService.resolveLocale(acceptLanguage);
        TemplateDefinition definition = templateService.create(
                request.getType(),
                request.getName().toValue(),
                request.getSubject() == null ? null : request.getSubject().toValue(),
                request.getContent().toValue(),
                request.getTo(),
                request.getCc(),
                request.getEndpoint(),
                request.isEnabled(),
                request.getDescription() == null ? null : request.getDescription().toValue());
        auditRecorder.record("Template", String.valueOf(definition.getId()), "CREATE_TEMPLATE",
                Localization.text(LocalizationKeys.Audit.TEMPLATE_CREATE),
                null, TemplateResponse.from(definition, locale));
        return ApiResponse.success(TemplateResponse.from(definition, locale));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TemplateResponse> update(@PathVariable long id, @Valid @RequestBody UpdateTemplateRequest request,
                                                @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false)
                                                String acceptLanguage) {
        Locale locale = localePreferenceService.resolveLocale(acceptLanguage);
        TemplateDefinition before = templateService.get(id, locale);
        TemplateDefinition updated = templateService.update(
                id,
                request.getName().toValue(),
                request.getSubject() == null ? null : request.getSubject().toValue(),
                request.getContent().toValue(),
                request.getTo(),
                request.getCc(),
                request.getEndpoint(),
                request.isEnabled(),
                request.getDescription() == null ? null : request.getDescription().toValue());
        auditRecorder.record("Template", String.valueOf(id), "UPDATE_TEMPLATE",
                Localization.text(LocalizationKeys.Audit.TEMPLATE_UPDATE),
                TemplateResponse.from(before, locale), TemplateResponse.from(updated, locale));
        return ApiResponse.success(TemplateResponse.from(updated, locale));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable long id,
                                    @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false)
                                    String acceptLanguage) {
        Locale locale = localePreferenceService.resolveLocale(acceptLanguage);
        TemplateDefinition before = templateService.get(id, locale);
        templateService.delete(id);
        auditRecorder.record("Template", String.valueOf(id), "DELETE_TEMPLATE",
                Localization.text(LocalizationKeys.Audit.TEMPLATE_DELETE),
                TemplateResponse.from(before, locale), null);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/render")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<RenderedTemplateResponse> render(@PathVariable long id,
                                                         @RequestBody(required = false) RenderTemplateRequest request,
                                                         @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false)
                                                         String acceptLanguage) {
        Locale locale = localePreferenceService.resolveLocale(acceptLanguage);
        RenderedTemplate rendered = templateService.render(id, request == null ? null : request.getContext(), locale);
        RenderedTemplateResponse response = RenderedTemplateResponse.from(rendered);
        auditRecorder.record("Template", String.valueOf(id), "RENDER_TEMPLATE",
                Localization.text(LocalizationKeys.Audit.TEMPLATE_RENDER), null, response);
        return ApiResponse.success(response);
    }
}
