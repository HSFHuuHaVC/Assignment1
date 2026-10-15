package com.fu.cinema.repository;

import com.fu.cinema.entity.*;
import com.fu.cinema.enums.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class SearchQueryTests extends JpaFixture {
    @Autowired CustomerRepository customers;
    @Autowired MovieRepository movies;
    @Autowired ShowtimeRepository showtimes;
    @Autowired BookingRepository bookings;

    @Test
    void q1SearchesNameEmailMobileAndCountsAllPages() {
        Customer alice = customer("Alice", 1);
        customer("ALICIA", 2);
        customer("Bob", 3);
        flushAndClear();
        var firstPage = customers.search("ali", PageRequest.of(0, 1, Sort.by("fullName")));
        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(customers.search("USER1@", PageRequest.of(0, 10)).getContent())
                .extracting(Customer::getCustomerId).containsExactly(alice.getCustomerId());
        var mobile = customers.search(alice.getMobile(), PageRequest.of(0, 10)).getContent();
        em.clear();
        assertThat(mobile.getFirst().getAccount().getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(customers.search(null, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(3);
        assertThat(customers.search("missing", PageRequest.of(0, 10))).isEmpty();
        Customer fetched = customers.findWithAccountById(alice.getCustomerId()).orElseThrow();
        em.clear();
        assertThat(fetched.getAccount().getUsername()).isEqualTo("user1");
    }

    @Test
    void q2CombinesTitleGenreStatusesAndPagination() {
        Genre action = genre("Action");
        Genre comedy = genre("Comedy");
        Movie first = movie("Alpha one", action, MovieStatus.SHOWING);
        movie("Alpha two", action, MovieStatus.COMING_SOON);
        movie("Alpha three", comedy, MovieStatus.SHOWING);
        movie("Stopped", action, MovieStatus.STOPPED);
        flushAndClear();
        var page = movies.search("ALPHA", action.getGenreId(),
                List.of(MovieStatus.SHOWING, MovieStatus.COMING_SOON), PageRequest.of(0, 1));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(movies.search(null, null, List.of(MovieStatus.SHOWING), PageRequest.of(0, 10)))
                .hasSize(2);
        assertThat(movies.search("missing", null, List.of(MovieStatus.SHOWING), PageRequest.of(0, 10)))
                .isEmpty();
        assertThat(movies.search(null, null, List.of(), PageRequest.of(0, 10))).isEmpty();
        Movie fetched = movies.findWithGenreById(first.getMovieId()).orElseThrow();
        em.clear();
        assertThat(fetched.getGenre().getGenreName()).isEqualTo("Action");
    }

    @Test
    void showtimeSearchUsesExclusiveEndAndFetchesMovieAndRoom() {
        Movie movie = movie("Film", genre("Action"), MovieStatus.SHOWING);
        Room room = room("Room 01");
        LocalDateTime from = LocalDateTime.of(2026, 10, 15, 0, 0);
        Showtime first = showtime(movie, room, from, ShowtimeStatus.OPEN);
        showtime(movie, room, from.plusDays(1), ShowtimeStatus.OPEN);
        flushAndClear();
        var page = showtimes.search(movie.getMovieId(), room.getRoomId(), from, from.plusDays(1),
                PageRequest.of(0, 1));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting(Showtime::getShowtimeId).containsExactly(first.getShowtimeId());
        assertThat(showtimes.search(null, null, null, null, PageRequest.of(0, 1)).getTotalElements()).isEqualTo(2);
        Showtime detail = showtimes.findDetailById(first.getShowtimeId()).orElseThrow();
        em.clear();
        assertThat(detail.getMovie().getTitle()).isEqualTo("Film");
        assertThat(detail.getRoom().getRoomName()).isEqualTo("Room 01");
    }

    @Test
    void q5FetchesWholeBookingIncludingEmptyTicketCollection() {
        Customer customer = customer("Alice", 1);
        Showtime showtime = showtime(movie("Film", genre("Action"), MovieStatus.SHOWING), room("Room 01"),
                LocalDateTime.of(2026, 10, 15, 9, 0), ShowtimeStatus.OPEN);
        Booking booking = booking(customer, showtime, BookingStatus.PENDING, showtime.getStartTime(), "B2", "A1");
        Booking empty = booking(customer, showtime, BookingStatus.CANCELLED, showtime.getStartTime());
        flushAndClear();
        Booking detail = bookings.findDetailById(booking.getBookingId()).orElseThrow();
        Booking emptyDetail = bookings.findDetailById(empty.getBookingId()).orElseThrow();
        em.clear();
        assertThat(detail.getCustomer().getFullName()).isEqualTo("Alice");
        assertThat(detail.getShowtime().getMovie().getTitle()).isEqualTo("Film");
        assertThat(detail.getShowtime().getRoom().getRoomName()).isEqualTo("Room 01");
        assertThat(detail.getTickets()).extracting(Ticket::getSeatCode).containsExactly("A1", "B2");
        assertThat(emptyDetail.getTickets()).isEmpty();
        assertThat(bookings.findDetailById(-1L)).isEmpty();
    }

    @Test
    void q6FiltersStatusCustomerAndBookingDateWithAccurateCount() {
        Customer alice = customer("Alice", 1);
        Customer bob = customer("Bob", 2);
        LocalDateTime from = LocalDateTime.of(2026, 10, 15, 0, 0);
        Showtime showtime = showtime(movie("Film", genre("Action"), MovieStatus.SHOWING), room("Room 01"),
                from.plusDays(5), ShowtimeStatus.OPEN);
        Booking first = booking(alice, showtime, BookingStatus.CONFIRMED, from, "A1");
        booking(alice, showtime, BookingStatus.CONFIRMED, from.plusHours(1), "A2");
        booking(alice, showtime, BookingStatus.CONFIRMED, from.plusDays(1), "A3");
        booking(alice, showtime, BookingStatus.PENDING, from, "A4");
        booking(bob, showtime, BookingStatus.CONFIRMED, from, "A5");
        flushAndClear();
        var page = bookings.search(BookingStatus.CONFIRMED, from, from.plusDays(1), "ALICE", PageRequest.of(0, 1));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(bookings.search(null, null, null, null, PageRequest.of(0, 1)).getTotalElements()).isEqualTo(5);
        assertThat(bookings.search(null, null, null, alice.getMobile(), PageRequest.of(0, 10))).hasSize(4);
        assertThat(bookings.search(null, null, null, "USER1@", PageRequest.of(0, 10))).hasSize(4);
        assertThat(bookings.search(null, null, null, "missing", PageRequest.of(0, 10))).isEmpty();
        assertThat(first.getBookingDate()).isEqualTo(from);
    }
}
