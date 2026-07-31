package in.tubalaw.courtos.modules.tracker.dto;

import lombok.Builder;
import lombok.Data;

/** Returned for async operations: initial search (202), refresh, captcha-solve, poll */
@Data @Builder
public class ScrapeJobStatusResponse {

    private Long   jobId;
    private String cnr;
    /** PENDING, RUNNING, DONE, FAILED */
    private String status;
    private String errorMessage;

    /** Populated when status = DONE — frontend can skip the detail GET */
    private CaseDetailResponse result;
}
