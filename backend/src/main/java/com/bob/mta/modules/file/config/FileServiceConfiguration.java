package com.bob.mta.modules.file.config;

import com.bob.mta.modules.file.service.FileService;
import com.bob.mta.modules.file.service.impl.InMemoryFileService;
import com.bob.mta.modules.file.service.impl.MinioFileService;
import io.minio.MinioClient;
import java.time.Duration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(FileStorageProperties.class)
public class FileServiceConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "app.file", name = "storage", havingValue = "MINIO")
    public MinioClient minioClient(FileStorageProperties properties) {
        FileStorageProperties.MinioProperties minio = properties.getMinio();
        if (!StringUtils.hasText(minio.getEndpoint())) {
            throw new IllegalStateException("MinIO endpoint must be configured when storage is MINIO");
        }
        if (!StringUtils.hasText(minio.getAccessKey()) || !StringUtils.hasText(minio.getSecretKey())) {
            throw new IllegalStateException("MinIO credentials must be configured when storage is MINIO");
        }
        return MinioClient.builder()
                .endpoint(minio.getEndpoint())
                .credentials(minio.getAccessKey(), minio.getSecretKey())
                .build();
    }

    @Bean
    public FileService fileService(FileStorageProperties properties,
                                   ObjectProvider<MinioClient> minioClientProvider,
                                   ObjectProvider<com.bob.mta.modules.file.persistence.FileMetadataMapper> metadataMapperProvider) {
        MinioClient client = minioClientProvider.getIfAvailable();
        if (client == null) {
            return new InMemoryFileService();
        }
        int expirySeconds = properties.getMinio().getDownloadExpirySeconds();
        if (expirySeconds <= 0) {
            throw new IllegalStateException("MinIO presigned URL expiry must be positive");
        }
        long maxSupported = Duration.ofDays(7).getSeconds();
        int boundedExpiry = (int) Math.min(expirySeconds, maxSupported);
        com.bob.mta.modules.file.persistence.FileMetadataMapper metadataMapper = metadataMapperProvider.getIfAvailable();
        if (metadataMapper == null) {
            throw new IllegalStateException("File metadata mapper must be available when MinIO storage is enabled");
        }
        return new MinioFileService(client, Duration.ofSeconds(boundedExpiry), metadataMapper);
    }
}
