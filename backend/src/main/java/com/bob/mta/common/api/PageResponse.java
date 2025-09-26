package com.bob.mta.common.api;

<<<<<<< HEAD
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
=======
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
>>>>>>> origin/main
    }

    public long getTotal() {
        return total;
    }

<<<<<<< HEAD
    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
=======
    public long getPage() {
        return page;
    }

    public long getPageSize() {
        return pageSize;
>>>>>>> origin/main
    }
}
