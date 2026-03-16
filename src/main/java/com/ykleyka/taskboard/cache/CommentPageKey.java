package com.ykleyka.taskboard.cache;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class CommentPageKey {
    private final Long taskId;
    private final int page;
    private final int size;
    private final String sort;

    public static CommentPageKey from(Long taskId, Pageable pageable) {
        String sortValue = pageable.getSort() == null ? "" : pageable.getSort().toString();
        return new CommentPageKey(
                taskId, pageable.getPageNumber(), pageable.getPageSize(), sortValue);
    }
}
