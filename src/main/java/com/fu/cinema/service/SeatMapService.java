package com.fu.cinema.service;

import com.fu.cinema.dto.SeatMap;
import com.fu.cinema.enums.ShowtimeStatus;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.exception.ResourceNotFoundException;
import com.fu.cinema.repository.ShowtimeRepository;
import com.fu.cinema.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.stream.IntStream;

/** Seat selection reads only; independent of the separately assigned showtime CRUD implementation. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeatMapService {
    private final ShowtimeRepository showtimes;
    private final TicketRepository tickets;

    public SeatMap getSeatMap(Long showtimeId) {
        var showtime = showtimes.findDetailById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Suất chiếu", showtimeId));
        if (showtime.getStatus() != ShowtimeStatus.OPEN
                || !showtime.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException("Suất chiếu không còn mở bán");
        }
        var rows = IntStream.range(0, showtime.getRoom().getTotalRows())
                .mapToObj(i -> String.valueOf((char) ('A' + i))).toList();
        return new SeatMap(showtime, rows, showtime.getRoom().getSeatsPerRow(),
                new HashSet<>(tickets.findSoldSeatCodes(showtimeId)));
    }
}
