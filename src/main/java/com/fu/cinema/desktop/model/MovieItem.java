package com.fu.cinema.desktop.model;

import com.fu.cinema.enums.AgeRating;

public record MovieItem(Long id, String title, int duration, AgeRating ageRating) {
    @Override
    public String toString() {
        return title + " (" + duration + "')";
    }
}

