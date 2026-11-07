package com.fu.cinema.desktop;

import com.fu.cinema.desktop.facade.GenreFacade;
import com.fu.cinema.desktop.model.GenreItem;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.dto.GenreForm;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.entity.Genre;
import com.fu.cinema.enums.Role;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.repository.GenreRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.datasource.url=jdbc:h2:mem:desktopGenre;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles({"test", "desktop"})
class GenreFacadeTests {

    @Autowired GenreFacade genreFacade;
    @Autowired DesktopSession session;
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
    void getAllGenresReturnsList() {
        List<GenreItem> genres = genreFacade.getAllGenres();
        assertNotNull(genres);
        assertFalse(genres.isEmpty());
        assertTrue(genres.stream().anyMatch(g -> g.genreName() != null));
    }

    @Test
    void createGenreRequiresAdmin() {
        session.logout();
        GenreForm form = new GenreForm();
        form.setGenreName("Thể loại test");
        form.setDescription("Mô tả test");

        assertThrows(BusinessException.class, () -> genreFacade.createGenre(form));
    }

    @Test
    @Transactional
    void createGenreValidatesAndSaves() {
        GenreForm form = new GenreForm();
        form.setGenreName("Kinh dị giật gân");
        form.setDescription("Các bộ phim hồi hộp thót tim");

        genreFacade.createGenre(form);

        List<GenreItem> all = genreFacade.getAllGenres();
        assertTrue(all.stream().anyMatch(g -> "Kinh dị giật gân".equals(g.genreName())));
    }

    @Test
    @Transactional
    void createGenreRejectsDuplicateName() {
        Genre existing = genreRepository.findAll().getFirst();

        GenreForm form = new GenreForm();
        form.setGenreName(existing.getGenreName());
        form.setDescription("Mô tả trùng tên");

        BusinessException ex = assertThrows(BusinessException.class, () -> genreFacade.createGenre(form));
        assertTrue(ex.getMessage().contains("tồn tại"));
    }

    @Test
    @Transactional
    void updateGenreUpdatesFields() {
        Genre genre = genreRepository.save(new Genre("Thể loại gốc", "Mô tả gốc"));

        GenreForm form = new GenreForm();
        form.setGenreId(genre.getGenreId());
        form.setGenreName("Thể loại đã sửa");
        form.setDescription("Mô tả đã sửa");

        genreFacade.updateGenre(genre.getGenreId(), form);

        Genre updated = genreRepository.findById(genre.getGenreId()).orElseThrow();
        assertEquals("Thể loại đã sửa", updated.getGenreName());
        assertEquals("Mô tả đã sửa", updated.getDescription());
    }

    @Test
    @Transactional
    void deleteGenreDeletesWhenNoMovies() {
        Genre genre = genreRepository.save(new Genre("Thể loại cô đơn", "Không có phim nào"));
        Long id = genre.getGenreId();

        genreFacade.deleteGenre(id);

        assertFalse(genreRepository.existsById(id));
    }

    @Test
    @Transactional
    void deleteGenreRejectsWhenMoviesExist() { // TC-D07
        // Pick a genre that has seeded movies (e.g. Hành động, Hoạt hình, etc.)
        Genre genreWithMovies = genreRepository.findAll().stream()
                .filter(g -> !g.getGenreName().equalsIgnoreCase("Thể loại cô đơn"))
                .findFirst()
                .orElseThrow();

        BusinessException ex = assertThrows(BusinessException.class, () -> genreFacade.deleteGenre(genreWithMovies.getGenreId()));
        assertTrue(ex.getMessage().contains("vẫn còn phim thuộc thể loại"));
    }
}
