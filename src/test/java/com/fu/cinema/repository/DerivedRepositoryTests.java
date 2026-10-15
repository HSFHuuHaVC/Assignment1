package com.fu.cinema.repository;

import com.fu.cinema.config.PasswordConfig;
import com.fu.cinema.entity.*;
import com.fu.cinema.enums.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@Import(PasswordConfig.class)
class DerivedRepositoryTests extends JpaFixture {
    @Autowired AccountRepository accounts;
    @Autowired CustomerRepository customers;
    @Autowired GenreRepository genres;
    @Autowired MovieRepository movies;
    @Autowired RoomRepository rooms;
    @Autowired ShowtimeRepository showtimes;
    @Autowired BookingRepository bookings;
    @Autowired ReviewRepository reviews;
    @Autowired TicketRepository tickets;
    @Autowired PasswordEncoder encoder;

    @Test
    void accountAndCustomerDuplicateChecksExcludeCurrentRecord() {
        Customer customer = customer("Alice", 1);
        Long id = customer.getCustomerId();
        Long accountId = customer.getAccount().getAccountId();
        flushAndClear();
        assertThat(accounts.findByUsername("user1")).isPresent();
        assertThat(accounts.existsByUsername("user1")).isTrue();
        assertThat(accounts.existsByUsernameAndAccountIdNot("user1", accountId)).isFalse();
        assertThat(accounts.existsByUsernameAndAccountIdNot("user1", -1L)).isTrue();
        assertThat(customers.findByAccount_AccountId(accountId)).isPresent();
        assertThat(customers.existsByEmail(customer.getEmail())).isTrue();
        assertThat(customers.existsByMobile(customer.getMobile())).isTrue();
        assertThat(customers.existsByIdentityCard(customer.getIdentityCard())).isTrue();
        assertThat(customers.existsByEmailAndCustomerIdNot(customer.getEmail(), id)).isFalse();
        assertThat(customers.existsByMobileAndCustomerIdNot(customer.getMobile(), id)).isFalse();
        assertThat(customers.existsByIdentityCardAndCustomerIdNot(customer.getIdentityCard(), id)).isFalse();
        assertThat(customers.existsByEmailAndCustomerIdNot(customer.getEmail(), -1L)).isTrue();
        assertThat(customers.existsByMobileAndCustomerIdNot(customer.getMobile(), -1L)).isTrue();
        assertThat(customers.existsByIdentityCardAndCustomerIdNot(customer.getIdentityCard(), -1L)).isTrue();
    }

    @Test
    void genreAndRoomNamesAreCheckedIgnoringCase() {
        Genre genre = genre("Comedy");
        genre("Action");
        Room room = room("Room 02");
        room("Room 01");
        Room maintenance = room("Maintenance");
        maintenance.setStatus(RoomStatus.MAINTENANCE);
        flushAndClear();
        assertThat(genres.existsByGenreNameIgnoreCase("COMEDY")).isTrue();
        assertThat(genres.existsByGenreNameIgnoreCaseAndGenreIdNot("comedy", genre.getGenreId())).isFalse();
        assertThat(genres.existsByGenreNameIgnoreCaseAndGenreIdNot("comedy", -1L)).isTrue();
        assertThat(genres.findAllByOrderByGenreNameAsc()).extracting(Genre::getGenreName)
                .containsExactly("Action", "Comedy");
        assertThat(rooms.existsByRoomNameIgnoreCase("ROOM 02")).isTrue();
        assertThat(rooms.existsByRoomNameIgnoreCaseAndRoomIdNot("room 02", room.getRoomId())).isFalse();
        assertThat(rooms.existsByRoomNameIgnoreCaseAndRoomIdNot("room 02", -1L)).isTrue();
        assertThat(rooms.findByStatus(RoomStatus.ACTIVE)).hasSize(2);
        assertThat(rooms.findByStatusOrderByRoomNameAsc(RoomStatus.ACTIVE)).extracting(Room::getRoomName)
                .containsExactly("Room 01", "Room 02");
    }

    @Test
    void movieDropdownAndHomeQueriesFilterAndOrderCorrectly() {
        Genre genre = genre("Action");
        Movie old = movie("B Film", genre, MovieStatus.SHOWING);
        Movie recent = movie("A Film", genre, MovieStatus.COMING_SOON);
        recent.setReleaseDate(old.getReleaseDate().plusDays(1));
        movie("Stopped", genre, MovieStatus.STOPPED);
        flushAndClear();
        assertThat(movies.existsByGenre_GenreId(genre.getGenreId())).isTrue();
        assertThat(movies.countByStatus(MovieStatus.SHOWING)).isEqualTo(1);
        assertThat(movies.findByStatusNotOrderByTitleAsc(MovieStatus.STOPPED)).extracting(Movie::getTitle)
                .containsExactly("A Film", "B Film");
        assertThat(movies.findByStatusInOrderByReleaseDateDesc(List.of(MovieStatus.SHOWING, MovieStatus.COMING_SOON)))
                .extracting(Movie::getMovieId).containsExactly(recent.getMovieId(), old.getMovieId());
    }

    @Test
    void upcomingShowtimesExcludePastBoundaryAndCancelledAndFetchRoom() {
        Movie movie = movie("Film", genre("Action"), MovieStatus.SHOWING);
        Room room = room("Room 01");
        LocalDateTime now = LocalDateTime.of(2026, 10, 15, 9, 0);
        showtime(movie, room, now, ShowtimeStatus.OPEN);
        Showtime later = showtime(movie, room, now.plusHours(3), ShowtimeStatus.OPEN);
        showtime(movie, room, now.plusHours(6), ShowtimeStatus.CANCELLED);
        flushAndClear();
        assertThat(showtimes.existsByMovie_MovieId(movie.getMovieId())).isTrue();
        assertThat(showtimes.existsByRoom_RoomId(room.getRoomId())).isTrue();
        List<Showtime> found = showtimes.findByMovie_MovieIdAndStatusAndStartTimeAfterOrderByStartTimeAsc(
                movie.getMovieId(), ShowtimeStatus.OPEN, now);
        em.clear();
        assertThat(found).extracting(Showtime::getShowtimeId).containsExactly(later.getShowtimeId());
        assertThat(found.getFirst().getRoom().getRoomName()).isEqualTo("Room 01");
    }

    @Test
    void customerHistoryIsPagedScopedAndFetchedOutsidePersistenceContext() {
        Customer customer = customer("Alice", 1);
        Customer other = customer("Other", 2);
        Showtime showtime = showtime(movie("Film", genre("Action"), MovieStatus.SHOWING), room("Room 01"),
                LocalDateTime.of(2026, 10, 15, 9, 0), ShowtimeStatus.OPEN);
        booking(customer, showtime, BookingStatus.PENDING, showtime.getStartTime(), "A1");
        Booking confirmed = booking(customer, showtime, BookingStatus.CONFIRMED, showtime.getStartTime(), "A2");
        booking(other, showtime, BookingStatus.CONFIRMED, showtime.getStartTime(), "A3");
        flushAndClear();
        assertThat(bookings.existsByCustomer_CustomerId(customer.getCustomerId())).isTrue();
        assertThat(bookings.existsByShowtime_ShowtimeId(showtime.getShowtimeId())).isTrue();
        assertThat(bookings.countByStatus(BookingStatus.CONFIRMED)).isEqualTo(2);
        assertThat(bookings.findByCustomer_CustomerId(customer.getCustomerId(), PageRequest.of(0, 1))
                .getTotalElements()).isEqualTo(2);
        var page = bookings.findByCustomer_CustomerIdAndStatus(customer.getCustomerId(),
                BookingStatus.CONFIRMED, PageRequest.of(0, 10));
        em.clear();
        assertThat(page.getContent()).extracting(Booking::getBookingId).containsExactly(confirmed.getBookingId());
        assertThat(page.getContent().getFirst().getShowtime().getMovie().getTitle()).isEqualTo("Film");
        assertThat(page.getContent().getFirst().getShowtime().getRoom().getRoomName()).isEqualTo("Room 01");
        assertThat(tickets.count()).isEqualTo(3);
    }

    @Test
    void reviewsAreOrderedAndFetchCustomerForView() {
        Movie movie = movie("Film", genre("Action"), MovieStatus.SHOWING);
        LocalDateTime now = LocalDateTime.of(2026, 10, 15, 9, 0);
        review(customer("Alice", 1), movie, 3, now);
        review(customer("Bob", 2), movie, 5, now.plusDays(1));
        flushAndClear();
        var found = reviews.findByMovie_MovieIdOrderByReviewDateDesc(movie.getMovieId());
        em.clear();
        assertThat(found).extracting(Review::getRating).containsExactly(5, 3);
        assertThat(found.getFirst().getCustomer().getFullName()).isEqualTo("Bob");
    }

    @Test
    void sharedPasswordEncoderUsesSaltedBCryptHashes() {
        String first = encoder.encode("Example@123");
        assertThat(first).startsWith("$2");
        assertThat(encoder.matches("Example@123", first)).isTrue();
        assertThat(encoder.matches("Wrong@123", first)).isFalse();
        assertThat(encoder.encode("Example@123")).isNotEqualTo(first);
    }
}
