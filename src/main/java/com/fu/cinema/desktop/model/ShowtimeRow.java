package com.fu.cinema.desktop.model;

import com.fu.cinema.enums.ShowtimeStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShowtimeRow(
        Long id,
        String movieTitle,
        String roomName,
        LocalDateTime start,
        LocalDateTime end,
        BigDecimal price,
        ShowtimeStatus status
) {}

