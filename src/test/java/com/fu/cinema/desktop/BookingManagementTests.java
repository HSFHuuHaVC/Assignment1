package com.fu.cinema.desktop;

import com.fu.cinema.desktop.facade.AuthFacade;
import com.fu.cinema.desktop.facade.BookingFacade;
import com.fu.cinema.desktop.model.BookingDetail;
import com.fu.cinema.desktop.model.BookingRow;
import com.fu.cinema.desktop.model.PageResult;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.enums.BookingStatus;
import com.fu.cinema.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.datasource.url=jdbc:h2:mem:desktopBooking;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles({"test", "desktop"})
class BookingManagementTests {

    @Autowired BookingFacade bookingFacade;
    @Autowired AuthFacade authFacade;
    @Autowired DesktopSession session;

    @AfterEach
    void clearSession() {
        session.logout();
    }

    @Test
    void testSearchBookings() {
        PageResult<BookingRow> result = bookingFacade.search(null, null, null, "", 0);
        assertNotNull(result);
        assertFalse(result.items().isEmpty(), "Dữ liệu seed phải có các đơn đặt vé mẫu");

        BookingRow row = result.items().getFirst();
        assertNotNull(row.id());
        assertNotNull(row.customerName());
        assertNotNull(row.movieTitle());
        assertNotNull(row.status());
    }

    @Test
    void testFilterByStatus() {
        PageResult<BookingRow> confirmed = bookingFacade.search(BookingStatus.CONFIRMED, null, null, "", 0);
        assertNotNull(confirmed);
        assertTrue(confirmed.items().stream().allMatch(r -> r.status() == BookingStatus.CONFIRMED));

        PageResult<BookingRow> pending = bookingFacade.search(BookingStatus.PENDING, null, null, "", 0);
        assertNotNull(pending);
        assertTrue(pending.items().stream().allMatch(r -> r.status() == BookingStatus.PENDING));
    }

    @Test
    void testBookingDetailWithoutLazyError() {
        PageResult<BookingRow> result = bookingFacade.search(null, null, null, "", 0);
        BookingRow row = result.items().getFirst();

        BookingDetail detail = bookingFacade.detail(row.id());
        assertNotNull(detail);
        assertEquals(row.id(), detail.id());
        assertNotNull(detail.customerName());
        assertNotNull(detail.movieTitle());
        assertNotNull(detail.roomName());
        assertNotNull(detail.seats());
        assertFalse(detail.seats().isEmpty());
    }

    @Test
    void testAdminRequiredForStateChanges() {
        assertThrows(BusinessException.class, () -> bookingFacade.confirm(1L));
        assertThrows(BusinessException.class, () -> bookingFacade.cancel(1L));
    }

    @Test
    @Transactional
    void testConfirmAndCancelFlow() {
        var admin = authFacade.loginAdmin("admin@fucinema.vn", "Admin@123");
        session.login(admin);

        PageResult<BookingRow> pendingList = bookingFacade.search(BookingStatus.PENDING, null, null, "", 0);
        if (!pendingList.items().isEmpty()) {
            Long pendingId = pendingList.items().getFirst().id();

            // Xác nhận đơn PENDING -> CONFIRMED
            bookingFacade.confirm(pendingId);
            BookingDetail afterConfirm = bookingFacade.detail(pendingId);
            assertEquals(BookingStatus.CONFIRMED, afterConfirm.status());

            // Huỷ đơn CONFIRMED -> CANCELLED
            bookingFacade.cancel(pendingId);
            BookingDetail afterCancel = bookingFacade.detail(pendingId);
            assertEquals(BookingStatus.CANCELLED, afterCancel.status());
        }
    }
}

