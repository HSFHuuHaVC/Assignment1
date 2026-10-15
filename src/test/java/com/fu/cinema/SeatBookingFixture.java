package com.fu.cinema;

import com.fu.cinema.entity.*;
import com.fu.cinema.enums.*;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class SeatBookingFixture {
    private SeatBookingFixture() {}
    public record Data(Customer adult, Customer teenager, Showtime showtime,
                       Showtime restricted, Showtime closed, Showtime past) {}

    public static Data seed(EntityManager em) {
        var bookings = AdminBookingFixture.seed(em, 3);
        var show = bookings.getFirst().getShowtime();
        show.getRoom().setTotalRows(7);
        show.getRoom().setSeatsPerRow(10);
        show.setTicketPrice(new BigDecimal("80000"));
        String[][] seats = {{"E5", "E6"}, {"E7", "E8"}, {"F1", "F3"}};
        for (int i = 0; i < bookings.size(); i++) {
            var booking = bookings.get(i);
            booking.setTotalAmount(new BigDecimal("160000"));
            for (int j = 0; j < booking.getTickets().size(); j++) {
                booking.getTickets().get(j).setSeatCode(seats[i][j]);
                booking.getTickets().get(j).setPrice(show.getTicketPrice());
            }
        }
        var adult = bookings.getFirst().getCustomer();
        var teenager = bookings.get(1).getCustomer();
        teenager.setBirthday(LocalDate.now().minusYears(15));
        Movie film = new Movie();
        film.setTitle("Ngôi Nhà Bí Ẩn");
        film.setDirector("Director");
        film.setDuration(120);
        film.setReleaseDate(LocalDate.now().minusDays(1));
        film.setAgeRating(AgeRating.T18);
        film.setDescription("Description");
        film.setPosterUrl("/poster.jpg");
        film.setGenre(show.getMovie().getGenre());
        film.setStatus(MovieStatus.SHOWING);
        em.persist(film);
        var restricted = showtime(em, film, show.getRoom(), LocalDateTime.now().plusDays(4), ShowtimeStatus.OPEN);
        var closed = showtime(em, show.getMovie(), show.getRoom(), LocalDateTime.now().plusDays(5), ShowtimeStatus.CANCELLED);
        var past = showtime(em, show.getMovie(), show.getRoom(), LocalDateTime.now().minusMinutes(1), ShowtimeStatus.OPEN);
        em.flush();
        return new Data(adult, teenager, show, restricted, closed, past);
    }

    private static Showtime showtime(EntityManager em, Movie film, Room room, LocalDateTime start, ShowtimeStatus status) {
        Showtime show = new Showtime();
        show.setMovie(film);
        show.setRoom(room);
        show.setStartTime(start);
        show.setEndTime(start.plusHours(2));
        show.setTicketPrice(new BigDecimal("80000"));
        show.setStatus(status);
        em.persist(show);
        return show;
    }
}
