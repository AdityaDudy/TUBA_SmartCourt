package in.tubalaw.courtos.modules.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportTable {
    private String title;
    private String filterSummary;
    @Builder.Default
    private List<String> headers = new ArrayList<>();
    @Builder.Default
    private List<List<Object>> rows = new ArrayList<>();
}
