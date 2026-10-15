package com.fu.cinema.repository;

import com.fu.cinema.entity.*;
import com.fu.cinema.enums.*;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class EntityMappingTests extends JpaFixture {
    @Test
    void persistsStringEnumsAndLazyMandatoryRelationships() {
        Customer customer = customer("Alice", 1);
        Movie movie = movie("Film", genre("Action"), MovieStatus.SHOWING);
        Room room = room("Room 01");
        Showtime showtime = showtime(movie, room, LocalDateTime.of(2026, 10, 15, 9, 0), ShowtimeStatus.OPEN);
        flushAndClear();
        assertThat(em.createNativeQuery("select role from accounts where account_id = :id", String.class)
                .setParameter("id", customer.getAccount().getAccountId()).getSingleResult()).isEqualTo("CUSTOMER");
        assertThat(em.createNativeQuery("select status from movies where movie_id = :id", String.class)
                .setParameter("id", movie.getMovieId()).getSingleResult()).isEqualTo("SHOWING");
        Customer loaded = em.find(Customer.class, customer.getCustomerId());
        assertThat(em.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(loaded, "account")).isFalse();
        Showtime loadedShowtime = em.find(Showtime.class, showtime.getShowtimeId());
        assertThat(em.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(loadedShowtime, "room")).isFalse();
        assertThat(em.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(loadedShowtime, "movie")).isFalse();
        assertThat(room.getCapacity()).isEqualTo(50);
    }

    @Test
    void databaseRejectsTwoCustomersSharingAnAccount() {
        Customer first = customer("First", 1);
        Customer second = customer("Second", 2);
        second.setAccount(first.getAccount());
        assertThatThrownBy(em::flush).isInstanceOf(PersistenceException.class);
    }

    @Test
    void databaseRejectsDuplicateCustomerEmail() {
        Customer first = customer("First", 1);
        Customer second = customer("Second", 2);
        second.setEmail(first.getEmail());
        assertThatThrownBy(em::flush).isInstanceOf(PersistenceException.class);
    }

    @Test
    void mapsCompositeReviewIdentityAndEnforcesOneReviewPerCustomerAndMovie() {
        Customer customer = customer("First", 1);
        Movie movie = movie("Film", genre("Action"), MovieStatus.SHOWING);
        Review review = review(customer, movie, 4, LocalDateTime.of(2026, 10, 15, 9, 0));
        ReviewId id = new ReviewId(customer.getCustomerId(), movie.getMovieId());
        flushAndClear();
        assertThat(review.getId()).isEqualTo(id).hasSameHashCodeAs(id);
        assertThat(em.find(Review.class, id).getRating()).isEqualTo(4);
        em.clear();
        assertThatThrownBy(() -> {
            review(customer, movie, 5, LocalDateTime.of(2026, 10, 16, 9, 0));
            em.flush();
        }).isInstanceOf(PersistenceException.class);
    }

    @Test
    void cascadesTicketsRemovesOrphansAndPreservesHistoricalCancelledSeats() {
        Customer customer = customer("First", 1);
        Movie movie = movie("Film", genre("Action"), MovieStatus.SHOWING);
        Showtime showtime = showtime(movie, room("Room 01"),
                LocalDateTime.of(2026, 10, 15, 9, 0), ShowtimeStatus.OPEN);
        Booking cancelled = booking(customer, showtime, BookingStatus.CANCELLED, showtime.getStartTime(), "A1");
        Booking active = booking(customer, showtime, BookingStatus.PENDING, showtime.getStartTime(), "A1", "A2");
        flushAndClear();
        Booking loaded = em.find(Booking.class, active.getBookingId());
        Long orphanId = loaded.getTickets().getFirst().getTicketId();
        loaded.getTickets().removeFirst();
        flushAndClear();
        assertThat(em.find(Ticket.class, orphanId)).isNull();
        em.remove(em.find(Booking.class, active.getBookingId()));
        flushAndClear();
        assertThat(em.createQuery("select count(t) from Ticket t", Long.class).getSingleResult()).isEqualTo(1L);
        assertThat(em.find(Booking.class, cancelled.getBookingId())).isNotNull();
        assertThat(em.find(Showtime.class, showtime.getShowtimeId())).isNotNull();
    }

    @Test
    void deletingGenreWithMoviesIsRejectedByForeignKey() {
        Genre genre = genre("Action");
        movie("Film", genre, MovieStatus.SHOWING);
        flushAndClear();
        em.remove(em.find(Genre.class, genre.getGenreId()));
        assertThatThrownBy(em::flush).isInstanceOf(PersistenceException.class);
    }
}
