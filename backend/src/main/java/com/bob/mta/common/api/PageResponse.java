package com.bob.mta.common.api;

import java.util.Collections;
import java.util.List;

public class PageResponse<T> {

    private final List<T> items;
    private final long total;
    private final int page;
    private final int size;

    public PageResponse(List<T> items, long total, int page, int size) {
        this.items = items == null ? Collections.emptyList() : List.copyOf(items);
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public static <T> PageResponse<T> of(List<T> items, long total, int page, int size) {
        return new PageResponse<>(items, total, page, size);
    }

    public List<T> getItems() {
        return items;
    }

    public long getTotal() {
        return total;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }
}
