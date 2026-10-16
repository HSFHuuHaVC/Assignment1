package com.fu.cinema.service.impl;

import com.fu.cinema.dto.RoomShowtimeCount;
import com.fu.cinema.dto.SeatMap;
import com.fu.cinema.dto.ShowtimeForm;
import com.fu.cinema.entity.Movie;
import com.fu.cinema.entity.Room;
import com.fu.cinema.entity.Showtime;
import com.fu.cinema.enums.MovieStatus;
import com.fu.cinema.enums.RoomStatus;
import com.fu.cinema.enums.ShowtimeStatus;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.exception.ResourceNotFoundException;
import com.fu.cinema.repository.*;
import com.fu.cinema.service.ShowtimeService;
import com.fu.cinema.util.DateUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShowtimeServiceImpl implements ShowtimeService {

    static final int CLEANING_MINUTES = 15;

    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;

    @Override
    public Page<Showtime> search(Long movieId, Long roomId, LocalDate date, int page, int size) {
        LocalDateTime from = date == null ? null : date.atStartOfDay();
        LocalDateTime to = date == null ? null : date.plusDays(1).atStartOfDay();
        return showtimeRepository.search(movieId, roomId, from, to,
                PageRequest.of(Math.max(page, 0), size, Sort.by("startTime").descending()));
    }

    @Override
    public ShowtimeForm getForm(Long showtimeId) {
        Showtime s = findDetail(showtimeId);
        ShowtimeForm f = new ShowtimeForm();
        f.setShowtimeId(s.getShowtimeId());
        f.setMovieId(s.getMovie().getMovieId());
        f.setRoomId(s.getRoom().getRoomId());
        f.setStartTime(s.getStartTime());
        f.setEndTime(s.getEndTime());
        f.setTicketPrice(s.getTicketPrice());
        f.setStatus(s.getStatus());
        return f;
    }

    @Override
    @Transactional
    public void create(ShowtimeForm form) {
        Showtime showtime = new Showtime();
        apply(form, showtime, null);
        showtimeRepository.save(showtime);
    }

    @Override
    @Transactional
    public void update(Long showtimeId, ShowtimeForm form) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Suất chiếu", showtimeId));
        boolean hasBookings = bookingRepository.existsByShowtime_ShowtimeId(showtimeId);
        if (hasBookings && (!showtime.getMovie().getMovieId().equals(form.getMovieId())
                || !showtime.getRoom().getRoomId().equals(form.getRoomId()))) {
            throw new BusinessException("Suất chiếu đã có người đặt vé: không được đổi phim hoặc phòng");
        }
        apply(form, showtime, showtimeId);
        if (showtime.getStatus() == ShowtimeStatus.CANCELLED) {
            // The edit form must cancel bookings just like the delete action (BR-09).
            // Q9 flushes the showtime change before clearing the persistence context.
            bookingRepository.cancelAllByShowtime(showtimeId);
        }
    }

    @Override
    @Transactional
    public boolean delete(Long showtimeId) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Suất chiếu", showtimeId));
        if (bookingRepository.existsByShowtime_ShowtimeId(showtimeId)) { // BR-09
            showtime.setStatus(ShowtimeStatus.CANCELLED);
            bookingRepository.cancelAllByShowtime(showtimeId); // Q9
            return false;
        }
        showtimeRepository.delete(showtime);
        return true;
    }

    @Override
    public List<Showtime> findUpcomingByMovie(Long movieId) {
        return showtimeRepository.findByMovie_MovieIdAndStatusAndStartTimeAfterOrderByStartTimeAsc(
                movieId, ShowtimeStatus.OPEN, LocalDateTime.now());
    }

    @Override
    public SeatMap getSeatMap(Long showtimeId) {
        Showtime s = findDetail(showtimeId);
        List<String> rows = IntStream.range(0, s.getRoom().getTotalRows())
                .mapToObj(i -> String.valueOf((char) ('A' + i)))
                .toList();
        return new SeatMap(s, rows, s.getRoom().getSeatsPerRow(),
                new HashSet<>(ticketRepository.findSoldSeatCodes(showtimeId)));
    }

    @Override
    public List<RoomShowtimeCount> countTodayByRoom() {
        return showtimeRepository.countTodayShowtimesByRoom();
    }

    // ================= helpers =================

    private Showtime findDetail(Long id) {
        return showtimeRepository.findDetailById(id).orElseThrow(() -> new ResourceNotFoundException("Suất chiếu", id));
    }

    private void apply(ShowtimeForm form, Showtime showtime, Long excludeId) {
        Movie movie = movieRepository.findById(form.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Phim", form.getMovieId()));
        Room room = roomRepository.findById(form.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Phòng chiếu", form.getRoomId()));

        if (movie.getStatus() == MovieStatus.STOPPED) {
            throw new BusinessException("Phim \"" + movie.getTitle() + "\" đã ngừng chiếu");
        }
        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new BusinessException("Phòng \"" + room.getRoomName() + "\" đang bảo trì");
        }

        LocalDateTime start = form.getStartTime();
        LocalDateTime end = form.getEndTime() != null
                ? form.getEndTime()
                : start.plusMinutes(movie.getDuration() + CLEANING_MINUTES); // gợi ý EndTime
        if (!start.isBefore(end)) {
            throw new BusinessException("Giờ bắt đầu phải trước giờ kết thúc");
        }
        if (end.isBefore(start.plusMinutes(movie.getDuration()))) {
            throw new BusinessException("Giờ kết thúc phải sau giờ bắt đầu ít nhất " + movie.getDuration() + " phút (thời lượng phim)");
        }

        if (form.getStatus() != ShowtimeStatus.CANCELLED) {
            List<Showtime> overlaps = showtimeRepository.findOverlapping(room.getRoomId(), start, end, excludeId);
            if (!overlaps.isEmpty()) { // BR-08
                Showtime o = overlaps.get(0);
                throw new BusinessException("Trùng lịch với suất \"" + o.getMovie().getTitle() + "\" ("
                        + DateUtils.format(o.getStartTime()) + " – " + DateUtils.format(o.getEndTime())
                        + ") tại " + room.getRoomName());
            }
        }

        showtime.setMovie(movie);
        showtime.setRoom(room);
        showtime.setStartTime(start);
        showtime.setEndTime(end);
        showtime.setTicketPrice(form.getTicketPrice());
        showtime.setStatus(form.getStatus());
    }
}
