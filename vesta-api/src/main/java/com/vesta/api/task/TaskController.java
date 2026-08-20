package com.vesta.api.task;

import com.vesta.api.common.security.CurrentUser;
import com.vesta.api.task.TaskDtos.CreateTaskRequest;
import com.vesta.api.task.TaskDtos.TaskFilter;
import com.vesta.api.task.TaskDtos.TaskPage;
import com.vesta.api.task.TaskDtos.TaskResponse;
import com.vesta.api.task.TaskDtos.UpdateTaskRequest;
import com.vesta.api.task.TaskDtos.VersionRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TaskResponse create(Authentication auth, @Valid @RequestBody CreateTaskRequest request) {
        return service.create(CurrentUser.id(auth), request);
    }

    @GetMapping("/{id}")
    TaskResponse get(Authentication auth, @PathVariable String id) {
        return service.get(CurrentUser.id(auth), id);
    }

    @GetMapping
    TaskPage list(Authentication auth,
                  @RequestParam(defaultValue = "ALL") TaskFilter status,
                  @RequestParam(required = false) String cursor,
                  @RequestParam(defaultValue = "50") int limit) {
        return service.list(CurrentUser.id(auth), status, cursor, limit);
    }

    @PatchMapping("/{id}")
    TaskResponse rename(Authentication auth, @PathVariable String id,
                        @Valid @RequestBody UpdateTaskRequest request) {
        return service.rename(CurrentUser.id(auth), id, request);
    }

    @PostMapping("/{id}/complete")
    TaskResponse complete(Authentication auth, @PathVariable String id,
                          @Valid @RequestBody(required = false) VersionRequest request) {
        return service.complete(CurrentUser.id(auth), id, request == null ? new VersionRequest(null) : request);
    }

    @PostMapping("/{id}/reopen")
    TaskResponse reopen(Authentication auth, @PathVariable String id,
                        @Valid @RequestBody(required = false) VersionRequest request) {
        return service.reopen(CurrentUser.id(auth), id, request == null ? new VersionRequest(null) : request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(Authentication auth, @PathVariable String id,
                @RequestParam(required = false) Long version) {
        service.delete(CurrentUser.id(auth), id, version);
    }
}

