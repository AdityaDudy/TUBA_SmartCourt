package in.tubalaw.courtos.modules.tracker.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class OrderDto {
    private Long   id;
    private String orderDate;
    private String orderNo;
    private String orderType;
    private String orderCategory;   // "JUDGMENT" | "INTERIM"
    /** Server-side download URL — never the raw eCourts URL */
    private String downloadUrl;
    private Long   fileSize;
    private String mimeType;
}
