package com.fu.cinema.repository;

import com.fu.cinema.enums.*;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.service.BookingService;
import com.fu.cinema.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Import(BookingServiceImpl.class)
class CustomerCancellationRuleTests extends JpaFixture {
    @Autowired BookingService service;
    @Autowired BookingRepository bookings;
    @Autowired TicketRepository tickets;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 15, 12, 0);

    @ParameterizedTest
    @ValueSource(longs = {7200, 7201, 86400})
    void allowsCancellationAtExactlyTwoHoursOrEarlier(long secondsBeforeStart) {
        var an = customer("An", 1);
        var show = showtime(movie("Film", genre("Action"), MovieStatus.SHOWING), room("Room"),
                NOW.plusSeconds(secondsBeforeStart), ShowtimeStatus.OPEN);
        var booking = booking(an, show, BookingStatus.PENDING, NOW.minusDays(1), "A1", "A2");
        flushAndClear();
        try (var time = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            time.when(LocalDateTime::now).thenReturn(NOW);
            service.cancelByCustomer(booking.getBookingId(), an.getCustomerId());
        }
        flushAndClear();
        assertThat(bookings.findById(booking.getBookingId()).orElseThrow().getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(tickets.count()).isEqualTo(2);
        assertThat(tickets.findSoldSeatCodes(show.getShowtimeId())).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(longs = {7199, 0, -1, -7200})
    void refusesCancellationInsideTwoHoursOrAfterShowtime(long secondsBeforeStart) {
        var an = customer("An", 1);
        var show = showtime(movie("Film", genre("Action"), MovieStatus.SHOWING), room("Room"),
                NOW.plusSeconds(secondsBeforeStart), ShowtimeStatus.OPEN);
        var booking = booking(an, show, BookingStatus.CONFIRMED, NOW.minusDays(1), "A1");
        flushAndClear();
        try (var time = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            time.when(LocalDateTime::now).thenReturn(NOW);
            assertThatThrownBy(() -> service.cancelByCustomer(booking.getBookingId(), an.getCustomerId()))
                    .isInstanceOf(BusinessException.class).hasMessage("Chỉ được huỷ đơn trước giờ chiếu ít nhất 2 giờ");
        }
        flushAndClear();
        assertThat(bookings.findById(booking.getBookingId()).orElseThrow().getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(tickets.count()).isEqualTo(1);
    }

    @Test
    void alreadyCancelledBookingCannotBeCancelledAgain() {
        var an = customer("An", 1);
        var show = showtime(movie("Film", genre("Action"), MovieStatus.SHOWING), room("Room"),
                NOW.plusDays(1), ShowtimeStatus.OPEN);
        var booking = booking(an, show, BookingStatus.CANCELLED, NOW.minusDays(1), "A1");
        flushAndClear();
        assertThatThrownBy(() -> service.cancelByCustomer(booking.getBookingId(), an.getCustomerId()))
                .isInstanceOf(BusinessException.class).hasMessage("Đơn đã được huỷ trước đó");
    }
}
