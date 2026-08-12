package in.tubalaw.courtos.modules.tracker.dto;

import lombok.Builder;
import lombok.Data;

/** One entry in the "Recent Searches" chips list on the tracker search page */
@Data @Builder
public class RecentSearchDto {
    private Long   jobId;
    private String cnr;
    private String status;
    private String searchedAt;      // ISO-8601
    private String caseTitle;       // petitioner vs respondent, populated from TrackedCase if available
}
