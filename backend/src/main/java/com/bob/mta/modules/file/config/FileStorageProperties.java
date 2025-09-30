package com.bob.mta.modules.file.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.file")
public class FileStorageProperties {

    private String storage = "IN_MEMORY";

    private final MinioProperties minio = new MinioProperties();

    public String getStorage() {
        return storage;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }

    public MinioProperties getMinio() {
        return minio;
    }

    public static class MinioProperties {

        private String endpoint;

        private String accessKey;

        private String secretKey;

        private int downloadExpirySeconds = 900;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public int getDownloadExpirySeconds() {
            return downloadExpirySeconds;
        }

        public void setDownloadExpirySeconds(int downloadExpirySeconds) {
            this.downloadExpirySeconds = downloadExpirySeconds;
        }
    }
}
