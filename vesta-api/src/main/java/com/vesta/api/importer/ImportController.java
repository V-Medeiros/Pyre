package com.vesta.api.importer;

import com.vesta.api.common.security.CurrentUser;
import com.vesta.api.importer.ImportDtos.ImportResponse;
import com.vesta.api.importer.ImportDtos.LocalStorageImportRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/imports")
public class ImportController {

    private final LocalStorageImportService service;

    public ImportController(LocalStorageImportService service) {
        this.service = service;
    }

    @PostMapping("/local-storage")
    ImportResponse importLocal(Authentication auth,
                               @RequestHeader("Idempotency-Key") String idempotencyKey,
                               @Valid @RequestBody LocalStorageImportRequest request) {
        return service.importData(CurrentUser.id(auth), idempotencyKey, request);
    }

    @GetMapping("/{id}")
    ImportResponse get(Authentication auth, @PathVariable UUID id) {
        return service.get(CurrentUser.id(auth), id);
    }
}

