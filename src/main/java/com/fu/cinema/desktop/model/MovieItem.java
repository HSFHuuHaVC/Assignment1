package com.fu.cinema.desktop.model;

import com.fu.cinema.entity.Movie;
import com.fu.cinema.enums.AgeRating;
import com.fu.cinema.enums.MovieStatus;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public record MovieItem(
        Long movieId,
        String title,
        String director,
        Integer duration,
        LocalDate releaseDate,
        AgeRating ageRating,
        String genreName,
        Long genreId,
        MovieStatus status,
        String posterUrl,
        String description
) {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static MovieItem from(Movie m) {
        if (m == null) return null;
        String genre = (m.getGenre() != null) ? m.getGenre().getGenreName() : "";
        Long gId = (m.getGenre() != null) ? m.getGenre().getGenreId() : null;
        return new MovieItem(
                m.getMovieId(),
                m.getTitle(),
                m.getDirector(),
                m.getDuration(),
                m.getReleaseDate(),
                m.getAgeRating(),
                genre,
                gId,
                m.getStatus(),
                m.getPosterUrl(),
                m.getDescription()
        );
    }

    public String formattedDuration() {
        return duration != null ? duration + " phút" : "";
    }

    public String formattedReleaseDate() {
        return releaseDate != null ? releaseDate.format(DATE_FORMATTER) : "";
    }

    public String formattedStatus() {
        if (status == null) return "";
        return switch (status) {
            case SHOWING -> "Đang chiếu";
            case COMING_SOON -> "Sắp chiếu";
            case STOPPED -> "Ngừng chiếu";
        };
    }
}
