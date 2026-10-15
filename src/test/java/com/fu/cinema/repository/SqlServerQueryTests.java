package com.fu.cinema.repository;

import com.fu.cinema.entity.*;
import com.fu.cinema.enums.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * Opt-in test: uses create-drop only in the dedicated FUCinemaIssue3Test database.
 * See docs/testing-issue-3.md for connection environment variables.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
@EnabledIfEnvironmentVariable(named = "CINEMA_SQLSERVER_TEST_URL", matches = ".+")
class SqlServerQueryTests {
    @Autowired EntityManager em;
    @Autowired ShowtimeRepository showtimes;
    @Autowired com.fu.cinema.service.BookingService bookingService;

    @DynamicPropertySource
    static void sqlServerProperties(DynamicPropertyRegistry properties) {
        String url = System.getenv("CINEMA_SQLSERVER_TEST_URL");
        boolean dedicatedDatabase = url != null && url.startsWith("jdbc:sqlserver:")
                && Arrays.stream(url.split(";"))
                .anyMatch(part -> part.trim().equalsIgnoreCase("databaseName=FUCinemaIssue3Test"));
        if (!dedicatedDatabase) {
            throw new IllegalArgumentException("SQL Server test requires databaseName=FUCinemaIssue3Test");
        }
        properties.add("spring.datasource.url", () -> url);
        properties.add("spring.datasource.driver-class-name", () -> "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        properties.add("spring.datasource.username", () -> System.getenv("CINEMA_SQLSERVER_TEST_USERNAME"));
        properties.add("spring.datasource.password", () -> System.getenv("CINEMA_SQLSERVER_TEST_PASSWORD"));
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Test
    void q11UsesServerDateCountsOnlyOpenTodayAndIncludesRoomsWithoutShowtimes() {
        LocalDate today = (LocalDate) em.createNativeQuery("SELECT CAST(GETDATE() AS DATE)", LocalDate.class)
                .getSingleResult();
        Genre genre = new Genre("Test genre", "Description");
        em.persist(genre);
        Movie movie = new Movie();
        movie.setTitle("Test film");
        movie.setDirector("Director");
        movie.setDuration(120);
        movie.setReleaseDate(today);
        movie.setAgeRating(AgeRating.P);
        movie.setDescription("Description");
        movie.setPosterUrl("/poster.jpg");
        movie.setGenre(genre);
        movie.setStatus(MovieStatus.SHOWING);
        em.persist(movie);
        Room first = new Room("Room 01", RoomType.TWO_D, 5, 10, RoomStatus.ACTIVE);
        Room second = new Room("Room 02", RoomType.TWO_D, 5, 10, RoomStatus.ACTIVE);
        Room empty = new Room("Room 03", RoomType.IMAX, 5, 10, RoomStatus.ACTIVE);
        em.persist(first);
        em.persist(second);
        em.persist(empty);
        addShowtime(movie, first, today.atStartOfDay(), ShowtimeStatus.OPEN);
        addShowtime(movie, first, today.atTime(23, 59), ShowtimeStatus.OPEN);
        addShowtime(movie, first, today.atTime(12, 0), ShowtimeStatus.CANCELLED);
        addShowtime(movie, first, today.minusDays(1).atTime(12, 0), ShowtimeStatus.OPEN);
        addShowtime(movie, first, today.plusDays(1).atStartOfDay(), ShowtimeStatus.OPEN);
        addShowtime(movie, second, today.atTime(10, 0), ShowtimeStatus.OPEN);
        em.flush();
        em.clear();
        var result = showtimes.countTodayShowtimesByRoom();
        assertThat(result).extracting(com.fu.cinema.dto.RoomShowtimeCount::getRoomName)
                .containsExactly("Room 01", "Room 02", "Room 03");
        Map<String, Long> counts = result.stream().collect(Collectors.toMap(
                com.fu.cinema.dto.RoomShowtimeCount::getRoomName,
                com.fu.cinema.dto.RoomShowtimeCount::getShowtimeCount));
        assertThat(counts).containsExactlyInAnyOrderEntriesOf(Map.of("Room 01", 2L, "Room 02", 1L, "Room 03", 0L));
    }

    @Test
    void adminBookingSearchDetailAndStatusLockWorkOnSqlServer() {
        var data = com.fu.cinema.AdminBookingFixture.seed(em, 3);
        Long id = data.getFirst().getBookingId();
        em.clear();
        var result = bookingService.search(BookingStatus.PENDING, LocalDate.of(2026, 10, 15),
                LocalDate.of(2026, 10, 15), "NGUYEN", 0, 10);
        assertThat(result.getContent()).extracting(Booking::getBookingId).containsExactly(id);
        var detail = bookingService.getDetail(id);
        em.clear();
        assertThat(detail.getShowtime().getRoom().getRoomName()).isEqualTo("Room 01");
        assertThat(detail.getTickets()).hasSize(2);
        bookingService.confirm(id);
        em.flush();
        em.clear();
        assertThat(bookingService.getDetail(id).getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        em.clear();
        bookingService.cancelByAdmin(id);
        em.flush();
        em.clear();
        assertThat(bookingService.getDetail(id).getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    private void addShowtime(Movie movie, Room room, LocalDateTime start, ShowtimeStatus status) {
        Showtime showtime = new Showtime();
        showtime.setMovie(movie);
        showtime.setRoom(room);
        showtime.setStartTime(start);
        showtime.setEndTime(start.plusHours(2));
        showtime.setStatus(status);
        showtime.setTicketPrice(new BigDecimal("70000"));
        em.persist(showtime);
    }
}
