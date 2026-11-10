package com.fu.cinema.desktop.model;

import com.fu.cinema.enums.BookingStatus;

import java.math.BigDecimal;
import java.util.List;

public record SaleResult(Long bookingId, List<String> seats, BigDecimal total, BookingStatus status) {}

