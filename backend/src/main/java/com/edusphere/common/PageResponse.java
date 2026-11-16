package com.edusphere.common;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    public static <S, T> PageResponse<T> of(Page<S> p, Function<List<S>, List<T>> mapper) {
        return new PageResponse<>(mapper.apply(p.getContent()), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }
}
