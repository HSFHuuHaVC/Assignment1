package com.fu.cinema;

import com.fu.cinema.entity.Booking;
import com.fu.cinema.enums.BookingStatus;
import jakarta.persistence.EntityManager;
import java.util.List;

public final class CustomerHistoryFixture {
    private CustomerHistoryFixture() {}
    public static List<Booking> seed(EntityManager em) {
        var data = AdminBookingFixture.seed(em, 24);
        data.forEach(booking -> booking.setStatus(BookingStatus.CONFIRMED));
        data.getFirst().setStatus(BookingStatus.PENDING);
        em.flush();
        return data;
    }
}
