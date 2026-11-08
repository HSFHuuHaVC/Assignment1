package com.fu.cinema.desktop;

import com.fu.cinema.desktop.facade.DashboardFacade;
import com.fu.cinema.desktop.model.RoomCount;
import com.fu.cinema.dto.RoomShowtimeCount;
import com.fu.cinema.service.BookingService;
import com.fu.cinema.service.CustomerService;
import com.fu.cinema.service.MovieService;
import com.fu.cinema.service.ShowtimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DashboardFacadeTest {
    private MovieService movies;
    private CustomerService customers;
    private BookingService bookings;
    private ShowtimeService showtimes;
    private DashboardFacade facade;

    @BeforeEach
    void setUp() {
        movies = mock(MovieService.class);
        customers = mock(CustomerService.class);
        bookings = mock(BookingService.class);
        showtimes = mock(ShowtimeService.class);
        facade = new DashboardFacade(movies, customers, bookings, showtimes);
    }

    @Test
    void loadsKpisAndRoomCountsFromTheSameServicesAsWebDashboard() {
        when(movies.countShowing()).thenReturn(7L);
        when(customers.count()).thenReturn(120L);
        when(bookings.countPending()).thenReturn(3L);
        RoomShowtimeCount room = mock(RoomShowtimeCount.class);
        when(room.getRoomName()).thenReturn("Phòng 1");
        when(room.getShowtimeCount()).thenReturn(5L);
        when(showtimes.countTodayByRoom()).thenReturn(List.of(room));

        var data = facade.load();

        assertAll(
                () -> assertEquals(7L, data.showingMovies()),
                () -> assertEquals(120L, data.customers()),
                () -> assertEquals(3L, data.pendingBookings()),
                () -> assertEquals(List.of(new RoomCount("Phòng 1", 5L)), data.todayByRoom()));
    }

    @Test
    void reloadsRoomCountsAfterNewShowtimeAndHandlesEmptyDay() {
        RoomShowtimeCount room = mock(RoomShowtimeCount.class);
        when(room.getRoomName()).thenReturn("Phòng 2");
        when(room.getShowtimeCount()).thenReturn(1L);
        when(showtimes.countTodayByRoom()).thenReturn(List.of(), List.of(room));

        assertTrue(facade.load().todayByRoom().isEmpty());
        assertEquals(List.of(new RoomCount("Phòng 2", 1L)), facade.load().todayByRoom());
        verify(showtimes, times(2)).countTodayByRoom();
        verify(movies, times(2)).countShowing();
        verify(customers, times(2)).count();
        verify(bookings, times(2)).countPending();
    }
}
