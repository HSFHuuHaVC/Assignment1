package com.fu.cinema.desktop.facade;

import com.fu.cinema.desktop.model.ReportBookingRow;
import com.fu.cinema.desktop.model.ReportData;
import com.fu.cinema.desktop.model.RevenueRow;
import com.fu.cinema.dto.ReportResult;
import com.fu.cinema.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReportFacade {

    private final ReportService reportService;

    public ReportData generate(LocalDate start, LocalDate end) {
        ReportResult r = reportService.generate(start, end); // BR-15 (Start > End -> BusinessException)
        return new ReportData(
                start,
                end,
                r.getRevenueByMovie().stream()
                        .map(m -> new RevenueRow(m.getTitle(), m.getTicketCount(), m.getRevenue()))
                        .toList(),
                r.getBookings().stream()
                        .map(b -> new ReportBookingRow(
                                b.getBookingId(),
                                b.getBookingDate(),
                                b.getCustomer().getFullName(),
                                b.getShowtime().getMovie().getTitle(),
                                b.getTotalAmount()))
                        .toList(),
                r.getTotalBookings(),
                r.getTotalTickets(),
                r.getTotalRevenue()
        );
    }
}

