package com.fu.cinema.desktop;

import com.fu.cinema.desktop.facade.RoomFacade;
import com.fu.cinema.desktop.facade.ShowtimeFacade;
import com.fu.cinema.desktop.model.PageResult;
import com.fu.cinema.desktop.model.RoomItem;
import com.fu.cinema.desktop.model.RoomRow;
import com.fu.cinema.desktop.model.ScheduleMovieItem;
import com.fu.cinema.desktop.model.ShowtimeRow;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.dto.RoomForm;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.dto.ShowtimeForm;
import com.fu.cinema.enums.Role;
import com.fu.cinema.enums.RoomStatus;
import com.fu.cinema.enums.RoomType;
import com.fu.cinema.enums.ShowtimeStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.datasource.url=jdbc:h2:mem:desktopSchedule;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles({"test", "desktop"})
class ScheduleFacadeTests {

    @Autowired private RoomFacade roomFacade;
    @Autowired private ShowtimeFacade showtimeFacade;
    @Autowired private DesktopSession session;

    @BeforeEach
    void setupAdmin() {
        session.login(new SessionUser(1L, null, "Administrator", Role.ADMIN));
    }

    @AfterEach
    void logout() {
        session.logout();
    }

    @Test
    void roomFacadeFindAllReturnsRooms() {
        List<RoomRow> rooms = roomFacade.findAll();
        assertNotNull(rooms);
        assertFalse(rooms.isEmpty());
        RoomRow first = rooms.get(0);
        assertNotNull(first.name());
        assertTrue(first.capacity() > 0);
    }

    @Test
    void showtimeFacadeDropdownsReturnData() {
        List<ScheduleMovieItem> movies = showtimeFacade.schedulableMovies();
        assertNotNull(movies);
        assertFalse(movies.isEmpty());
        assertTrue(movies.get(0).duration() > 0);

        List<RoomItem> rooms = showtimeFacade.activeRooms();
        assertNotNull(rooms);
        assertFalse(rooms.isEmpty());
    }

    @Test
    void showtimeFacadeSearchReturnsPage() {
        PageResult<ShowtimeRow> page = showtimeFacade.search(null, null, null, 0);
        assertNotNull(page);
        assertNotNull(page.items());
    }

    @Test
    @Transactional
    void roomFacadeCreateAndUpdate() {
        RoomForm form = new RoomForm();
        form.setRoomName("Phòng VIP Test 99");
        form.setRoomType(RoomType.IMAX);
        form.setTotalRows(8);
        form.setSeatsPerRow(10);
        form.setStatus(RoomStatus.ACTIVE);

        roomFacade.save(form);

        List<RoomRow> rooms = roomFacade.findAll();
        RoomRow created = rooms.stream()
                .filter(r -> "Phòng VIP Test 99".equals(r.name()))
                .findFirst()
                .orElse(null);
        assertNotNull(created);
        assertEquals(80, created.capacity());

        // Update
        RoomForm editForm = roomFacade.getForm(created.id());
        editForm.setRoomName("Phòng VIP Đã Đổi Tên");
        roomFacade.save(editForm);

        RoomForm updated = roomFacade.getForm(created.id());
        assertEquals("Phòng VIP Đã Đổi Tên", updated.getRoomName());
    }

    @Test
    @Transactional
    void showtimeFacadeCreateAndUpdate() {
        List<ScheduleMovieItem> movies = showtimeFacade.schedulableMovies();
        List<RoomItem> rooms = showtimeFacade.activeRooms();

        ShowtimeForm form = new ShowtimeForm();
        form.setMovieId(movies.get(0).id());
        form.setRoomId(rooms.get(0).id());
        form.setStartTime(LocalDate.now().plusDays(2).atTime(20, 0));
        form.setTicketPrice(new BigDecimal("90000"));
        form.setStatus(ShowtimeStatus.OPEN);

        showtimeFacade.save(form);

        PageResult<ShowtimeRow> result = showtimeFacade.search(movies.get(0).id(), rooms.get(0).id(), LocalDate.now().plusDays(2), 0);
        assertNotNull(result);
        assertFalse(result.items().isEmpty());

        ShowtimeRow created = result.items().get(0);
        assertNotNull(created.id());
        assertEquals(new BigDecimal("90000"), created.price());

        // Delete
        boolean deleted = showtimeFacade.delete(created.id());
        assertTrue(deleted);
    }
}

