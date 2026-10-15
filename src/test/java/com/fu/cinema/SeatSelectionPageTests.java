package com.fu.cinema;

import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.enums.Role;
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
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:seat_selection;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class SeatSelectionPageTests {
    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    private SeatBookingFixture.Data data;
    @BeforeEach void seed() { data = SeatBookingFixture.seed(em); em.clear(); }

    private MockHttpSession session(Role role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionUtils.CURRENT_USER, new SessionUser(data.adult().getAccount().getAccountId(),
                role == Role.CUSTOMER ? data.adult().getCustomerId() : null, "An", role));
        return session;
    }

    @Test
    void rendersSoldSeatsDisabledAndCancelledSeatFreeWithCsrf() throws Exception {
        String html = mvc.perform(get("/customer/showtimes/" + data.showtime().getShowtimeId() + "/seats")
                        .session(session(Role.CUSTOMER))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("E5", "E6", "F1", "G10", "80,000đ/vé", "name=\"_csrf\"", "seat-map-scroll");
        assertThat(html).containsPattern("id=\"E5\"[^>]*disabled").containsPattern("id=\"E6\"[^>]*disabled");
        assertThat(html).doesNotContainPattern("id=\"F1\"[^>]*disabled");
    }

    @Test
    void onlyCustomerCanSelectSeatsAndMissingShowtimeIs404() throws Exception {
        String url = "/customer/showtimes/" + data.showtime().getShowtimeId() + "/seats";
        mvc.perform(get(url)).andExpect(redirectedUrl("/login"));
        mvc.perform(get(url).session(session(Role.ADMIN))).andExpect(status().isForbidden());
        mvc.perform(get("/customer/showtimes/-1/seats").session(session(Role.CUSTOMER))).andExpect(status().isNotFound());
    }

    @Test
    void closedAndPastShowtimesShowErrorWithoutBookingForm() throws Exception {
        for (var show : new com.fu.cinema.entity.Showtime[]{data.closed(), data.past()}) {
            String html = mvc.perform(get("/customer/showtimes/" + show.getShowtimeId() + "/seats")
                            .session(session(Role.CUSTOMER))).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            assertThat(html).contains("Suất chiếu không còn mở bán").doesNotContain("name=\"seatCodes\"");
        }
    }
}
