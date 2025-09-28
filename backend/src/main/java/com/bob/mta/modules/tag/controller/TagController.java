package com.bob.mta.modules.tag.controller;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.i18n.LocalePreferenceService;
import com.bob.mta.i18n.Localization;
import com.bob.mta.i18n.LocalizationKeys;
import com.bob.mta.modules.audit.service.AuditRecorder;
import com.bob.mta.modules.customer.service.CustomerService;
import com.bob.mta.modules.plan.service.PlanService;
import com.bob.mta.modules.tag.domain.TagAssignment;
import com.bob.mta.modules.tag.domain.TagDefinition;
import com.bob.mta.modules.tag.domain.TagEntityType;
import com.bob.mta.modules.tag.domain.TagScope;
import com.bob.mta.modules.tag.dto.AssignTagRequest;
import com.bob.mta.modules.tag.dto.CreateTagRequest;
import com.bob.mta.modules.tag.dto.TagAssignmentResponse;
import com.bob.mta.modules.tag.dto.TagResponse;
import com.bob.mta.modules.tag.dto.UpdateTagRequest;
import com.bob.mta.modules.tag.service.TagService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/tags")
public class TagController {

    private final TagService tagService;
    private final CustomerService customerService;
    private final PlanService planService;
    private final AuditRecorder auditRecorder;
    private final LocalePreferenceService localePreferenceService;

    public TagController(TagService tagService, CustomerService customerService, PlanService planService,
                         AuditRecorder auditRecorder, LocalePreferenceService localePreferenceService) {
        this.tagService = tagService;
        this.customerService = customerService;
        this.planService = planService;
        this.auditRecorder = auditRecorder;
        this.localePreferenceService = localePreferenceService;
    }

    @GetMapping
    public ApiResponse<List<TagResponse>> list(@RequestParam(required = false) TagScope scope,
                                               @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false)
                                               String acceptLanguage) {
        Locale locale = localePreferenceService.resolveLocale(acceptLanguage);
        List<TagResponse> responses = tagService.list(scope, locale).stream()
                .map(definition -> TagResponse.from(definition, locale))
                .toList();
        return ApiResponse.success(responses);
    }

    @GetMapping("/{id}")
    public ApiResponse<TagResponse> get(@PathVariable long id,
                                        @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false)
                                        String acceptLanguage) {
        Locale locale = localePreferenceService.resolveLocale(acceptLanguage);
        return ApiResponse.success(TagResponse.from(tagService.getById(id, locale), locale));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TagResponse> create(@Valid @RequestBody CreateTagRequest request,
                                           @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false)
                                           String acceptLanguage) {
        Locale locale = localePreferenceService.resolveLocale(acceptLanguage);
        TagDefinition definition = tagService.create(
                request.getName().toValue(),
                request.getColor(),
                request.getIcon(),
                request.getScope(),
                request.getApplyRule(),
                request.isEnabled());
        auditRecorder.record("Tag", String.valueOf(definition.getId()), "CREATE_TAG",
                Localization.text(LocalizationKeys.Audit.TAG_CREATE),
                null, TagResponse.from(definition, locale));
        return ApiResponse.success(TagResponse.from(definition, locale));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TagResponse> update(@PathVariable long id, @Valid @RequestBody UpdateTagRequest request,
                                           @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false)
                                           String acceptLanguage) {
        Locale locale = localePreferenceService.resolveLocale(acceptLanguage);
        TagDefinition before = tagService.getById(id, locale);
        TagDefinition updated = tagService.update(
                id,
                request.getName().toValue(),
                request.getColor(),
                request.getIcon(),
                request.getScope(),
                request.getApplyRule(),
                request.isEnabled());
        auditRecorder.record("Tag", String.valueOf(id), "UPDATE_TAG",
                Localization.text(LocalizationKeys.Audit.TAG_UPDATE),
                TagResponse.from(before, locale), TagResponse.from(updated, locale));
        return ApiResponse.success(TagResponse.from(updated, locale));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable long id,
                                    @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false)
                                    String acceptLanguage) {
        Locale locale = localePreferenceService.resolveLocale(acceptLanguage);
        TagDefinition before = tagService.getById(id, locale);
        tagService.delete(id);
        auditRecorder.record("Tag", String.valueOf(id), "DELETE_TAG",
                Localization.text(LocalizationKeys.Audit.TAG_DELETE),
                TagResponse.from(before, locale), null);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<TagAssignmentResponse> assign(@PathVariable long id, @Valid @RequestBody AssignTagRequest request) {
        validateEntity(request.getEntityType(), request.getEntityId());
        TagAssignment assignment = tagService.assign(id, request.getEntityType(), request.getEntityId());
        auditRecorder.record("TagAssignment", assignment.getEntityType().name() + ":" + assignment.getEntityId(),
                "ASSIGN_TAG", Localization.text(LocalizationKeys.Audit.TAG_ASSIGN), null,
                TagAssignmentResponse.from(assignment));
        return ApiResponse.success(TagAssignmentResponse.from(assignment));
    }

    @DeleteMapping("/{id}/assignments/{entityType}/{entityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<Void> removeAssignment(@PathVariable long id,
                                              @PathVariable TagEntityType entityType,
                                              @PathVariable String entityId) {
        tagService.removeAssignment(id, entityType, entityId);
        auditRecorder.record("TagAssignment", entityType.name() + ":" + entityId,
                "REMOVE_TAG", Localization.text(LocalizationKeys.Audit.TAG_REMOVE), null, null);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/assignments")
    public ApiResponse<List<TagAssignmentResponse>> listAssignments(@PathVariable long id) {
        List<TagAssignmentResponse> responses = tagService.listAssignments(id).stream()
                .map(TagAssignmentResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @GetMapping("/entities/{entityType}/{entityId}")
    public ApiResponse<List<TagResponse>> listByEntity(@PathVariable TagEntityType entityType,
                                                       @PathVariable String entityId,
                                                       @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false)
                                                       String acceptLanguage) {
        Locale locale = localePreferenceService.resolveLocale(acceptLanguage);
        List<TagResponse> responses = tagService.findByEntity(entityType, entityId, locale).stream()
                .map(definition -> TagResponse.from(definition, locale))
                .toList();
        return ApiResponse.success(responses);
    }

    private void validateEntity(TagEntityType entityType, String entityId) {
        switch (entityType) {
            case CUSTOMER -> customerService.getCustomer(entityId);
            case PLAN -> planService.getPlan(entityId);
        }
    }
}
