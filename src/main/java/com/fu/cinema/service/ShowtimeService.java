package com.fu.cinema.service;

import com.fu.cinema.dto.RoomShowtimeCount;
import com.fu.cinema.dto.SeatMap;
import com.fu.cinema.dto.ShowtimeForm;
import com.fu.cinema.entity.Showtime;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public interface ShowtimeService {

    Page<Showtime> search(Long movieId, Long roomId, LocalDate date, int page, int size);

    ShowtimeForm getForm(Long showtimeId);

    void create(ShowtimeForm form);

    void update(Long showtimeId, ShowtimeForm form);

    /** @return true nếu đã xoá hẳn, false nếu chỉ huỷ suất chiếu (BR-09) */
    boolean delete(Long showtimeId);

    List<Showtime> findUpcomingByMovie(Long movieId);

    SeatMap getSeatMap(Long showtimeId);

    List<RoomShowtimeCount> countTodayByRoom();
}
