package com.fu.cinema;

import com.fu.cinema.entity.*;
import com.fu.cinema.enums.*;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Isolated fixture shared by HTTP, SQL Server and browser checks. */
public final class AdminBookingFixture {
    private AdminBookingFixture() {}

    public static List<Booking> seed(EntityManager em, int count) {
        Customer an = customer(em, "Nguyen Van An", 1);
        Customer binh = customer(em, "Tran Thi Binh", 2);
        Genre genre = new Genre("Action", "Description");
        em.persist(genre);
        Movie movie = new Movie();
        movie.setTitle("Test film");
        movie.setDirector("Director");
        movie.setDuration(120);
        movie.setReleaseDate(LocalDate.of(2026, 1, 1));
        movie.setAgeRating(AgeRating.P);
        movie.setDescription("Description");
        movie.setPosterUrl("/poster.jpg");
        movie.setGenre(genre);
        movie.setStatus(MovieStatus.SHOWING);
        em.persist(movie);
        Room room = new Room("Room 01", RoomType.TWO_D, 5, 30, RoomStatus.ACTIVE);
        em.persist(room);
        Showtime showtime = new Showtime();
        showtime.setMovie(movie);
        showtime.setRoom(room);
        showtime.setStartTime(LocalDateTime.now().plusDays(3));
        showtime.setEndTime(showtime.getStartTime().plusHours(2));
        showtime.setTicketPrice(new BigDecimal("70000"));
        showtime.setStatus(ShowtimeStatus.OPEN);
        em.persist(showtime);
        List<Booking> bookings = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Booking booking = new Booking();
            booking.setCustomer(i % 2 == 0 ? an : binh);
            booking.setShowtime(showtime);
            booking.setBookingDate(LocalDateTime.of(2026, 10, 15, 12, 0).plusMinutes(i));
            booking.setTotalAmount(new BigDecimal("140000"));
            booking.setStatus(BookingStatus.values()[i % BookingStatus.values().length]);
            booking.addTicket(new Ticket(showtime, "A" + (i + 1), showtime.getTicketPrice()));
            booking.addTicket(new Ticket(showtime, "B" + (i + 1), showtime.getTicketPrice()));
            em.persist(booking);
            bookings.add(booking);
        }
        em.flush();
        return bookings;
    }

    private static Customer customer(EntityManager em, String name, int number) {
        Account account = new Account("fixture" + number + "@example.test", "test-only-hash",
                Role.CUSTOMER, AccountStatus.ACTIVE);
        em.persist(account);
        Customer customer = new Customer();
        customer.setAccount(account);
        customer.setFullName(name);
        customer.setEmail(account.getUsername());
        customer.setMobile(String.format("09%08d", number));
        customer.setBirthday(LocalDate.of(2000, 1, 1));
        customer.setIdentityCard(String.format("%012d", number));
        em.persist(customer);
        return customer;
    }
}
