package com.fu.cinema.service.impl;

import com.fu.cinema.dto.MovieForm;
import com.fu.cinema.entity.Genre;
import com.fu.cinema.entity.Movie;
import com.fu.cinema.enums.MovieStatus;
import com.fu.cinema.exception.ResourceNotFoundException;
import com.fu.cinema.repository.GenreRepository;
import com.fu.cinema.repository.MovieRepository;
import com.fu.cinema.repository.ShowtimeRepository;
import com.fu.cinema.service.MovieService;
import com.fu.cinema.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieServiceImpl implements MovieService {

    private static final Set<String> SORTABLE = Set.of("title", "releaseDate", "duration");

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final ShowtimeRepository showtimeRepository;

    @Override
    public Page<Movie> searchForAdmin(String keyword, Long genreId, MovieStatus status, int page, int size, String sort) {
        String field = SORTABLE.contains(sort) ? sort : "releaseDate";
        Sort order = "title".equals(field) ? Sort.by(field).ascending() : Sort.by(field).descending();
        Set<MovieStatus> statuses = status == null ? EnumSet.allOf(MovieStatus.class) : EnumSet.of(status);
        return movieRepository.search(ValidationUtils.blankToNull(keyword), genreId, statuses,
                PageRequest.of(Math.max(page, 0), size, order));
    }

    @Override
    public Page<Movie> searchForCustomer(String keyword, Long genreId, int page, int size) {
        // Khách hàng không thấy phim STOPPED (BR-06)
        return movieRepository.search(ValidationUtils.blankToNull(keyword), genreId,
                EnumSet.of(MovieStatus.SHOWING, MovieStatus.COMING_SOON),
                PageRequest.of(Math.max(page, 0), size, Sort.by("releaseDate").descending()));
    }

    @Override
    public Movie getById(Long movieId) {
        return movieRepository.findWithGenreById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Phim", movieId));
    }

    @Override
    public MovieForm getForm(Long movieId) {
        Movie m = getById(movieId);
        MovieForm f = new MovieForm();
        f.setMovieId(m.getMovieId());
        f.setTitle(m.getTitle());
        f.setDirector(m.getDirector());
        f.setDuration(m.getDuration());
        f.setReleaseDate(m.getReleaseDate());
        f.setAgeRating(m.getAgeRating());
        f.setDescription(m.getDescription());
        f.setPosterUrl(m.getPosterUrl());
        f.setGenreId(m.getGenre().getGenreId());
        f.setStatus(m.getStatus());
        return f;
    }

    @Override
    @Transactional
    public void create(MovieForm form) {
        Movie movie = new Movie();
        copy(form, movie);
        movieRepository.save(movie);
    }

    @Override
    @Transactional
    public void update(Long movieId, MovieForm form) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Phim", movieId));
        copy(form, movie);
    }

    @Override
    @Transactional
    public boolean delete(Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Phim", movieId));
        if (showtimeRepository.existsByMovie_MovieId(movieId)) { // BR-06: soft delete khi đã có suất chiếu
            movie.setStatus(MovieStatus.STOPPED);
            return false;
        }
        movieRepository.delete(movie);
        return true;
    }

    @Override
    public List<Movie> findSchedulable() {
        return movieRepository.findByStatusNotOrderByTitleAsc(MovieStatus.STOPPED);
    }

    @Override
    public long countShowing() {
        return movieRepository.countByStatus(MovieStatus.SHOWING);
    }

    private void copy(MovieForm form, Movie movie) {
        Genre genre = genreRepository.findById(form.getGenreId())
                .orElseThrow(() -> new ResourceNotFoundException("Thể loại", form.getGenreId()));
        movie.setTitle(form.getTitle().trim());
        movie.setDirector(form.getDirector().trim());
        movie.setDuration(form.getDuration());
        movie.setReleaseDate(form.getReleaseDate());
        movie.setAgeRating(form.getAgeRating());
        movie.setDescription(form.getDescription().trim());
        movie.setPosterUrl(form.getPosterUrl().trim());
        movie.setGenre(genre);
        movie.setStatus(form.getStatus());
    }
}