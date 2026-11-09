package com.fu.cinema.desktop.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReportData(
        LocalDate start,
        LocalDate end,
        List<RevenueRow> revenue,
        List<ReportBookingRow> bookings,
        long totalBookings,
        long totalTickets,
        BigDecimal totalRevenue
) {
    public boolean isEmpty() {
        return bookings == null || bookings.isEmpty();
    }
}

