package com.fu.cinema.service.impl;

import com.fu.cinema.dto.MovieRevenueDTO;
import com.fu.cinema.dto.ReportResult;
import com.fu.cinema.entity.Booking;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.repository.BookingRepository;
import com.fu.cinema.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final BookingRepository bookingRepository;

    @Override
    public ReportResult generate(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BusinessException("Vui lòng chọn đầy đủ từ ngày và đến ngày");
        }
        if (startDate.isAfter(endDate)) {                                                 // BR-15
            throw new BusinessException("Từ ngày phải trước hoặc bằng đến ngày");
        }
        // [startDate 00:00, endDate + 1 ngày 00:00) -> bao gồm trọn ngày cuối
        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime to = endDate.plusDays(1).atStartOfDay();

        List<MovieRevenueDTO> byMovie = bookingRepository.revenueByMovie(from, to);        // giảm dần doanh thu
        List<Booking> bookings = bookingRepository.findConfirmedInPeriod(from, to);       // giảm dần ngày đặt

        long totalTickets = byMovie.stream().mapToLong(MovieRevenueDTO::getTicketCount).sum();
        BigDecimal totalRevenue = byMovie.stream().map(MovieRevenueDTO::getRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ReportResult(startDate, endDate, byMovie, bookings, bookings.size(), totalTickets, totalRevenue);
    }
}
