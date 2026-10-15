package com.fu.cinema;

import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.entity.Booking;
import com.fu.cinema.enums.*;
import com.fu.cinema.exception.ResourceNotFoundException;
import com.fu.cinema.repository.BookingRepository;
import com.fu.cinema.service.BookingService;
import com.fu.cinema.util.SessionUtils;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:booking_ownership;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class CustomerBookingOwnershipTests {
    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    @Autowired BookingService service;
    @Autowired BookingRepository bookings;
    private List<Booking> data;
    private Long currentCustomerId;
    @BeforeEach void seed() {
        data = AdminBookingFixture.seed(em, 3);
        currentCustomerId = data.getFirst().getCustomer().getCustomerId();
        em.clear();
    }

    private MockHttpSession session(Long customerId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionUtils.CURRENT_USER, new SessionUser(-1L, customerId, "An", Role.CUSTOMER));
        return session;
    }

    @Test
    void detailAndCancelForOtherCustomerAreIndistinguishableFromMissingBooking() throws Exception {
        Long otherId = data.get(1).getBookingId();
        for (Long id : new Long[]{otherId, -1L}) {
            String html = mvc.perform(get("/customer/bookings/" + id).session(session(currentCustomerId))
                            .queryParam("customerId", data.get(1).getCustomer().getCustomerId().toString()))
                    .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
            assertThat(html).doesNotContain("Tran Thi Binh", "fixture2@example.test", ">A2<", ">B2<");
            mvc.perform(post("/customer/bookings/" + id + "/cancel").session(session(currentCustomerId)).with(csrf())
                            .param("customerId", data.get(1).getCustomer().getCustomerId().toString()))
                    .andExpect(status().isNotFound());
        }
        assertThat(bookings.findById(otherId).orElseThrow().getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void serviceAndRepositoryQueriesEnforceOwnershipAndKeepOwnedDetailDetached() {
        Long ownId = data.getFirst().getBookingId();
        Long otherId = data.get(1).getBookingId();
        assertThat(bookings.findDetailByIdAndCustomerId(otherId, currentCustomerId)).isEmpty();
        assertThat(bookings.findByIdAndCustomerIdForUpdate(otherId, currentCustomerId)).isEmpty();
        assertThatThrownBy(() -> service.getDetailForCustomer(otherId, currentCustomerId))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.cancelByCustomer(otherId, currentCustomerId))
                .isInstanceOf(ResourceNotFoundException.class);
        var detail = service.getDetailForCustomer(ownId, currentCustomerId);
        em.clear();
        assertThat(detail.getTickets()).hasSize(2);
        assertThat(detail.getShowtime().getRoom().getRoomName()).isEqualTo("Room 01");
        assertThat(detail.getShowtime().getMovie().getTitle()).isEqualTo("Test film");
    }

    @Test
    void malformedCustomerSessionCannotListReadOrCancelBookings() throws Exception {
        var malformed = session(null);
        mvc.perform(get("/customer/bookings").session(malformed)).andExpect(status().isForbidden());
        mvc.perform(get("/customer/bookings/" + data.getFirst().getBookingId()).session(malformed))
                .andExpect(status().isForbidden());
        mvc.perform(post("/customer/bookings/" + data.getFirst().getBookingId() + "/cancel")
                        .session(malformed).with(csrf())).andExpect(status().isForbidden());
        assertThatThrownBy(() -> service.findByCustomer(null, null, 0, 10)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.getDetailForCustomer(data.getFirst().getBookingId(), null))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
