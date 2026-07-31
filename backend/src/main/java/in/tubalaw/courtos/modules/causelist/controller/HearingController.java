package in.tubalaw.courtos.modules.causelist.controller;

import in.tubalaw.courtos.common.util.ApiResponse;
import in.tubalaw.courtos.modules.causelist.entity.Hearing;
import in.tubalaw.courtos.modules.causelist.service.HearingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hearings")
@RequiredArgsConstructor
public class HearingController {

    private final HearingService hearingService;

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<List<Hearing>>> today() {
        return ResponseEntity.ok(ApiResponse.ok(hearingService.today()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Hearing>>> list(
            @RequestParam(required = false) String date) {
        if ("all".equalsIgnoreCase(date)) return ResponseEntity.ok(ApiResponse.ok(hearingService.filter("All")));
        if (date != null && !date.isBlank()) return ResponseEntity.ok(ApiResponse.ok(hearingService.byDate(date)));
        return ResponseEntity.ok(ApiResponse.ok(hearingService.today()));
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<List<Hearing>>> filter(@RequestParam(required = false) String type) {
        return ResponseEntity.ok(ApiResponse.ok(hearingService.filter(type)));
    }

    /**
     * Admin-only manual sync: re-derives Hearing rows from all already-stored
     * matter-linked TrackedCases. No new eCourts calls — useful right after bulk
     * matter linking so admins don't wait until the next midnight cron run.
     */
    @PostMapping("/sync")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> sync() {
        int synced = hearingService.syncAll();
        return ResponseEntity.ok(ApiResponse.ok(Map.of("synced", synced), "Hearing sync complete."));
    }
}

