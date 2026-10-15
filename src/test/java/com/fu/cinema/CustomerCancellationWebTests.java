package com.fu.cinema;

import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.entity.Booking;
import com.fu.cinema.enums.*;
import com.fu.cinema.repository.*;
import com.fu.cinema.util.SessionUtils;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:customer_cancel;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class CustomerCancellationWebTests {
    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    @Autowired BookingRepository bookings;
    @Autowired TicketRepository tickets;
    private List<Booking> data;
    @BeforeEach void seed() { data = AdminBookingFixture.seed(em, 3); em.clear(); }

    private MockHttpSession customer(Long id) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionUtils.CURRENT_USER, new SessionUser(-1L, id, "Customer", Role.CUSTOMER));
        return session;
    }

    @Test
    void pendingAndConfirmedCancellationUsePrgAndPreserveHistory() throws Exception {
        for (int i = 0; i < 2; i++) {
            Long id = data.get(i).getBookingId();
            var session = customer(data.get(i).getCustomer().getCustomerId());
            mvc.perform(post("/customer/bookings/" + id + "/cancel").session(session).with(csrf()))
                    .andExpect(redirectedUrl("/customer/bookings/" + id))
                    .andExpect(flash().attribute("success", "Đã huỷ đơn #" + id));
            String html = mvc.perform(get("/customer/bookings/" + id).session(session))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(html).contains("CANCELLED").doesNotContain("Bạn chắc chắn muốn huỷ đơn này?");
            mvc.perform(post("/customer/bookings/" + id + "/cancel").session(session).with(csrf()))
                    .andExpect(flash().attribute("error", "Đơn đã được huỷ trước đó"));
        }
        assertThat(bookings.count()).isEqualTo(3);
        assertThat(tickets.count()).isEqualTo(6);
    }

    @Test
    void tooLateCancellationReturnsErrorOnSameDetailWithoutReferer() throws Exception {
        Long id = data.getFirst().getBookingId();
        em.find(com.fu.cinema.entity.Showtime.class, data.getFirst().getShowtime().getShowtimeId())
                .setStartTime(LocalDateTime.now().plusMinutes(119));
        em.flush();
        em.clear();
        mvc.perform(post("/customer/bookings/" + id + "/cancel")
                        .session(customer(data.getFirst().getCustomer().getCustomerId())).with(csrf()))
                .andExpect(redirectedUrl("/customer/bookings/" + id))
                .andExpect(flash().attribute("error", "Chỉ được huỷ đơn trước giờ chiếu ít nhất 2 giờ"));
        assertThat(bookings.findById(id).orElseThrow().getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    void cancellationRequiresCustomerPostAndCsrf() throws Exception {
        String url = "/customer/bookings/" + data.getFirst().getBookingId() + "/cancel";
        var session = customer(data.getFirst().getCustomer().getCustomerId());
        mvc.perform(get(url).session(session)).andExpect(status().isMethodNotAllowed());
        mvc.perform(post(url).session(session)).andExpect(status().isForbidden());
        mvc.perform(post(url).with(csrf())).andExpect(redirectedUrl("/login"));
        var admin = new MockHttpSession();
        admin.setAttribute(SessionUtils.CURRENT_USER, new SessionUser(-1L, null, "Admin", Role.ADMIN));
        mvc.perform(post(url).session(admin).with(csrf())).andExpect(status().isForbidden());
        String html = mvc.perform(get("/customer/bookings/" + data.getFirst().getBookingId()).session(session))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("name=\"_csrf\"", "Bạn chắc chắn muốn huỷ đơn này?");
    }
}
