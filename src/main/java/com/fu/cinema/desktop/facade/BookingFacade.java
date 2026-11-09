package com.fu.cinema.desktop.facade;

import com.fu.cinema.desktop.model.BookingDetail;
import com.fu.cinema.desktop.model.BookingRow;
import com.fu.cinema.desktop.model.PageResult;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.entity.Booking;
import com.fu.cinema.entity.Showtime;
import com.fu.cinema.entity.Ticket;
import com.fu.cinema.enums.BookingStatus;
import com.fu.cinema.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BookingFacade {

    private final BookingService bookingService;
    private final DesktopSession session;

    public PageResult<BookingRow> search(BookingStatus status, LocalDate from, LocalDate to, String keyword, int page) {
        return PageResult.of(bookingService.search(status, from, to, keyword, page, 15), BookingRow::from);
    }

    public BookingDetail detail(Long id) {
        Booking b = bookingService.getDetail(id);                     // Q5 JOIN FETCH
        Showtime s = b.getShowtime();
        return new BookingDetail(b.getBookingId(), b.getCustomer().getFullName(), b.getCustomer().getMobile(),
                s.getMovie().getTitle(), s.getRoom().getRoomName(), s.getStartTime(),
                b.getTickets().stream().map(Ticket::getSeatCode).toList(),
                b.getTotalAmount(), b.getStatus(), b.getBookingDate());
    }

    @Transactional
    public void confirm(Long id) {
        session.requireAdmin();
        bookingService.confirm(id);
    }

    @Transactional
    public void cancel(Long id) {
        session.requireAdmin();
        bookingService.cancelByAdmin(id);
    }
}

