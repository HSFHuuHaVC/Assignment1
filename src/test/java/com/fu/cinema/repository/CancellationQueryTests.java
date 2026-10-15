package com.fu.cinema.repository;

import com.fu.cinema.entity.*;
import com.fu.cinema.enums.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class CancellationQueryTests extends JpaFixture {
    @Autowired BookingRepository bookings;
    @Autowired TicketRepository tickets;

    @Test
    void q9CancelsOnlyTargetShowtimeAndIsIdempotent() {
        Customer customer = customer("Alice", 1);
        Movie movie = movie("Film", genre("Action"), MovieStatus.SHOWING);
        Room room = room("Room 01");
        LocalDateTime start = LocalDateTime.of(2026, 10, 15, 9, 0);
        Showtime target = showtime(movie, room, start, ShowtimeStatus.OPEN);
        Showtime other = showtime(movie, room, start.plusDays(1), ShowtimeStatus.OPEN);
        Booking pending = booking(customer, target, BookingStatus.PENDING, start, "A1");
        Booking confirmed = booking(customer, target, BookingStatus.CONFIRMED, start, "A2");
        booking(customer, target, BookingStatus.CANCELLED, start, "A3");
        Booking untouched = booking(customer, other, BookingStatus.CONFIRMED, start, "A1");
        em.flush();
        assertThat(bookings.cancelAllByShowtime(target.getShowtimeId())).isEqualTo(2);
        assertThat(em.contains(pending)).isFalse();
        assertThat(bookings.findById(pending.getBookingId()).orElseThrow().getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(bookings.findById(confirmed.getBookingId()).orElseThrow().getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(bookings.findById(untouched.getBookingId()).orElseThrow().getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(tickets.findSoldSeatCodes(target.getShowtimeId())).isEmpty();
        assertThat(tickets.count()).isEqualTo(4);
        assertThat(bookings.cancelAllByShowtime(target.getShowtimeId())).isZero();
        assertThat(bookings.cancelAllByShowtime(-1L)).isZero();
    }

    @Test
    void q9FlushesPendingChangesBeforeClearingPersistenceContext() {
        Customer customer = customer("Alice", 1);
        Showtime showtime = showtime(movie("Film", genre("Action"), MovieStatus.SHOWING), room("Room 01"),
                LocalDateTime.of(2026, 10, 15, 9, 0), ShowtimeStatus.OPEN);
        Booking booking = booking(customer, showtime, BookingStatus.PENDING, showtime.getStartTime(), "A1");
        em.flush();
        customer.setFullName("Updated name");
        assertThat(bookings.cancelAllByShowtime(showtime.getShowtimeId())).isEqualTo(1);
        assertThat(em.find(Customer.class, customer.getCustomerId()).getFullName()).isEqualTo("Updated name");
        assertThat(bookings.findById(booking.getBookingId()).orElseThrow().getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }
}
