package com.bob.mta.common.api;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Pagination metadata container compatible with Ant Design table expectations.
 *
 * @param <T> row type
 */
public class PageResponse<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final List<T> list;

    private final long total;

    private final long page;

    private final long pageSize;

    private PageResponse(final List<T> list, final long total, final long page, final long pageSize) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
    }

    public static <T> PageResponse<T> of(final List<T> list, final long total, final long page, final long pageSize) {
        final List<T> safeList = list == null ? Collections.emptyList() : List.copyOf(list);
        return new PageResponse<>(safeList, total, page, pageSize);
    }

    public List<T> getList() {
        return list;
    }

    public long getTotal() {
        return total;
    }

    public long getPage() {
        return page;
    }

    public long getPageSize() {
        return pageSize;
    }
}
