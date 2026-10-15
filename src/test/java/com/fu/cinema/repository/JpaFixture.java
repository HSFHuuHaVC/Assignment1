package com.fu.cinema.repository;

import com.fu.cinema.entity.*;
import com.fu.cinema.enums.*;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
abstract class JpaFixture {
    @Autowired protected EntityManager em;

    protected Customer customer(String name, int number) {
        Account account = new Account("user" + number, "hash", Role.CUSTOMER, AccountStatus.ACTIVE);
        em.persist(account);
        Customer customer = new Customer();
        customer.setFullName(name);
        customer.setEmail("user" + number + "@example.test");
        customer.setMobile(String.format("09%08d", number));
        customer.setIdentityCard(String.format("%012d", number));
        customer.setBirthday(LocalDate.of(2000, 1, 1));
        customer.setAccount(account);
        em.persist(customer);
        return customer;
    }

    protected Genre genre(String name) {
        Genre genre = new Genre(name, "Description");
        em.persist(genre);
        return genre;
    }

    protected Movie movie(String title, Genre genre, MovieStatus status) {
        Movie movie = new Movie();
        movie.setTitle(title);
        movie.setDirector("Director");
        movie.setDuration(120);
        movie.setReleaseDate(LocalDate.of(2026, 1, 1));
        movie.setAgeRating(AgeRating.P);
        movie.setDescription("Description");
        movie.setPosterUrl("/poster.jpg");
        movie.setGenre(genre);
        movie.setStatus(status);
        em.persist(movie);
        return movie;
    }

    protected Room room(String name) {
        Room room = new Room(name, RoomType.TWO_D, 5, 10, RoomStatus.ACTIVE);
        em.persist(room);
        return room;
    }

    protected Showtime showtime(Movie movie, Room room, LocalDateTime start, ShowtimeStatus status) {
        Showtime showtime = new Showtime();
        showtime.setMovie(movie);
        showtime.setRoom(room);
        showtime.setStartTime(start);
        showtime.setEndTime(start.plusHours(2));
        showtime.setTicketPrice(new BigDecimal("70000"));
        showtime.setStatus(status);
        em.persist(showtime);
        return showtime;
    }

    protected Booking booking(Customer customer, Showtime showtime, BookingStatus status,
                              LocalDateTime date, String... seats) {
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setShowtime(showtime);
        booking.setStatus(status);
        booking.setBookingDate(date);
        booking.setTotalAmount(showtime.getTicketPrice().multiply(BigDecimal.valueOf(seats.length)));
        for (String seat : seats) {
            booking.addTicket(new Ticket(showtime, seat, showtime.getTicketPrice()));
        }
        em.persist(booking);
        return booking;
    }

    protected Review review(Customer customer, Movie movie, int rating, LocalDateTime date) {
        Review review = new Review();
        review.setId(new ReviewId());
        review.setCustomer(customer);
        review.setMovie(movie);
        review.setRating(rating);
        review.setComment("Review");
        review.setReviewDate(date);
        em.persist(review);
        return review;
    }

    protected void flushAndClear() {
        em.flush();
        em.clear();
    }
}
