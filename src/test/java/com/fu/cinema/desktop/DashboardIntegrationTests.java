package com.fu.cinema.desktop;

import com.fu.cinema.controller.admin.AdminDashboardController;
import com.fu.cinema.desktop.facade.DashboardFacade;
import com.fu.cinema.desktop.model.RoomCount;
import com.fu.cinema.dto.DashboardStats;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.ui.ExtendedModelMap;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class DashboardIntegrationTests {
    @Autowired private DashboardFacade facade;
    @Autowired private AdminDashboardController web;

    @Test
    void webAndDesktopReadTheSameDatabaseStatistics() {
        var model = new ExtendedModelMap();
        assertEquals("admin/dashboard", web.dashboard(model));
        var webData = (DashboardStats) model.get("stats");
        assertNotNull(webData);
        var desktopData = facade.load();
        assertAll(
                () -> assertEquals(webData.getShowingMovies(), desktopData.showingMovies()),
                () -> assertEquals(webData.getCustomers(), desktopData.customers()),
                () -> assertEquals(webData.getPendingBookings(), desktopData.pendingBookings()),
                () -> assertEquals(webData.getTodayShowtimesByRoom().stream()
                        .map(r -> new RoomCount(r.getRoomName(), r.getShowtimeCount())).toList(),
                        desktopData.todayByRoom()));
    }
}
