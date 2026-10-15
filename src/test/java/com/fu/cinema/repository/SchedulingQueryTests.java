package com.fu.cinema.repository;

import com.fu.cinema.entity.*;
import com.fu.cinema.enums.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class SchedulingQueryTests extends JpaFixture {
    @Autowired ShowtimeRepository showtimes;
    @Autowired TicketRepository tickets;

    @Test
    void q3FindsOnlyIntersectingOpenShowtimesInSameRoom() {
        Movie movie = movie("Film", genre("Action"), MovieStatus.SHOWING);
        Room room = room("Room 01");
        LocalDateTime start = LocalDateTime.of(2026, 10, 15, 9, 0);
        Showtime match = showtime(movie, room, start, ShowtimeStatus.OPEN);
        showtime(movie, room, start.plusHours(3), ShowtimeStatus.CANCELLED);
        showtime(movie, room("Room 02"), start, ShowtimeStatus.OPEN);
        flushAndClear();
        assertThat(showtimes.findOverlapping(room.getRoomId(), start.plusMinutes(30), start.plusHours(1), null))
                .extracting(Showtime::getShowtimeId).containsExactly(match.getShowtimeId());
        assertThat(showtimes.findOverlapping(room.getRoomId(), start.minusHours(1), start.plusHours(3), null))
                .extracting(Showtime::getShowtimeId).containsExactly(match.getShowtimeId());
        assertThat(showtimes.findOverlapping(room.getRoomId(), start.plusHours(3), start.plusHours(4), null)).isEmpty();
        assertThat(showtimes.findOverlapping(room.getRoomId(), start, start.plusHours(2), match.getShowtimeId())).isEmpty();
    }

    @Test
    void q3AllowsAdjacentShowtimesAtBothBoundaries() {
        Movie movie = movie("Film", genre("Action"), MovieStatus.SHOWING);
        Room room = room("Room 01");
        LocalDateTime start = LocalDateTime.of(2026, 10, 15, 9, 0);
        showtime(movie, room, start, ShowtimeStatus.OPEN);
        flushAndClear();
        assertThat(showtimes.findOverlapping(room.getRoomId(), start.minusHours(2), start, null)).isEmpty();
        assertThat(showtimes.findOverlapping(room.getRoomId(), start.plusHours(2), start.plusHours(4), null)).isEmpty();
    }

    @Test
    void q4ReservesPendingAndConfirmedSeatsAndReleasesCancelledOnes() {
        Customer customer = customer("Alice", 1);
        Movie movie = movie("Film", genre("Action"), MovieStatus.SHOWING);
        Room room = room("Room 01");
        LocalDateTime start = LocalDateTime.of(2026, 10, 15, 9, 0);
        Showtime showtime = showtime(movie, room, start, ShowtimeStatus.OPEN);
        Showtime other = showtime(movie, room, start.plusDays(1), ShowtimeStatus.OPEN);
        booking(customer, showtime, BookingStatus.PENDING, start, "A1");
        booking(customer, showtime, BookingStatus.CONFIRMED, start, "A2");
        booking(customer, showtime, BookingStatus.CANCELLED, start, "A3");
        booking(customer, other, BookingStatus.CONFIRMED, start, "A4");
        flushAndClear();
        assertThat(tickets.findSoldSeatCodes(showtime.getShowtimeId())).containsExactlyInAnyOrder("A1", "A2");
        assertThat(tickets.findSoldSeatCodes(-1L)).isEmpty();
    }

    @Test
    void pessimisticLockQueryWorksInsideTransaction() {
        Showtime showtime = showtime(movie("Film", genre("Action"), MovieStatus.SHOWING), room("Room 01"),
                LocalDateTime.of(2026, 10, 15, 9, 0), ShowtimeStatus.OPEN);
        flushAndClear();
        assertThat(showtimes.findByIdForUpdate(showtime.getShowtimeId())).isPresent();
        assertThat(showtimes.findByIdForUpdate(-1L)).isEmpty();
    }
}
