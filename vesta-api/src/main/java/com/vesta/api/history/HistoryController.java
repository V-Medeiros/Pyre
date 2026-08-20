package com.vesta.api.history;

import com.vesta.api.common.security.CurrentUser;
import com.vesta.api.history.HistoryDtos.HistoryResponse;
import com.vesta.api.history.HistoryDtos.StatsSummary;
import com.vesta.api.history.HistoryDtos.StreakResponse;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HistoryController {

    private final HistoryService service;

    public HistoryController(HistoryService service) {
        this.service = service;
    }

    @GetMapping("/history")
    HistoryResponse history(Authentication auth,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                            LocalDate from,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                            LocalDate to) {
        return service.history(CurrentUser.id(auth), from, to);
    }

    @GetMapping("/streak")
    StreakResponse streak(Authentication auth) {
        return service.streak(CurrentUser.id(auth));
    }

    @GetMapping("/stats/summary")
    StatsSummary stats(Authentication auth,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                       LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                       LocalDate to) {
        return service.stats(CurrentUser.id(auth), from, to);
    }
}

