package in.tubalaw.courtos.modules.tracker.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class HearingDto {
    private String hearingDate;
    private String judge;
    private String purposeOfHearing;
    private String nextHearingDate;
    private String businessRemarks;
}
