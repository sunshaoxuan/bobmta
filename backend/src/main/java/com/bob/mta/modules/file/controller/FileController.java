package com.bob.mta.modules.file.controller;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.common.i18n.MessageResolver;
import com.bob.mta.modules.audit.service.AuditRecorder;
import com.bob.mta.modules.file.domain.FileMetadata;
import com.bob.mta.modules.file.dto.FileResponse;
import com.bob.mta.modules.file.dto.RegisterFileRequest;
import com.bob.mta.i18n.Localization;
import com.bob.mta.i18n.LocalizationKeys;
import com.bob.mta.modules.file.service.FileService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;
    private final AuditRecorder auditRecorder;
    private final MessageResolver messageResolver;

    public FileController(FileService fileService, AuditRecorder auditRecorder, MessageResolver messageResolver) {
        this.fileService = fileService;
        this.auditRecorder = auditRecorder;
        this.messageResolver = messageResolver;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<FileResponse> register(@Valid @RequestBody RegisterFileRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String uploader = authentication != null ? authentication.getName() : "system";
        FileMetadata metadata = fileService.register(
                request.getFileName(),
                request.getContentType(),
                request.getSize(),
                request.getBucket(),
                request.getBizType(),
                request.getBizId(),
                uploader);
        FileResponse response = FileResponse.from(metadata, fileService.buildDownloadUrl(metadata));
        auditRecorder.record("File", metadata.getId(), "REGISTER_FILE",
                Localization.text(LocalizationKeys.Audit.FILE_REGISTER), null, response);
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    public ApiResponse<FileResponse> get(@PathVariable String id) {
        FileMetadata metadata = fileService.get(id);
        return ApiResponse.success(FileResponse.from(metadata, fileService.buildDownloadUrl(metadata)));
    }

    @GetMapping
    public ApiResponse<List<FileResponse>> list(@RequestParam(required = false) String bizType,
                                                @RequestParam(required = false) String bizId) {
        List<FileResponse> responses = fileService.listByBiz(bizType, bizId).stream()
                .map(meta -> FileResponse.from(meta, fileService.buildDownloadUrl(meta)))
                .toList();
        return ApiResponse.success(responses);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        FileMetadata metadata = fileService.get(id);
        fileService.delete(id);
        auditRecorder.record("File", id, "DELETE_FILE",
                Localization.text(LocalizationKeys.Audit.FILE_DELETE),
                FileResponse.from(metadata, fileService.buildDownloadUrl(metadata)), null);
        return ApiResponse.success();
    }
}
