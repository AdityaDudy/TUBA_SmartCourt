package in.tubalaw.courtos.modules.tracker.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class PartyDto {
    private String name;
    private String advocate;
}
