package com.fu.cinema;

import com.fu.cinema.dto.BookingRequest;
import com.fu.cinema.entity.Booking;
import com.fu.cinema.enums.BookingStatus;
import com.fu.cinema.repository.*;
import com.fu.cinema.service.BookingService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.datasource.url=jdbc:h2:mem:seat_booking_atomic;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class SeatBookingTransactionTests {
    @Autowired BookingService service;
    @MockitoSpyBean BookingRepository bookings;
    @Autowired TicketRepository tickets;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager tm;
    private SeatBookingFixture.Data data;

    @BeforeEach
    void seed() {
        data = new TransactionTemplate(tm).execute(status -> {
            for (String entity : List.of("Ticket", "Booking", "Showtime", "Movie", "Genre", "Room", "Customer", "Account")) {
                em.createQuery("delete from " + entity).executeUpdate();
            }
            return SeatBookingFixture.seed(em);
        });
    }

    private BookingRequest request(String... seats) {
        BookingRequest request = new BookingRequest();
        request.setShowtimeId(data.showtime().getShowtimeId());
        request.setSeatCodes(new ArrayList<>(List.of(seats)));
        return request;
    }

    @Test
    void transactionPersistsNormalizedDistinctSeatsAndCalculatesAllPricesOnServer() {
        var booking = service.create(data.adult().getCustomerId(), request(" f2 ", "F1", "f1"));
        var detail = service.getDetail(booking.getBookingId());
        assertThat(detail.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(detail.getTotalAmount()).isEqualByComparingTo("160000");
        assertThat(detail.getTickets()).extracting(com.fu.cinema.entity.Ticket::getSeatCode).containsExactly("F1", "F2");
        assertThat(detail.getTickets()).allSatisfy(ticket -> assertThat(ticket.getPrice()).isEqualByComparingTo("80000"));
        assertThat(bookings.count()).isEqualTo(4);
        assertThat(tickets.count()).isEqualTo(8);
    }

    @Test
    void failureAfterBookingAndTicketInsertsRollsBackTheEntireTransaction() {
        AtomicBoolean inserted = new AtomicBoolean();
        doAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            em.persist(booking);
            em.flush();
            inserted.set(em.createQuery("select count(t) from Ticket t", Long.class).getSingleResult() == 8);
            throw new DataIntegrityViolationException("simulated failure after cascading ticket inserts");
        }).when(bookings).saveAndFlush(any());
        try {
            assertThatThrownBy(() -> service.create(data.adult().getCustomerId(), request("F1", "F2")))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThat(inserted).isTrue();
            assertThat(bookings.count()).isEqualTo(3);
            assertThat(tickets.count()).isEqualTo(6);
            assertThat(tickets.findSoldSeatCodes(data.showtime().getShowtimeId())).doesNotContain("F1", "F2");
        } finally {
            reset(bookings);
        }
    }
}
