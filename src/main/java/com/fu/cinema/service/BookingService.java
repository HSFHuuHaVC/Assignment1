package com.fu.cinema.service;

import com.fu.cinema.dto.BookingRequest;
import com.fu.cinema.entity.Booking;
import com.fu.cinema.enums.BookingStatus;
import org.springframework.data.domain.Page;

import java.time.LocalDate;

public interface BookingService {

    // ===== Customer =====
    Booking create(Long customerId, BookingRequest request);

    Page<Booking> findByCustomer(Long customerId, BookingStatus status, int page, int size);

    Booking getDetailForCustomer(Long bookingId, Long customerId);

    void cancelByCustomer(Long bookingId, Long customerId);

    // ===== Admin =====
    Page<Booking> search(BookingStatus status, LocalDate fromDate, LocalDate toDate, String keyword, int page, int size);

    Booking getDetail(Long bookingId);

    void confirm(Long bookingId);

    void cancelByAdmin(Long bookingId);

    long countPending();
}
