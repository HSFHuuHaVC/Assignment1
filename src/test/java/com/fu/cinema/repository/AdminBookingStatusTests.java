package com.fu.cinema.repository;

import com.fu.cinema.enums.*;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.exception.ResourceNotFoundException;
import com.fu.cinema.service.BookingService;
import com.fu.cinema.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;

@Import(BookingServiceImpl.class)
class AdminBookingStatusTests extends JpaFixture {
    @Autowired BookingService service;
    @Autowired BookingRepository bookings;
    @Autowired TicketRepository tickets;

    private Long seed(BookingStatus status) {
        var an = customer("An", 1);
        var show = showtime(movie("Film", genre("Action"), MovieStatus.SHOWING), room("Room"),
                LocalDateTime.now().plusDays(1), ShowtimeStatus.OPEN);
        var booking = booking(an, show, status, LocalDateTime.of(2026, 10, 15, 12, 0), "A1", "B2");
        flushAndClear();
        return booking.getBookingId();
    }

    @ParameterizedTest
    @EnumSource(BookingStatus.class)
    void confirmsOnlyPendingAndRejectsEveryOtherInitialStatus(BookingStatus initial) {
        Long id = seed(initial);
        if (initial == BookingStatus.PENDING) {
            service.confirm(id);
        } else {
            assertThatThrownBy(() -> service.confirm(id)).isInstanceOf(BusinessException.class)
                    .hasMessage("Chỉ xác nhận được đơn đang ở trạng thái PENDING");
        }
        flushAndClear();
        var actual = service.getDetail(id);
        assertThat(actual.getStatus()).isEqualTo(initial == BookingStatus.PENDING ? BookingStatus.CONFIRMED : initial);
        assertThat(actual.getTotalAmount()).isEqualByComparingTo("140000");
        assertThat(actual.getBookingDate()).isEqualTo(LocalDateTime.of(2026, 10, 15, 12, 0));
        assertThat(tickets.count()).isEqualTo(2);
    }

    @ParameterizedTest
    @EnumSource(BookingStatus.class)
    void cancellationPreservesHistoryAndReleasesSeatsWithoutAllowingCancelledToChange(BookingStatus initial) {
        Long id = seed(initial);
        Long showtimeId = service.getDetail(id).getShowtime().getShowtimeId();
        if (initial == BookingStatus.CANCELLED) {
            assertThatThrownBy(() -> service.cancelByAdmin(id)).isInstanceOf(BusinessException.class)
                    .hasMessage("Đơn đã được huỷ trước đó");
        } else {
            service.cancelByAdmin(id);
        }
        flushAndClear();
        assertThat(bookings.findById(id).orElseThrow().getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(tickets.count()).isEqualTo(2);
        assertThat(tickets.findSoldSeatCodes(showtimeId)).isEmpty();
    }

    @Test
    void statusChangeRequiresExistingBooking() {
        assertThatThrownBy(() -> service.confirm(-1L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.cancelByAdmin(-1L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
