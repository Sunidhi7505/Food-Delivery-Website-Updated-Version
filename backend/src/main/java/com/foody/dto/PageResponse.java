package com.foody.dto;

import java.util.List;

/** Generic page wrapper used by "load more" lists. */
public class PageResponse<T> {

    private final List<T> items;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean hasMore;

    private PageResponse(List<T> items, int page, int size, long totalElements) {
        this.items = items;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = (int) Math.ceil(totalElements / (double) size);
        this.hasMore = page + 1 < totalPages;
    }

    /** Slices an already filtered/sorted list. Page is zero based; size is clamped to 1..50. */
    public static <T> PageResponse<T> of(List<T> all, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        int safePage = Math.max(page, 0);
        int from = (int) Math.min((long) safePage * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        return new PageResponse<>(all.subList(from, to), safePage, safeSize, all.size());
    }

    public List<T> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean isHasMore() {
        return hasMore;
    }
}
