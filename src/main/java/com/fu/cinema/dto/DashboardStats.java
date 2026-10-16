package com.fu.cinema.dto;

import lombok.Value;

import java.util.List;

@Value
public class DashboardStats {

    long showingMovies;
    long customers;
    long pendingBookings;
    List<RoomShowtimeCount> todayShowtimesByRoom;
}
