package com.quickbite.restaurant.dto.response;

import lombok.*;
import org.springframework.data.domain.Page;
import java.util.List;

/**
 * PagedResponse<T> — wraps Spring Page results for API responses.
 * Used for GET /restaurants (paginated list).
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PagedResponse<T> {
    private List<T> content;
    private int pageNo;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean last;

    public static <T> PagedResponse<T> of(Page<T> page) {
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
