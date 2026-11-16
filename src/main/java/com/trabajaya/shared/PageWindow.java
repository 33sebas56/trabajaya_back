package com.trabajaya.shared;

/** Safe pagination arithmetic shared by all SQL list adapters. */
public final class PageWindow {
    private PageWindow() { }

    public static long offset(int page, int size) {
        if (size < 1) throw new IllegalArgumentException("Page size must be positive");
        return (long) Math.max(0, page) * size;
    }
}
