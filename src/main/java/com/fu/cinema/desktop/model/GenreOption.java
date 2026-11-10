package com.fu.cinema.desktop.model;

import com.fu.cinema.entity.Genre;

public record GenreOption(Long genreId, String genreName) {
    public static GenreOption from(Genre g) {
        if (g == null) return null;
        return new GenreOption(g.getGenreId(), g.getGenreName());
    }

    @Override
    public String toString() {
        return genreName;
    }
}
