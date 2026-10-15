package com.fu.cinema.repository;

import com.fu.cinema.entity.*;
import com.fu.cinema.enums.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class ReportingQueryTests extends JpaFixture {
    @Autowired BookingRepository bookings;
    @Autowired ReviewRepository reviews;

    @Test
    void q7CountsTicketsAndRevenueByMovieUsingOnlyConfirmedBookingDates() {
        Customer customer = customer("Alice", 1);
        Genre genre = genre("Action");
        Room room = room("Room 01");
        LocalDateTime from = LocalDateTime.of(2026, 10, 15, 0, 0);
        // Same titles must not combine different movies.
        Movie first = movie("Same title", genre, MovieStatus.SHOWING);
        Movie second = movie("Same title", genre, MovieStatus.SHOWING);
        Showtime a = showtime(first, room, from.plusDays(10), ShowtimeStatus.OPEN);
        Showtime b = showtime(second, room, from.minusDays(10), ShowtimeStatus.OPEN);
        b.setTicketPrice(new BigDecimal("90000"));
        booking(customer, a, BookingStatus.CONFIRMED, from, "A1", "A2", "A3");
        booking(customer, b, BookingStatus.CONFIRMED, from.plusHours(1), "A1");
        booking(customer, a, BookingStatus.PENDING, from, "A4");
        booking(customer, a, BookingStatus.CANCELLED, from, "A5");
        booking(customer, a, BookingStatus.CONFIRMED, from.minusSeconds(1), "A6");
        booking(customer, a, BookingStatus.CONFIRMED, from.plusDays(1), "A7");
        flushAndClear();
        var rows = bookings.revenueByMovie(from, from.plusDays(1));
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).getTicketCount()).isEqualTo(3L);
        assertThat(rows.get(0).getRevenue()).isEqualByComparingTo("210000");
        assertThat(rows.get(1).getTicketCount()).isEqualTo(1L);
        assertThat(rows.get(1).getRevenue()).isEqualByComparingTo("90000");
        assertThat(bookings.revenueByMovie(from.minusDays(2), from.minusDays(1))).isEmpty();
    }

    @Test
    void q8FiltersConfirmedBookingsInHalfOpenPeriodNewestFirst() {
        Customer customer = customer("Alice", 1);
        LocalDateTime from = LocalDateTime.of(2026, 10, 15, 0, 0);
        Showtime showtime = showtime(movie("Film", genre("Action"), MovieStatus.SHOWING), room("Room 01"),
                from.plusDays(10), ShowtimeStatus.OPEN);
        Booking first = booking(customer, showtime, BookingStatus.CONFIRMED, from, "A1");
        Booking latest = booking(customer, showtime, BookingStatus.CONFIRMED, from.plusHours(1), "A2");
        booking(customer, showtime, BookingStatus.CONFIRMED, from.plusDays(1), "A3");
        booking(customer, showtime, BookingStatus.CANCELLED, from, "A4");
        booking(customer, showtime, BookingStatus.PENDING, from, "A5");
        flushAndClear();
        var rows = bookings.findConfirmedInPeriod(from, from.plusDays(1));
        em.clear();
        assertThat(rows).extracting(Booking::getBookingId).containsExactly(latest.getBookingId(), first.getBookingId());
        assertThat(rows.getFirst().getCustomer().getFullName()).isEqualTo("Alice");
        assertThat(rows.getFirst().getShowtime().getMovie().getTitle()).isEqualTo("Film");
    }

    @Test
    void q10ReturnsNullAverageAndZeroCountForUnreviewedMovie() {
        Movie movie = movie("Film", genre("Action"), MovieStatus.SHOWING);
        var empty = reviews.summarize(movie.getMovieId());
        assertThat(empty.getAverage()).isNull();
        assertThat(empty.getTotal()).isZero();
        LocalDateTime now = LocalDateTime.of(2026, 10, 15, 9, 0);
        review(customer("Alice", 1), movie, 3, now);
        review(customer("Bob", 2), movie, 4, now);
        Movie other = movie("Other", movie.getGenre(), MovieStatus.SHOWING);
        review(customer("Other", 3), other, 1, now);
        flushAndClear();
        var summary = reviews.summarize(movie.getMovieId());
        assertThat(summary.getAverage()).isEqualTo(3.5);
        assertThat(summary.getTotal()).isEqualTo(2L);
    }

    @Test
    void q12RequiresCorrectCustomerMovieConfirmedStatusAndFinishedShowtime() {
        Customer customer = customer("Alice", 1);
        Customer other = customer("Bob", 2);
        Genre genre = genre("Action");
        Movie movie = movie("Film", genre, MovieStatus.SHOWING);
        Movie different = movie("Other", genre, MovieStatus.SHOWING);
        Room room = room("Room 01");
        LocalDateTime now = LocalDateTime.of(2026, 10, 15, 12, 0);
        Showtime past = showtime(movie, room, now.minusHours(3), ShowtimeStatus.OPEN);
        Showtime boundary = showtime(movie, room, now.minusHours(2), ShowtimeStatus.OPEN);
        Showtime future = showtime(movie, room, now.plusDays(1), ShowtimeStatus.OPEN);
        Showtime otherMovie = showtime(different, room, now.minusDays(1), ShowtimeStatus.OPEN);
        booking(customer, past, BookingStatus.CONFIRMED, now.minusDays(2), "A1");
        booking(customer, past, BookingStatus.PENDING, now.minusDays(2), "A2");
        booking(customer, past, BookingStatus.CANCELLED, now.minusDays(2), "A3");
        booking(customer, boundary, BookingStatus.CONFIRMED, now.minusDays(2), "A1");
        booking(customer, future, BookingStatus.CONFIRMED, now.minusDays(2), "A1");
        booking(other, past, BookingStatus.CONFIRMED, now.minusDays(2), "A4");
        booking(customer, otherMovie, BookingStatus.CONFIRMED, now.minusDays(2), "A1");
        flushAndClear();
        assertThat(bookings.countWatched(customer.getCustomerId(), movie.getMovieId(), now)).isEqualTo(1);
        assertThat(bookings.countWatched(-1L, movie.getMovieId(), now)).isZero();
        assertThat(bookings.countWatched(customer.getCustomerId(), -1L, now)).isZero();
    }
}
