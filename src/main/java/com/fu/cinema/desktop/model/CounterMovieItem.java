package com.fu.cinema.desktop.model;

import com.fu.cinema.enums.AgeRating;

public record CounterMovieItem(Long id, String title, Integer duration, AgeRating ageRating) {
    @Override
    public String toString() {
        return title;
    }
}
