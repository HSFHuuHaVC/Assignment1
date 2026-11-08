package com.fu.cinema.desktop;

import com.fu.cinema.desktop.facade.AuthFacade;
import com.fu.cinema.desktop.facade.CounterSaleFacade;
import com.fu.cinema.desktop.model.CustomerItem;
import com.fu.cinema.desktop.model.MovieItem;
import com.fu.cinema.desktop.model.SaleResult;
import com.fu.cinema.desktop.model.SeatMapData;
import com.fu.cinema.desktop.model.ShowtimeItem;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.enums.BookingStatus;
import com.fu.cinema.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.datasource.url=jdbc:h2:mem:desktopCounterSale;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles({"test", "desktop"})
class CounterSaleTests {

    @Autowired CounterSaleFacade counterSaleFacade;
    @Autowired AuthFacade authFacade;
    @Autowired DesktopSession session;

    @AfterEach
    void clearSession() {
        session.logout();
    }

    @Test
    void testSeatOrderComparator() {
        List<String> raw = List.of("B1", "A10", "A2", "A1");
        List<String> sorted = raw.stream().sorted(CounterSaleFacade.SEAT_ORDER).toList();
        assertEquals(List.of("A1", "A2", "A10", "B1"), sorted, "Thứ tự ghế phải là A1 < A2 < A10 < B1");
    }

    @Test
    void testSellRequiresAdmin() {
        assertThrows(BusinessException.class, () ->
                counterSaleFacade.sell(1L, 1L, List.of("A1"), true));
    }

    @Test
    @Transactional
    void testSearchAndSellFlow() {
        // Đăng nhập Admin
        var admin = authFacade.loginAdmin("admin@fucinema.vn", "Admin@123");
        session.login(admin);

        // 1. Tìm khách
        List<CustomerItem> customers = counterSaleFacade.searchCustomers("An");
        assertFalse(customers.isEmpty());
        CustomerItem customer = customers.getFirst();

        // 2. Lấy danh sách phim
        List<MovieItem> movies = counterSaleFacade.movies();
        assertFalse(movies.isEmpty());
        MovieItem movie = movies.getFirst();

        // 3. Lấy suất chiếu sắp tới của phim
        List<ShowtimeItem> showtimes = counterSaleFacade.showtimes(movie.id());
        if (!showtimes.isEmpty()) {
            ShowtimeItem st = showtimes.getFirst();

            // 4. Lấy sơ đồ ghế
            SeatMapData seatMap = counterSaleFacade.seatMap(st.id());
            assertNotNull(seatMap);

            // Tìm ghế còn trống
            String availableSeat = seatMap.rowLabels().getFirst() + "3";
            if (!seatMap.soldSeats().contains(availableSeat)) {
                // 5. Bán vé đã thanh toán
                SaleResult result = counterSaleFacade.sell(customer.id(), st.id(), List.of(availableSeat), true);
                assertNotNull(result.bookingId());
                assertEquals(BookingStatus.CONFIRMED, result.status());
                assertTrue(result.seats().contains(availableSeat));
            }
        }
    }
}

