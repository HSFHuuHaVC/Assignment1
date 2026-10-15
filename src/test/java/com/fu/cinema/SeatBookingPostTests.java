package com.fu.cinema;

import com.fu.cinema.dto.SessionUser;
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
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:seat_booking_post;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class SeatBookingPostTests {
    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    @Autowired BookingRepository bookings;
    private SeatBookingFixture.Data data;
    @BeforeEach void seed() { data = SeatBookingFixture.seed(em); em.clear(); }

    private MockHttpSession customer(Long id) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionUtils.CURRENT_USER, new SessionUser(-1L, id, "Customer", Role.CUSTOMER));
        return session;
    }

    @Test
    void booksMultipleSeatsForSessionCustomerCalculatesPriceAndRendersOwnedDetail() throws Exception {
        var result = mvc.perform(post("/customer/bookings").session(customer(data.adult().getCustomerId())).with(csrf())
                        .param("showtimeId", data.showtime().getShowtimeId().toString()).param("seatCodes", "F1", "F2")
                        .param("customerId", data.teenager().getCustomerId().toString()).param("totalAmount", "1")
                        .param("price", "1").param("status", "CONFIRMED"))
                .andExpect(status().is3xxRedirection()).andReturn();
        String url = result.getResponse().getRedirectedUrl();
        Long id = Long.valueOf(url.substring(url.lastIndexOf('/') + 1));
        em.clear();
        var booking = bookings.findDetailById(id).orElseThrow();
        assertThat(booking.getCustomer().getCustomerId()).isEqualTo(data.adult().getCustomerId());
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(booking.getTotalAmount()).isEqualByComparingTo("160000");
        assertThat(booking.getTickets()).extracting(com.fu.cinema.entity.Ticket::getSeatCode).containsExactly("F1", "F2");
        String html = mvc.perform(get(url).session(customer(data.adult().getCustomerId())))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("F1", "F2", "160,000đ", "PENDING");
        mvc.perform(get(url).session(customer(data.teenager().getCustomerId()))).andExpect(status().isNotFound());
    }

    @Test
    void emptyAndNineSeatsRedirectWithValidationMessagesWithoutCreatingBookings() throws Exception {
        String showtimeId = data.showtime().getShowtimeId().toString();
        mvc.perform(post("/customer/bookings").session(customer(data.adult().getCustomerId())).with(csrf())
                        .param("showtimeId", showtimeId))
                .andExpect(redirectedUrl("/customer/showtimes/" + showtimeId + "/seats"))
                .andExpect(flash().attribute("error", "Vui lòng chọn ít nhất 1 ghế"));
        mvc.perform(post("/customer/bookings").session(customer(data.adult().getCustomerId())).with(csrf())
                        .param("showtimeId", showtimeId).param("seatCodes", "A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8", "A9"))
                .andExpect(flash().attribute("error", "Mỗi đơn tối đa 8 ghế"));
        assertThat(bookings.count()).isEqualTo(3);
    }

    @Test
    void bookingRequiresCustomerCsrfAndValidShowtimeId() throws Exception {
        String showtimeId = data.showtime().getShowtimeId().toString();
        mvc.perform(post("/customer/bookings").with(csrf()).param("showtimeId", showtimeId).param("seatCodes", "F1"))
                .andExpect(redirectedUrl("/login"));
        mvc.perform(post("/customer/bookings").session(customer(data.adult().getCustomerId())))
                .andExpect(status().isForbidden());
        MockHttpSession admin = new MockHttpSession();
        admin.setAttribute(SessionUtils.CURRENT_USER, new SessionUser(-1L, null, "Admin", Role.ADMIN));
        mvc.perform(post("/customer/bookings").session(admin).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/customer/bookings").session(customer(data.adult().getCustomerId())).with(csrf())
                        .param("seatCodes", "F1")).andExpect(status().isNotFound());
        assertThat(bookings.count()).isEqualTo(3);
    }
}
