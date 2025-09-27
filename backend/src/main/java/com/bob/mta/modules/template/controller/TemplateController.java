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
import com.bob.mta.i18n.Localization;
import com.bob.mta.i18n.LocalizationKeys;
import com.bob.mta.modules.template.service.TemplateService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController {

    private final TemplateService templateService;
    private final AuditRecorder auditRecorder;

    public TemplateController(TemplateService templateService, AuditRecorder auditRecorder) {
        this.templateService = templateService;
        this.auditRecorder = auditRecorder;
    }

    @GetMapping
    public ApiResponse<List<TemplateResponse>> list(@RequestParam(required = false) TemplateType type) {
        List<TemplateResponse> responses = templateService.list(type).stream()
                .map(TemplateResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @GetMapping("/{id}")
    public ApiResponse<TemplateResponse> get(@PathVariable long id) {
        return ApiResponse.success(TemplateResponse.from(templateService.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TemplateResponse> create(@Valid @RequestBody CreateTemplateRequest request) {
        TemplateDefinition definition = templateService.create(
                request.getType(),
                request.getName(),
                request.getSubject(),
                request.getContent(),
                request.getTo(),
                request.getCc(),
                request.getEndpoint(),
                request.isEnabled(),
                request.getDescription());
        auditRecorder.record("Template", String.valueOf(definition.getId()), "CREATE_TEMPLATE",
                Localization.text(LocalizationKeys.Audit.TEMPLATE_CREATE),
                null, TemplateResponse.from(definition));
        return ApiResponse.success(TemplateResponse.from(definition));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TemplateResponse> update(@PathVariable long id, @Valid @RequestBody UpdateTemplateRequest request) {
        TemplateDefinition before = templateService.get(id);
        TemplateDefinition updated = templateService.update(
                id,
                request.getName(),
                request.getSubject(),
                request.getContent(),
                request.getTo(),
                request.getCc(),
                request.getEndpoint(),
                request.isEnabled(),
                request.getDescription());
        auditRecorder.record("Template", String.valueOf(id), "UPDATE_TEMPLATE",
                Localization.text(LocalizationKeys.Audit.TEMPLATE_UPDATE),
                TemplateResponse.from(before), TemplateResponse.from(updated));
        return ApiResponse.success(TemplateResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable long id) {
        TemplateDefinition before = templateService.get(id);
        templateService.delete(id);
        auditRecorder.record("Template", String.valueOf(id), "DELETE_TEMPLATE",
                Localization.text(LocalizationKeys.Audit.TEMPLATE_DELETE),
                TemplateResponse.from(before), null);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/render")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<RenderedTemplateResponse> render(@PathVariable long id,
                                                         @RequestBody(required = false) RenderTemplateRequest request) {
        RenderedTemplate rendered = templateService.render(id, request == null ? null : request.getContext());
        RenderedTemplateResponse response = RenderedTemplateResponse.from(rendered);
        auditRecorder.record("Template", String.valueOf(id), "RENDER_TEMPLATE",
                Localization.text(LocalizationKeys.Audit.TEMPLATE_RENDER), null, response);
        return ApiResponse.success(response);
    }
}
