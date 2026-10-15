package com.fu.cinema.repository;

import com.fu.cinema.enums.*;
import com.fu.cinema.exception.*;
import com.fu.cinema.service.SeatMapService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;

@Import(SeatMapService.class)
class SeatMapServiceTests extends JpaFixture {
    @Autowired SeatMapService service;

    @Test
    void buildsSeatRowsAndSoldSeatsWithDetachedMovieAndRoom() {
        var an = customer("An", 1);
        var room = room("Room");
        room.setTotalRows(7);
        var show = showtime(movie("Film", genre("Action"), MovieStatus.SHOWING), room,
                LocalDateTime.now().plusDays(1), ShowtimeStatus.OPEN);
        booking(an, show, BookingStatus.PENDING, LocalDateTime.now(), "E5", "E6");
        booking(an, show, BookingStatus.CANCELLED, LocalDateTime.now(), "F1");
        flushAndClear();
        var map = service.getSeatMap(show.getShowtimeId());
        em.clear();
        assertThat(map.getRowLabels()).containsExactly("A", "B", "C", "D", "E", "F", "G");
        assertThat(map.getSeatsPerRow()).isEqualTo(10);
        assertThat(map.isSold("E5")).isTrue();
        assertThat(map.isSold("E6")).isTrue();
        assertThat(map.isSold("F1")).isFalse();
        assertThat(map.getShowtime().getMovie().getTitle()).isEqualTo("Film");
        assertThat(map.getShowtime().getRoom().getRoomName()).isEqualTo("Room");
    }

    @Test
    void closedPastAndMissingShowtimesAreRejected() {
        var film = movie("Film", genre("Action"), MovieStatus.SHOWING);
        var room = room("Room");
        var closed = showtime(film, room, LocalDateTime.now().plusDays(1), ShowtimeStatus.CANCELLED);
        var past = showtime(film, room, LocalDateTime.now().minusMinutes(1), ShowtimeStatus.OPEN);
        flushAndClear();
        assertThatThrownBy(() -> service.getSeatMap(closed.getShowtimeId())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.getSeatMap(past.getShowtimeId())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.getSeatMap(-1L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
