package com.fu.cinema.desktop;

import com.fu.cinema.desktop.facade.MovieFacade;
import com.fu.cinema.desktop.model.MovieItem;
import com.fu.cinema.desktop.model.PageResult;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.dto.MovieForm;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.entity.Genre;
import com.fu.cinema.entity.Movie;
import com.fu.cinema.enums.AgeRating;
import com.fu.cinema.enums.MovieStatus;
import com.fu.cinema.enums.Role;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.repository.GenreRepository;
import com.fu.cinema.repository.MovieRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.datasource.url=jdbc:h2:mem:desktopMovie;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles({"test", "desktop"})
class MovieFacadeTests {

    @Autowired MovieFacade movieFacade;
    @Autowired DesktopSession session;
    @Autowired MovieRepository movieRepository;
    @Autowired GenreRepository genreRepository;

    @BeforeEach
    void setupAdminSession() {
        session.login(new SessionUser(1L, null, "Administrator", Role.ADMIN));
    }

    @AfterEach
    void clearSession() {
        session.logout();
    }

    @Test
    void searchMoviesReturnsPagedResultWithoutLazyInitializationException() { // TC-D08
        PageResult<MovieItem> result = movieFacade.searchMovies(null, null, null, 0, 10, "releaseDate");
        assertNotNull(result);
        assertFalse(result.items().isEmpty());

        // Verify eagerly mapped fields in record
        MovieItem first = result.items().getFirst();
        assertNotNull(first.title());
        assertNotNull(first.genreName());
        assertNotNull(first.formattedDuration());
        assertNotNull(first.formattedReleaseDate());
    }

    @Test
    void searchMoviesFiltersByKeywordGenreAndStatus() { // TC-D08
        PageResult<MovieItem> all = movieFacade.searchMovies(null, null, null, 0, 20, "releaseDate");
        assertFalse(all.items().isEmpty());

        MovieItem sample = all.items().getFirst();
        String keyword = sample.title().substring(0, Math.min(3, sample.title().length()));

        PageResult<MovieItem> keywordResult = movieFacade.searchMovies(keyword, null, null, 0, 10, "title");
        assertTrue(keywordResult.items().stream().anyMatch(m -> m.title().toLowerCase().contains(keyword.toLowerCase())));

        PageResult<MovieItem> statusResult = movieFacade.searchMovies(null, null, MovieStatus.SHOWING, 0, 10, "releaseDate");
        assertTrue(statusResult.items().stream().allMatch(m -> m.status() == MovieStatus.SHOWING));

        if (sample.genreId() != null) {
            PageResult<MovieItem> genreResult = movieFacade.searchMovies(null, sample.genreId(), null, 0, 10, "releaseDate");
            assertTrue(genreResult.items().stream().allMatch(m -> sample.genreId().equals(m.genreId())));
        }
    }

    @Test
    void createMovieRequiresAdmin() {
        session.logout();
        MovieForm form = new MovieForm();
        form.setTitle("Unauthorized Movie");

        assertThrows(BusinessException.class, () -> movieFacade.createMovie(form));
    }

    @Test
    @Transactional
    void createAndGetMovieForm() { // TC-D09
        Genre genre = genreRepository.findAll().getFirst();

        MovieForm form = new MovieForm();
        form.setTitle("Chiến binh vũ trụ");
        form.setDirector("Đạo diễn Quang");
        form.setDuration(135);
        form.setReleaseDate(LocalDate.now().plusDays(5));
        form.setAgeRating(AgeRating.T16);
        form.setGenreId(genre.getGenreId());
        form.setStatus(MovieStatus.COMING_SOON);
        form.setPosterUrl("/images/posters/test.jpg");
        form.setDescription("Phim hành động viễn tưởng hấp dẫn");

        movieFacade.createMovie(form);

        PageResult<MovieItem> search = movieFacade.searchMovies("Chiến binh vũ trụ", null, null, 0, 5, "title");
        assertFalse(search.items().isEmpty());
        MovieItem saved = search.items().getFirst();
        assertEquals("Chiến binh vũ trụ", saved.title());

        MovieForm loadedForm = movieFacade.getMovieForm(saved.movieId());
        assertEquals("Đạo diễn Quang", loadedForm.getDirector());
        assertEquals(135, loadedForm.getDuration());
        assertEquals(genre.getGenreId(), loadedForm.getGenreId());
    }

    @Test
    @Transactional
    void updateMovieUpdatesFields() {
        Genre genre = genreRepository.findAll().getFirst();

        Movie movie = new Movie();
        movie.setTitle("Phim thử nghiệm");
        movie.setDirector("Đạo diễn A");
        movie.setDuration(100);
        movie.setReleaseDate(LocalDate.now());
        movie.setAgeRating(AgeRating.P);
        movie.setGenre(genre);
        movie.setStatus(MovieStatus.COMING_SOON);
        movie.setPosterUrl("/test.jpg");
        movie.setDescription("Mô tả test");
        movie = movieRepository.save(movie);

        MovieForm updateForm = new MovieForm();
        updateForm.setMovieId(movie.getMovieId());
        updateForm.setTitle("Phim đã cập nhật");
        updateForm.setDirector("Đạo diễn B");
        updateForm.setDuration(110);
        updateForm.setReleaseDate(LocalDate.now().plusDays(2));
        updateForm.setAgeRating(AgeRating.T13);
        updateForm.setGenreId(genre.getGenreId());
        updateForm.setStatus(MovieStatus.SHOWING);
        updateForm.setPosterUrl("/test_updated.jpg");
        updateForm.setDescription("Mô tả cập nhật");

        movieFacade.updateMovie(movie.getMovieId(), updateForm);

        Movie updated = movieRepository.findById(movie.getMovieId()).orElseThrow();
        assertEquals("Phim đã cập nhật", updated.getTitle());
        assertEquals("Đạo diễn B", updated.getDirector());
        assertEquals(110, updated.getDuration());
        assertEquals(MovieStatus.SHOWING, updated.getStatus());
    }

    @Test
    @Transactional
    void deleteMovieWithoutShowtimesIsHardDeleted() { // TC-D10
        Genre genre = genreRepository.findAll().getFirst();
        Movie movie = new Movie();
        movie.setTitle("Phim không có suất");
        movie.setDirector("Đạo diễn C");
        movie.setDuration(90);
        movie.setReleaseDate(LocalDate.now());
        movie.setAgeRating(AgeRating.P);
        movie.setGenre(genre);
        movie.setStatus(MovieStatus.COMING_SOON);
        movie.setPosterUrl("/test.jpg");
        movie.setDescription("Test");
        movie = movieRepository.save(movie);

        Long movieId = movie.getMovieId();
        boolean hardDeleted = movieFacade.deleteMovie(movieId);

        assertTrue(hardDeleted);
        assertFalse(movieRepository.existsById(movieId));
    }

    @Test
    @Transactional
    void deleteMovieWithShowtimesIsSoftDeletedToStopped() { // TC-D10
        // Seeded movies have showtimes
        Movie movieWithShowtimes = movieRepository.findAll().stream()
                .filter(m -> m.getStatus() != MovieStatus.STOPPED)
                .findFirst()
                .orElseThrow();

        Long movieId = movieWithShowtimes.getMovieId();
        boolean hardDeleted = movieFacade.deleteMovie(movieId);

        assertFalse(hardDeleted, "Movie with showtimes should be soft deleted");
        Movie updated = movieRepository.findById(movieId).orElseThrow();
        assertEquals(MovieStatus.STOPPED, updated.getStatus());
    }
}
