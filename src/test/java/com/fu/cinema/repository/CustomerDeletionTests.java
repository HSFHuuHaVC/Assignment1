package com.fu.cinema.repository;

import com.fu.cinema.config.PasswordConfig;
import com.fu.cinema.enums.*;
import com.fu.cinema.service.CustomerService;
import com.fu.cinema.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;

@Import({CustomerServiceImpl.class, PasswordConfig.class, CustomerSearchServiceTests.ValidationConfig.class})
class CustomerDeletionTests extends JpaFixture {
    @Autowired CustomerService service;
    @Autowired AccountRepository accounts;
    @Autowired CustomerRepository customers;
    @Autowired BookingRepository bookings;
    @Autowired TicketRepository tickets;

    @ParameterizedTest
    @EnumSource(BookingStatus.class)
    void anyBookingHistoryLocksAccountAndPreservesBookingAndTickets(BookingStatus status) {
        var customer = customer("Nguyen Van An", 1);
        var showtime = showtime(movie("Film", genre("Action"), MovieStatus.SHOWING), room("Room 01"),
                LocalDateTime.now().plusDays(1), ShowtimeStatus.OPEN);
        var booking = booking(customer, showtime, status, LocalDateTime.now(), "A1", "A2");
        Long accountId = customer.getAccount().getAccountId();
        flushAndClear();
        assertThat(service.delete(customer.getCustomerId())).isFalse();
        flushAndClear();
        assertThat(customers.existsById(customer.getCustomerId())).isTrue();
        assertThat(accounts.findById(accountId).orElseThrow().getStatus()).isEqualTo(AccountStatus.LOCKED);
        assertThat(bookings.findById(booking.getBookingId()).orElseThrow().getStatus()).isEqualTo(status);
        assertThat(tickets.count()).isEqualTo(2);
        assertThat(service.delete(customer.getCustomerId())).isFalse();
    }

    @Test
    void customerAndAccountArePhysicallyDeletedInForeignKeySafeOrder() {
        var customer = customer("No bookings", 1);
        Long accountId = customer.getAccount().getAccountId();
        flushAndClear();
        assertThat(service.delete(customer.getCustomerId())).isTrue();
        flushAndClear();
        assertThat(customers.existsById(customer.getCustomerId())).isFalse();
        assertThat(accounts.existsById(accountId)).isFalse();
    }
}
