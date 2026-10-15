package com.fu.cinema;

import com.fu.cinema.dto.BookingRequest;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.repository.BookingRepository;
import com.fu.cinema.repository.TicketRepository;
import com.fu.cinema.service.BookingService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.List;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.datasource.url=jdbc:h2:mem:seat_booking_concurrency;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000")
@ActiveProfiles("test")
class SeatBookingConcurrencyTests {
    @Autowired BookingService service;
    @Autowired BookingRepository bookings;
    @Autowired TicketRepository tickets;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager tm;
    private SeatBookingFixture.Data data;
    @BeforeEach void seed() {
        data = new TransactionTemplate(tm).execute(status -> SeatBookingFixture.seed(em));
    }

    @Test
    void simultaneousRequestsForOneSeatCreateExactlyOneBookingAndOneTicket() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            Callable<String> attempt = () -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("Start latch timed out");
                BookingRequest request = new BookingRequest();
                request.setShowtimeId(data.showtime().getShowtimeId());
                request.setSeatCodes(List.of("G1"));
                try {
                    service.create(data.adult().getCustomerId(), request);
                    return "created";
                } catch (BusinessException ex) {
                    assertThat(ex).hasMessage("Ghế đã có người đặt: G1");
                    return "taken";
                }
            };
            Future<String> first = pool.submit(attempt);
            Future<String> second = pool.submit(attempt);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("created", "taken");
            assertThat(bookings.count()).isEqualTo(4);
            assertThat(tickets.count()).isEqualTo(7);
            assertThat(tickets.findSoldSeatCodes(data.showtime().getShowtimeId())).contains("G1");
        } finally {
            start.countDown();
            pool.shutdownNow();
            assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }
}
