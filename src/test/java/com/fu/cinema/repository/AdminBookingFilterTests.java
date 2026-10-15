package com.fu.cinema.repository;

import com.fu.cinema.entity.Booking;
import com.fu.cinema.enums.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;

class AdminBookingFilterTests extends JpaFixture {
    @Autowired BookingRepository bookings;

    @Test
    void q6CombinesStatusInclusiveStartExclusiveEndAndCaseInsensitiveCustomerSearch() {
        var an = customer("Nguyen Van An", 1);
        var binh = customer("Tran Thi Binh", 2);
        LocalDateTime start = LocalDateTime.of(2026, 10, 1, 0, 0);
        var show = showtime(movie("Film", genre("Action"), MovieStatus.SHOWING), room("Room"),
                start.plusDays(10), ShowtimeStatus.OPEN);
        Booking included = booking(an, show, BookingStatus.PENDING, start);
        booking(an, show, BookingStatus.PENDING, start.plusDays(1));
        booking(an, show, BookingStatus.CONFIRMED, start);
        booking(binh, show, BookingStatus.PENDING, start);
        booking(an, show, BookingStatus.PENDING, start.minusNanos(1_000_000));
        flushAndClear();
        var result = bookings.search(BookingStatus.PENDING, start, start.plusDays(1), "NGUYEN", PageRequest.of(0, 10));
        assertThat(result.getContent()).extracting(Booking::getBookingId).containsExactly(included.getBookingId());
        assertThat(bookings.search(null, null, null, "USER2@", PageRequest.of(0, 10))).hasSize(1);
        assertThat(bookings.search(null, null, null, binh.getMobile(), PageRequest.of(0, 10))).hasSize(1);
        assertThat(bookings.search(null, null, null, "missing", PageRequest.of(0, 10))).isEmpty();
    }

    @Test
    void q6PaginatesNewestFirstWithStableIdOrderForMatchingDates() {
        var an = customer("An", 1);
        LocalDateTime date = LocalDateTime.of(2026, 10, 1, 0, 0);
        var show = showtime(movie("Film", genre("Action"), MovieStatus.SHOWING), room("Room"), date.plusDays(10), ShowtimeStatus.OPEN);
        Booking oldest = booking(an, show, BookingStatus.PENDING, date.minusDays(1));
        Booking sameDateFirst = booking(an, show, BookingStatus.PENDING, date);
        Booking sameDateLast = booking(an, show, BookingStatus.PENDING, date);
        flushAndClear();
        var page = bookings.search(null, null, null, null, PageRequest.of(0, 2));
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).extracting(Booking::getBookingId)
                .containsExactly(sameDateLast.getBookingId(), sameDateFirst.getBookingId());
        assertThat(bookings.search(null, null, null, null, PageRequest.of(1, 2)).getContent())
                .extracting(Booking::getBookingId).containsExactly(oldest.getBookingId());
    }
}
