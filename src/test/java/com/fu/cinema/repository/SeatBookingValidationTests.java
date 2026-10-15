package com.fu.cinema.repository;

import com.fu.cinema.SeatBookingFixture;
import com.fu.cinema.dto.BookingRequest;
import com.fu.cinema.exception.*;
import com.fu.cinema.service.BookingService;
import com.fu.cinema.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

@Import(BookingServiceImpl.class)
class SeatBookingValidationTests extends JpaFixture {
    @Autowired BookingService service;
    @Autowired BookingRepository bookings;
    private SeatBookingFixture.Data data;
    @BeforeEach void seed() { data = SeatBookingFixture.seed(em); em.clear(); }

    private BookingRequest request(Long showtimeId, String... seats) {
        BookingRequest request = new BookingRequest();
        request.setShowtimeId(showtimeId);
        request.setSeatCodes(new ArrayList<>(List.of(seats)));
        return request;
    }

    @ParameterizedTest
    @ValueSource(strings = {"E5", "E6", "E7", "E8"})
    void rejectsPendingAndConfirmedSeatsInsideTransaction(String seat) {
        assertThatThrownBy(() -> service.create(data.adult().getCustomerId(), request(data.showtime().getShowtimeId(), seat)))
                .isInstanceOf(BusinessException.class).hasMessage("Ghế đã có người đặt: " + seat);
        assertThat(bookings.count()).isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Z99", "A0", "A11", "H1", "", "A01", "A-1"})
    void rejectsInvalidFormatAndSeatsOutsideRoomBounds(String seat) {
        assertThatThrownBy(() -> service.create(data.adult().getCustomerId(), request(data.showtime().getShowtimeId(), seat)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("không tồn tại trong phòng");
        assertThat(bookings.count()).isEqualTo(3);
    }

    @Test
    void rejectsCancelledAndAlreadyStartedShowtimes() {
        for (var show : new com.fu.cinema.entity.Showtime[]{data.closed(), data.past()}) {
            assertThatThrownBy(() -> service.create(data.adult().getCustomerId(), request(show.getShowtimeId(), "F1")))
                    .isInstanceOf(BusinessException.class).hasMessage("Suất chiếu không còn mở bán");
        }
    }

    @Test
    void enforcesAgeOnShowDateAndAllowsExactBirthdayBoundary() {
        Long showtimeId = data.restricted().getShowtimeId();
        Long teenagerId = data.teenager().getCustomerId();
        assertThatThrownBy(() -> service.create(teenagerId, request(showtimeId, "A1")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("từ 18 tuổi trở lên");
        var teenager = em.find(com.fu.cinema.entity.Customer.class, teenagerId);
        teenager.setBirthday(data.restricted().getStartTime().toLocalDate().minusYears(18));
        em.flush();
        em.clear();
        assertThat(service.create(teenagerId, request(showtimeId, "A1")).getBookingId()).isNotNull();
    }

    @Test
    void acceptsExactlyEightDistinctSeatsAndRejectsNineOrEmptySelections() {
        var empty = request(data.showtime().getShowtimeId());
        assertThatThrownBy(() -> service.create(data.adult().getCustomerId(), empty)).hasMessageContaining("ít nhất 1 ghế");
        empty.setSeatCodes(null);
        assertThatThrownBy(() -> service.create(data.adult().getCustomerId(), empty)).hasMessageContaining("ít nhất 1 ghế");
        assertThatThrownBy(() -> service.create(data.adult().getCustomerId(),
                request(data.showtime().getShowtimeId(), "A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8", "A9")))
                .hasMessage("Mỗi đơn tối đa 8 ghế");
        var booking = service.create(data.adult().getCustomerId(),
                request(data.showtime().getShowtimeId(), "A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8"));
        assertThat(booking.getTickets()).hasSize(8);
        assertThat(booking.getTotalAmount()).isEqualByComparingTo("640000");
    }

    @Test
    void missingSessionOrShowtimeIsRejectedWithoutNullPointerErrors() {
        assertThatThrownBy(() -> service.create(null, request(data.showtime().getShowtimeId(), "F1")))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.create(data.adult().getCustomerId(), null)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.create(data.adult().getCustomerId(), request(-1L, "F1")))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
