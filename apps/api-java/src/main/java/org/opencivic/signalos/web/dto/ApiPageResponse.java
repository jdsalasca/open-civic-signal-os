package org.opencivic.signalos.web.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record ApiPageResponse<T>(
    List<T> content,
    long totalElements,
    int totalPages,
    int size,
    int number,
    boolean first,
    boolean last
) {
    public static <T> ApiPageResponse<T> from(Page<T> page) {
        return new ApiPageResponse<>(
            page.getContent(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.getSize(),
            page.getNumber(),
            page.isFirst(),
            page.isLast()
        );
    }
}
