package com.fu.cinema;

import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.entity.Booking;
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
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:admin_booking_pages;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class AdminBookingPagesTests {
    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    private List<Booking> data;

    @BeforeEach
    void seed() {
        data = AdminBookingFixture.seed(em, 12);
        em.clear();
    }

    private MockHttpSession session(Role role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionUtils.CURRENT_USER,
                new SessionUser(-1L, role == Role.CUSTOMER ? 1L : null, "Tester", role));
        return session;
    }

    @Test
    void paginationRetainsAllFiltersAndRendersNewestFirst() throws Exception {
        String html = mvc.perform(get("/admin/bookings").session(session(Role.ADMIN))
                        .queryParam("from", "2026-10-15").queryParam("to", "2026-10-15")
                        .queryParam("kw", "EXAMPLE.TEST").queryParam("page", "0"))
                .andExpect(status().isOk()).andExpect(view().name("admin/bookings/list"))
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Test film", "140,000đ", "from=2026-10-15", "to=2026-10-15",
                "kw=EXAMPLE.TEST", "page=1");
        assertThat(html.indexOf("/admin/bookings/" + data.get(11).getBookingId() + "\""))
                .isLessThan(html.indexOf("/admin/bookings/" + data.get(10).getBookingId() + "\""));
        String next = mvc.perform(get("/admin/bookings").session(session(Role.ADMIN))
                        .queryParam("kw", "EXAMPLE.TEST").queryParam("page", "1"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(next).contains("/admin/bookings/" + data.getFirst().getBookingId() + "\"");
    }

    @Test
    void combinedStatusAndKeywordFiltersAndEmptyResultRenderCorrectly() throws Exception {
        var result = mvc.perform(get("/admin/bookings").session(session(Role.ADMIN))
                        .queryParam("status", "PENDING").queryParam("kw", "nguyen"))
                .andExpect(status().isOk()).andReturn();
        org.springframework.data.domain.Page<?> page = (org.springframework.data.domain.Page<?>)
                result.getModelAndView().getModel().get("bookings");
        assertThat(page.getTotalElements()).isEqualTo(2);
        String empty = mvc.perform(get("/admin/bookings").session(session(Role.ADMIN)).queryParam("kw", "<script>"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(empty).contains("Không có dữ liệu", "&lt;script&gt;").doesNotContain("<script>");
    }

    @Test
    void reversedDateRangeShowsErrorOnSameFormWithoutRedirectLoop() throws Exception {
        String html = mvc.perform(get("/admin/bookings").session(session(Role.ADMIN))
                        .queryParam("from", "2026-10-16").queryParam("to", "2026-10-15"))
                .andExpect(status().isOk()).andExpect(view().name("admin/bookings/list"))
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Từ ngày phải trước hoặc bằng đến ngày", "2026-10-16", "2026-10-15");
    }

    @Test
    void detailRendersAllAssociationsAndMissingIdIs404() throws Exception {
        Long id = data.getFirst().getBookingId();
        String html = mvc.perform(get("/admin/bookings/" + id).session(session(Role.ADMIN)))
                .andExpect(status().isOk()).andExpect(view().name("admin/bookings/detail"))
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Nguyen Van An", "fixture1@example.test", "0900000001", "Test film",
                "Room 01", "A1", "B1", "140,000đ");
        mvc.perform(get("/admin/bookings/-1").session(session(Role.ADMIN))).andExpect(status().isNotFound());
    }

    @Test
    void guestAndCustomerCannotViewAdminListOrDetail() throws Exception {
        for (String url : new String[]{"/admin/bookings", "/admin/bookings/" + data.getFirst().getBookingId()}) {
            mvc.perform(get(url)).andExpect(redirectedUrl("/login"));
            mvc.perform(get(url).session(session(Role.CUSTOMER))).andExpect(status().isForbidden());
        }
    }
}
