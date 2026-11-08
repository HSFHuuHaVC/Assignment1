package com.fu.cinema.desktop.model;

import com.fu.cinema.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BookingDetail(Long id, String customerName, String mobile, String movieTitle, String roomName,
                            LocalDateTime startTime, List<String> seats, BigDecimal total,
                            BookingStatus status, LocalDateTime bookingDate) {}

