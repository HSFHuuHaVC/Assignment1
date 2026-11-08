package com.fu.cinema.desktop.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class MoneyUtil {
    private MoneyUtil() { }
    public static String format(BigDecimal value) {
        // NumberFormat is mutable: do not share a static instance across Task threads.
        return value == null ? "" : NumberFormat.getNumberInstance(Locale.of("vi", "VN")).format(value) + " đ";
    }
}
