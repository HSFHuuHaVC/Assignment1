package com.fu.cinema.desktop.facade;

import com.fu.cinema.desktop.model.DashboardData;
import com.fu.cinema.desktop.model.RoomCount;
import com.fu.cinema.service.BookingService;
import com.fu.cinema.service.CustomerService;
import com.fu.cinema.service.MovieService;
import com.fu.cinema.service.ShowtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DashboardFacade {

    private final MovieService movieService;
    private final CustomerService customerService;
    private final BookingService bookingService;
    private final ShowtimeService showtimeService;

    public DashboardData load() {
        List<RoomCount> rooms = showtimeService.countTodayByRoom().stream()
                .map(r -> new RoomCount(r.getRoomName(), r.getShowtimeCount()))
                .toList();
        return new DashboardData(movieService.countShowing(), customerService.count(),
                bookingService.countPending(), rooms);
    }
}
