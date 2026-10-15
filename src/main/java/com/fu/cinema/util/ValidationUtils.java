package com.fu.cinema.util;

import java.time.LocalDate;
import java.time.Period;

public final class ValidationUtils {

    private ValidationUtils() {
    }

    public static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    public static int age(LocalDate birthday) {
        return Period.between(birthday, LocalDate.now()).getYears();
    }
}
