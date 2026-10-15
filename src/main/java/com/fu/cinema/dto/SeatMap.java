package com.fu.cinema.dto;

import com.fu.cinema.entity.Showtime;
import lombok.Value;

import java.util.List;
import java.util.Set;

// Dữ liệu cho màn hình chọn ghế
@Value
public class SeatMap {

    Showtime showtime;
    List<String> rowLabels;   // A, B, C...
    int seatsPerRow;
    Set<String> soldSeats;

    public boolean isSold(String seatCode) {
        return soldSeats.contains(seatCode);
    }
}
