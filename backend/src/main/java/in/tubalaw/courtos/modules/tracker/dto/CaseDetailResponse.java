package in.tubalaw.courtos.modules.tracker.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** Full case detail response — returned by GET /api/tracker/{cnr} and embedded in ScrapeJobStatusResponse when DONE */
@Data @Builder
public class CaseDetailResponse {

    // ── Identity ──────────────────────────────────────────────────
    private String cnr;
    private String caseType;
    private String filingNo;
    private String filingDate;
    private String registrationNo;
    private String registrationDate;

    // ── Court & Bench ─────────────────────────────────────────────
    private String courtName;
    private String courtComplex;
    private String judgeName;

    // ── Status ────────────────────────────────────────────────────
    /** PENDING, DISPOSED, STAYED, DISMISSED, TRANSFERRED */
    private String caseStatus;
    private String stageOfCase;
    private String nextHearingDate;

    // ── Acts & FIR ────────────────────────────────────────────────
    private List<String> actsAndSections;
    private String firNo;
    private String firYear;
    private String policeStation;

    // ── Parties ───────────────────────────────────────────────────
    private List<PartyDto> petitioners;
    private List<PartyDto> respondents;

    // ── History ───────────────────────────────────────────────────
    private List<HearingDto> hearings;

    // ── Orders ────────────────────────────────────────────────────
    private List<OrderDto> orders;

    // ── Matter link ───────────────────────────────────────────────
    private Long matterId;
    private String matterTitle;         // populated if linked

    // ── Alerts ────────────────────────────────────────────────────
    private boolean alertActive;        // for the calling user

    // ── Metadata ──────────────────────────────────────────────────
    /** CACHE or LIVE — tells frontend whether to show stale-data banner */
    private String cacheSource;
    private String lastSyncedAt;        // ISO-8601
}
