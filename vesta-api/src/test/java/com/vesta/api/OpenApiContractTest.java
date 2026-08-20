package com.vesta.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class OpenApiContractTest {

    @Test
    void versionedContractContainsCoreProductOperations() throws IOException {
        Path contract = Path.of("..", "docs", "api", "openapi.yaml");
        Map<String, Object> document = new Yaml().load(Files.readString(contract));

        assertThat(document.get("openapi")).isEqualTo("3.1.0");
        @SuppressWarnings("unchecked")
        Map<String, Object> paths = (Map<String, Object>) document.get("paths");
        assertThat(paths).containsKeys(
                "/api/v1/auth/register",
                "/api/v1/tasks",
                "/api/v1/focus-sessions",
                "/api/v1/streak",
                "/api/v1/imports/local-storage",
                "/api/v1/sync/changes");
    }
}

