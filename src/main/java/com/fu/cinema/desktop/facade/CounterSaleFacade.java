package com.fu.cinema.desktop.facade;

import com.fu.cinema.desktop.model.CustomerItem;
import com.fu.cinema.desktop.model.CounterMovieItem;
import com.fu.cinema.desktop.model.SaleResult;
import com.fu.cinema.desktop.model.SeatMapData;
import com.fu.cinema.desktop.model.ShowtimeItem;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.dto.BookingRequest;
import com.fu.cinema.dto.SeatMap;
import com.fu.cinema.entity.Booking;
import com.fu.cinema.entity.Showtime;
import com.fu.cinema.service.BookingService;
import com.fu.cinema.service.CustomerService;
import com.fu.cinema.service.MovieService;
import com.fu.cinema.service.ShowtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CounterSaleFacade {

    private final CustomerService customerService;
    private final MovieService movieService;
    private final ShowtimeService showtimeService;
    private final BookingService bookingService;
    private final DesktopSession session;

    public List<CustomerItem> searchCustomers(String keyword) {
        return customerService.search(keyword, 0, 20).map(CustomerItem::from).getContent();
    }

    public List<CounterMovieItem> movies() {
        return movieService.findSchedulable().stream()
                .map(m -> new CounterMovieItem(m.getMovieId(), m.getTitle(), m.getDuration(), m.getAgeRating())).toList();
    }

    public List<ShowtimeItem> showtimes(Long movieId) {                   // chỉ suất OPEN sắp tới
        return showtimeService.findUpcomingByMovie(movieId).stream()
                .map(s -> new ShowtimeItem(s.getShowtimeId(), s.getStartTime(), s.getRoom().getRoomName(), s.getTicketPrice()))
                .toList();
    }

    public SeatMapData seatMap(Long showtimeId) {
        SeatMap m = showtimeService.getSeatMap(showtimeId);          // BR-10: suất không OPEN/đã bắt đầu → BusinessException
        Showtime s = m.getShowtime();
        return new SeatMapData(showtimeId, s.getMovie().getTitle(), s.getRoom().getRoomName(), s.getStartTime(),
                s.getTicketPrice(), List.copyOf(m.getRowLabels()), m.getSeatsPerRow(), Set.copyOf(m.getSoldSeats()));
    }

    /** BR-10, BR-11, BR-12 kiểm tra trong BookingService.create (khoá PESSIMISTIC_WRITE suất chiếu). */
    @Transactional                                                          // ⚠ KHÔNG readOnly
    public SaleResult sell(Long customerId, Long showtimeId, List<String> seats, boolean paid) {
        session.requireAdmin();
        BookingRequest request = new BookingRequest();
        request.setShowtimeId(showtimeId);
        request.setSeatCodes(new ArrayList<>(seats));
        Booking booking = bookingService.create(customerId, request);       // PENDING
        if (paid) {
            bookingService.confirm(booking.getBookingId());                 // BR-D2 → CONFIRMED (cùng transaction)
        }
        return new SaleResult(booking.getBookingId(), seats.stream().sorted(SEAT_ORDER).toList(),
                booking.getTotalAmount(), booking.getStatus());
    }

    /** A2 < A10 < B1 */
    public static final Comparator<String> SEAT_ORDER = Comparator
            .comparing((String s) -> s.charAt(0))
            .thenComparingInt(s -> Integer.parseInt(s.substring(1)));
}

