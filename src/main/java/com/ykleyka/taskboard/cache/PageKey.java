package com.ykleyka.taskboard.cache;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class PageKey {
    private final int page;
    private final int size;
    private final String sort;

    public static PageKey from(Pageable pageable) {
        String sortValue = pageable.getSort() == null ? "" : pageable.getSort().toString();
        return new PageKey(pageable.getPageNumber(), pageable.getPageSize(), sortValue);
    }
}
