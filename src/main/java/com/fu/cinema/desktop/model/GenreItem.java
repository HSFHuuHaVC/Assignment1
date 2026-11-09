package com.fu.cinema.desktop.model;

import com.fu.cinema.entity.Genre;

public record GenreItem(Long genreId, String genreName, String description) {
    public static GenreItem from(Genre g) {
        if (g == null) return null;
        return new GenreItem(g.getGenreId(), g.getGenreName(), g.getDescription());
    }

    @Override
    public String toString() {
        return genreName;
    }
}
