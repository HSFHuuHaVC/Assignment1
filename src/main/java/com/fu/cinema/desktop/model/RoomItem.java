package com.fu.cinema.desktop.model;

public record RoomItem(Long id, String name) {
    @Override
    public String toString() {
        return name;
    }
}

