package com.fu.cinema.desktop.model;

import org.springframework.data.domain.Page;
import java.util.List;
import java.util.function.Function;

public record PageResult<T>(List<T> items, int page, int totalPages, long totalElements) {
    public PageResult { items = List.copyOf(items); }
    public static <E, T> PageResult<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResult<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), Math.max(1, page.getTotalPages()), page.getTotalElements());
    }
}
