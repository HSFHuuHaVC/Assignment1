package com.fu.cinema.util;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

public final class PageUtils {

    public static final int DEFAULT_SIZE = 10;

    private PageUtils() {
    }

    /** URL hiện tại (giữ nguyên các tham số lọc) nhưng bỏ tham số page → dùng cho thanh phân trang. */
    public static String currentUrlWithoutPage() {
        return ServletUriComponentsBuilder.fromCurrentRequest()
                .replaceQueryParam("page")
                .build(true)
                .toUriString();
    }
}
