package com.fu.cinema.dto;

// Interface projection cho native query: tên getter khớp alias trong SQL
public interface RoomShowtimeCount {
    String getRoomName();

    Long getShowtimeCount();
}
