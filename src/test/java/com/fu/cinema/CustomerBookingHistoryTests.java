package com.fu.cinema;

import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.entity.Booking;
import com.fu.cinema.enums.*;
import com.fu.cinema.service.BookingService;
import com.fu.cinema.util.SessionUtils;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:booking_history;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class CustomerBookingHistoryTests {
    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    @Autowired BookingService service;
    private List<Booking> data;
    private Long customerId;

    @BeforeEach void seed() {
        data = CustomerHistoryFixture.seed(em);
        customerId = data.getFirst().getCustomer().getCustomerId();
        em.clear();
    }

    private MockHttpSession session(Role role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionUtils.CURRENT_USER,
                new SessionUser(-1L, role == Role.CUSTOMER ? customerId : null, "An", role));
        return session;
    }

    @Test
    @SuppressWarnings("unchecked")
    void historyShowsOnlySessionCustomerNewestFirstAndIgnoresForgedCustomerId() throws Exception {
        var result = mvc.perform(get("/customer/bookings").session(session(Role.CUSTOMER))
                        .queryParam("customerId", data.get(1).getCustomer().getCustomerId().toString()))
                .andExpect(status().isOk()).andExpect(view().name("customer/bookings")).andReturn();
        Page<Booking> page = (Page<Booking>) result.getModelAndView().getModel().get("bookings");
        assertThat(page.getTotalElements()).isEqualTo(12);
        assertThat(page.getSize()).isEqualTo(10);
        assertThat(page.getContent()).allSatisfy(b -> assertThat(b.getCustomer().getCustomerId()).isEqualTo(customerId));
        assertThat(page.getContent()).extracting(Booking::getBookingId)
                .containsExactly(data.get(22).getBookingId(), data.get(20).getBookingId(), data.get(18).getBookingId(),
                        data.get(16).getBookingId(), data.get(14).getBookingId(), data.get(12).getBookingId(),
                        data.get(10).getBookingId(), data.get(8).getBookingId(), data.get(6).getBookingId(), data.get(4).getBookingId());
    }

    @Test
    void statusPaginationRetainsFilterAndSecondPageContainsRemainingOwnedBooking() throws Exception {
        String html = mvc.perform(get("/customer/bookings").session(session(Role.CUSTOMER))
                        .queryParam("status", "CONFIRMED").queryParam("page", "0"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("status=CONFIRMED&amp;page=1", "Room 01", "Test film", "140,000đ")
                .doesNotContain("/customer/bookings/" + data.getFirst().getBookingId() + "\"");
        String next = mvc.perform(get("/customer/bookings").session(session(Role.CUSTOMER))
                        .queryParam("status", "CONFIRMED").queryParam("page", "1"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(next).contains("/customer/bookings/" + data.get(2).getBookingId() + "\"", "status=CONFIRMED&amp;page=0");
    }

    @Test
    void pageGraphKeepsMovieAndRoomAvailableAfterPersistenceContextCloses() {
        var page = service.findByCustomer(customerId, BookingStatus.CONFIRMED, -1, 10);
        em.clear();
        assertThat(page.getTotalElements()).isEqualTo(11);
        assertThat(page.getNumber()).isZero();
        page.forEach(booking -> {
            assertThat(booking.getShowtime().getMovie().getTitle()).isEqualTo("Test film");
            assertThat(booking.getShowtime().getRoom().getRoomName()).isEqualTo("Room 01");
        });
    }

    @Test
    void emptyFilterAndRoleChecksRenderCorrectly() throws Exception {
        String html = mvc.perform(get("/customer/bookings").session(session(Role.CUSTOMER)).queryParam("status", "CANCELLED"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Bạn chưa có đơn đặt vé nào").doesNotContain("aria-label=\"Phân trang\"");
        mvc.perform(get("/customer/bookings")).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/customer/bookings").session(session(Role.ADMIN))).andExpect(status().isForbidden());
    }
}
