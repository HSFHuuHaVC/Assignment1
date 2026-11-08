package com.fu.cinema.desktop.facade;

import com.fu.cinema.desktop.model.GenreOption;
import com.fu.cinema.desktop.model.MovieItem;
import com.fu.cinema.desktop.model.PageResult;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.dto.MovieForm;
import com.fu.cinema.enums.MovieStatus;
import com.fu.cinema.service.GenreService;
import com.fu.cinema.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MovieFacade {
    private final MovieService movieService;
    private final GenreService genreService;
    private final DesktopSession session;

    @Transactional(readOnly = true)
    public PageResult<MovieItem> searchMovies(String keyword, Long genreId, MovieStatus status,
                                             int page, int size, String sort) {
        Page<com.fu.cinema.entity.Movie> result = movieService.searchForAdmin(
                keyword, genreId, status, page, size, sort
        );
        return PageResult.of(result, MovieItem::from);
    }

    @Transactional(readOnly = true)
    public List<GenreOption> getGenreOptions() {
        return genreService.findAll().stream()
                .map(GenreOption::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MovieForm getMovieForm(Long movieId) {
        return movieService.getForm(movieId);
    }

    @Transactional
    public void createMovie(MovieForm form) {
        session.requireAdmin();
        movieService.create(form);
    }

    @Transactional
    public void updateMovie(Long movieId, MovieForm form) {
        session.requireAdmin();
        movieService.update(movieId, form);
    }

    @Transactional
    public boolean deleteMovie(Long movieId) {
        session.requireAdmin();
        return movieService.delete(movieId);
    }
}
