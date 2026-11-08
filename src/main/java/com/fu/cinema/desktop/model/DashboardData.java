package com.fu.cinema.desktop.model;

import java.util.List;

public record DashboardData(long showingMovies, long customers, long pendingBookings, List<RoomCount> todayByRoom) {}
