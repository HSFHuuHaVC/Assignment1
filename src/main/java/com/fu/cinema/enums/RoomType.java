package com.fu.cinema.enums;

public enum RoomType {
    TWO_D("2D"), THREE_D("3D"), IMAX("IMAX");

    private final String label;

    RoomType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
