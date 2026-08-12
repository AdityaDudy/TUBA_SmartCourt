package in.tubalaw.courtos.common.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PagedApiResponse<T> {
    private final boolean success;
    private final List<T> content;
    private final long totalElements;
    private final int totalPages;
    private final int page;
    private final int size;
    private final boolean first;
    private final boolean last;
    private final String timestamp;

    private PagedApiResponse(Page<T> page) {
        this.success       = true;
        this.content       = page.getContent();
        this.totalElements = page.getTotalElements();
        this.totalPages    = page.getTotalPages();
        this.page          = page.getNumber();
        this.size          = page.getSize();
        this.first         = page.isFirst();
        this.last          = page.isLast();
        this.timestamp     = Instant.now().toString();
    }

    public static <T> PagedApiResponse<T> of(Page<T> page) {
        return new PagedApiResponse<>(page);
    }
}
