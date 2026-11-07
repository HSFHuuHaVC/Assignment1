package com.fu.cinema.desktop.model;

import com.fu.cinema.entity.Booking;
import com.fu.cinema.entity.Showtime;
import com.fu.cinema.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingRow(Long id, String customerName, String movieTitle, String roomName,
                         LocalDateTime startTime, int seatCount, BigDecimal total,
                         BookingStatus status, LocalDateTime bookingDate) {
    public static BookingRow from(Booking b) {
        Showtime s = b.getShowtime();
        return new BookingRow(b.getBookingId(), b.getCustomer().getFullName(), s.getMovie().getTitle(),
                s.getRoom().getRoomName(), s.getStartTime(), b.getTickets().size(), b.getTotalAmount(),
                b.getStatus(), b.getBookingDate());
    }
}

