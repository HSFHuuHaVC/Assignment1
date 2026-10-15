package com.fu.cinema.repository;

import com.fu.cinema.enums.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;

class SeatAvailabilityTests extends JpaFixture {
    @Autowired TicketRepository tickets;

    @Test
    void soldSeatsIncludePendingAndConfirmedExcludeCancelledAndStayWithinShowtime() {
        var an = customer("An", 1);
        var film = movie("Film", genre("Action"), MovieStatus.SHOWING);
        var room = room("Room");
        var show = showtime(film, room, LocalDateTime.now().plusDays(1), ShowtimeStatus.OPEN);
        var other = showtime(film, room, LocalDateTime.now().plusDays(2), ShowtimeStatus.OPEN);
        booking(an, show, BookingStatus.PENDING, LocalDateTime.now(), "E5");
        booking(an, show, BookingStatus.CONFIRMED, LocalDateTime.now(), "E6");
        booking(an, show, BookingStatus.CANCELLED, LocalDateTime.now(), "F1", "E5");
        booking(an, other, BookingStatus.PENDING, LocalDateTime.now(), "G1");
        flushAndClear();
        assertThat(tickets.findSoldSeatCodes(show.getShowtimeId())).containsExactly("E5", "E6");
        assertThat(tickets.findSoldSeatCodes(other.getShowtimeId())).containsExactly("G1");
        assertThat(tickets.findSoldSeatCodes(-1L)).isEmpty();
    }
}
