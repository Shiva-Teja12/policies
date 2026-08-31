package com.example.hrmspolicies2.dto.response;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
public class PageResponse<T> {

    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean first;
    private final boolean last;
    private final boolean hasNext;
    private final boolean hasPrevious;

    public PageResponse(Page<T> result) {
        this.content =
                result.getContent();

        this.page =
                result.getNumber();

        this.size =
                result.getSize();

        this.totalElements =
                result.getTotalElements();

        this.totalPages =
                result.getTotalPages();

        this.first =
                result.isFirst();

        this.last =
                result.isLast();

        this.hasNext =
                result.hasNext();

        this.hasPrevious =
                result.hasPrevious();
    }
}