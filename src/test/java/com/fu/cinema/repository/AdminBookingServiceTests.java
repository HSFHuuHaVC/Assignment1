package com.fu.cinema.repository;

import com.fu.cinema.enums.*;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.exception.ResourceNotFoundException;
import com.fu.cinema.service.BookingService;
import com.fu.cinema.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import java.time.LocalDate;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;

@Import(BookingServiceImpl.class)
class AdminBookingServiceTests extends JpaFixture {
    @Autowired BookingService service;

    @Test
    void convertsInclusiveDatesAndNormalizesKeywordBeforeSearching() {
        var an = customer("Nguyen Van An", 1);
        LocalDate day = LocalDate.of(2026, 10, 15);
        var show = showtime(movie("Film", genre("Action"), MovieStatus.SHOWING), room("Room"),
                day.plusDays(10).atTime(10, 0), ShowtimeStatus.OPEN);
        var first = booking(an, show, BookingStatus.PENDING, day.atStartOfDay());
        var last = booking(an, show, BookingStatus.PENDING, day.atTime(23, 59, 59));
        booking(an, show, BookingStatus.PENDING, day.plusDays(1).atStartOfDay());
        booking(an, show, BookingStatus.CONFIRMED, day.atTime(12, 0));
        flushAndClear();
        var result = service.search(BookingStatus.PENDING, day, day, " NGUYEN ", -1, 10);
        assertThat(result.getContent()).extracting(com.fu.cinema.entity.Booking::getBookingId)
                .containsExactly(last.getBookingId(), first.getBookingId());
        assertThat(service.search(null, null, null, " ", 0, 0).getTotalElements()).isEqualTo(4);
        assertThat(service.search(null, null, null, null, 0, 0).getSize()).isEqualTo(10);
        assertThatThrownBy(() -> service.search(null, day.plusDays(1), day, null, 0, 10))
                .isInstanceOf(BusinessException.class).hasMessage("Từ ngày phải trước hoặc bằng đến ngày");
    }

    @Test
    void detailIsUsableAfterPersistenceContextClosesIncludingSortedTickets() {
        var an = customer("Nguyen Van An", 1);
        var show = showtime(movie("Test film", genre("Action"), MovieStatus.SHOWING), room("Room 01"),
                LocalDateTime.now().plusDays(1), ShowtimeStatus.OPEN);
        var booking = booking(an, show, BookingStatus.PENDING, LocalDateTime.now(), "B2", "A1");
        flushAndClear();
        var detail = service.getDetail(booking.getBookingId());
        em.clear();
        assertThat(detail.getCustomer().getFullName()).isEqualTo("Nguyen Van An");
        assertThat(detail.getCustomer().getEmail()).isEqualTo("user1@example.test");
        assertThat(detail.getCustomer().getMobile()).isEqualTo(an.getMobile());
        assertThat(detail.getShowtime().getMovie().getTitle()).isEqualTo("Test film");
        assertThat(detail.getShowtime().getRoom().getRoomName()).isEqualTo("Room 01");
        assertThat(detail.getTickets()).extracting(com.fu.cinema.entity.Ticket::getSeatCode).containsExactly("A1", "B2");
        assertThat(detail.getTotalAmount()).isEqualByComparingTo("140000");
    }

    @Test
    void detailSupportsEmptyTicketsAndMissingBookingReturns404Exception() {
        var an = customer("An", 1);
        var show = showtime(movie("Film", genre("Action"), MovieStatus.SHOWING), room("Room"),
                LocalDateTime.now().plusDays(1), ShowtimeStatus.OPEN);
        var booking = booking(an, show, BookingStatus.CANCELLED, LocalDateTime.now());
        flushAndClear();
        var detail = service.getDetail(booking.getBookingId());
        em.clear();
        assertThat(detail.getTickets()).isEmpty();
        assertThatThrownBy(() -> service.getDetail(-1L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void countsOnlyPendingBookings() {
        var an = customer("An", 1);
        var show = showtime(movie("Film", genre("Action"), MovieStatus.SHOWING), room("Room"),
                LocalDateTime.now().plusDays(1), ShowtimeStatus.OPEN);
        for (BookingStatus status : BookingStatus.values()) booking(an, show, status, LocalDateTime.now());
        flushAndClear();
        assertThat(service.countPending()).isEqualTo(1);
    }
}
