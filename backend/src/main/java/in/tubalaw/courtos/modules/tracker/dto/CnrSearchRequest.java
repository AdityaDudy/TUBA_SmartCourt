package in.tubalaw.courtos.modules.tracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CnrSearchRequest {

    /**
     * CNR format: 4 uppercase letters (state code + court code) + 6-digit running number + 4-digit year
     * Example: DLHC010023452024
     */
    @NotBlank(message = "CNR number is required")
    @Pattern(
        regexp = "^[A-Z]{4}\\d{10,14}$",
        message = "Invalid CNR format. Expected format: 4 letters followed by 10 to 14 digits (e.g. DLST010012342024)"
    )
    private String cnr;
}
