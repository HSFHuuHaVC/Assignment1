package com.fu.cinema.service.impl;

import com.fu.cinema.dto.BookingRequest;
import com.fu.cinema.entity.Booking;
import com.fu.cinema.entity.Customer;
import com.fu.cinema.entity.Showtime;
import com.fu.cinema.entity.Ticket;
import com.fu.cinema.enums.BookingStatus;
import com.fu.cinema.enums.ShowtimeStatus;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.exception.ResourceNotFoundException;
import com.fu.cinema.repository.BookingRepository;
import com.fu.cinema.repository.CustomerRepository;
import com.fu.cinema.repository.ShowtimeRepository;
import com.fu.cinema.repository.TicketRepository;
import com.fu.cinema.service.BookingService;
import com.fu.cinema.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    static final int MAX_SEATS_PER_BOOKING = 8;
    static final int CANCEL_BEFORE_HOURS = 2;
    private static final Pattern SEAT_PATTERN = Pattern.compile("^([A-Z])([1-9]\\d?)$");

    private final BookingRepository bookingRepository;
    private final ShowtimeRepository showtimeRepository;
    private final CustomerRepository customerRepository;
    private final TicketRepository ticketRepository;

    // ================= Customer =================

    @Override
    @Transactional
    public Booking create(Long customerId, BookingRequest request) {
        List<String> seats = request.getSeatCodes().stream()
                .filter(Objects::nonNull)
                .map(s -> s.trim().toUpperCase())
                .distinct()
                .sorted()
                .toList();
        if (seats.isEmpty()) {
            throw new BusinessException("Vui lòng chọn ít nhất 1 ghế");
        }
        if (seats.size() > MAX_SEATS_PER_BOOKING) {
            throw new BusinessException("Mỗi đơn tối đa " + MAX_SEATS_PER_BOOKING + " ghế");
        }

        Showtime showtime = showtimeRepository.findByIdForUpdate(request.getShowtimeId())
                .orElseThrow(() -> new ResourceNotFoundException("Suất chiếu", request.getShowtimeId()));
        if (showtime.getStatus() != ShowtimeStatus.OPEN || !showtime.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException("Suất chiếu không còn mở bán");
        }

        int rows = showtime.getRoom().getTotalRows();
        int perRow = showtime.getRoom().getSeatsPerRow();
        for (String seat : seats) {
            Matcher m = SEAT_PATTERN.matcher(seat);
            if (!m.matches() || m.group(1).charAt(0) - 'A' >= rows || Integer.parseInt(m.group(2)) > perRow) {
                throw new BusinessException("Ghế " + seat + " không tồn tại trong phòng " + showtime.getRoom().getRoomName());
            }
        }

        Set<String> sold = new HashSet<>(ticketRepository.findSoldSeatCodes(showtime.getShowtimeId()));
        List<String> taken = seats.stream().filter(sold::contains).toList();
        if (!taken.isEmpty()) {
            throw new BusinessException("Ghế đã có người đặt: " + String.join(", ", taken));
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Khách hàng", customerId));
        int minAge = showtime.getMovie().getAgeRating().getMinAge();
        int ageAtShow = Period.between(customer.getBirthday(), showtime.getStartTime().toLocalDate()).getYears();
        if (ageAtShow < minAge) {
            throw new BusinessException("Phim \"" + showtime.getMovie().getTitle() + "\" chỉ dành cho khán giả từ "
                    + minAge + " tuổi trở lên");
        }

        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setShowtime(showtime);
        booking.setBookingDate(LocalDateTime.now());
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalAmount(showtime.getTicketPrice().multiply(BigDecimal.valueOf(seats.size())));
        seats.forEach(seat -> booking.addTicket(new Ticket(showtime, seat, showtime.getTicketPrice())));
        return bookingRepository.save(booking);
    }

    @Override
    public Page<Booking> findByCustomer(Long customerId, BookingStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), size, Sort.by("bookingDate").descending());
        return status == null
                ? bookingRepository.findByCustomer_CustomerId(customerId, pageable)
                : bookingRepository.findByCustomer_CustomerIdAndStatus(customerId, status, pageable);
    }

    @Override
    public Booking getDetailForCustomer(Long bookingId, Long customerId) {
        Booking booking = getDetail(bookingId);
        if (!booking.getCustomer().getCustomerId().equals(customerId)) {
            throw new ResourceNotFoundException("Đơn đặt vé", bookingId);
        }
        return booking;
    }

    @Override
    @Transactional
    public void cancelByCustomer(Long bookingId, Long customerId) {
        Booking booking = getDetailForCustomer(bookingId, customerId);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessException("Đơn đã được huỷ trước đó");
        }
        if (LocalDateTime.now().isAfter(booking.getShowtime().getStartTime().minusHours(CANCEL_BEFORE_HOURS))) {
            throw new BusinessException("Chỉ được huỷ đơn trước giờ chiếu ít nhất " + CANCEL_BEFORE_HOURS + " giờ");
        }
        booking.setStatus(BookingStatus.CANCELLED);
    }

    // ================= Admin =================

    @Override
    public Page<Booking> search(BookingStatus status, LocalDate fromDate, LocalDate toDate, String keyword, int page, int size) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BusinessException("Từ ngày phải trước hoặc bằng đến ngày");
        }
        return bookingRepository.search(status,
                fromDate == null ? null : fromDate.atStartOfDay(),
                toDate == null ? null : toDate.plusDays(1).atStartOfDay(),
                ValidationUtils.blankToNull(keyword),
                PageRequest.of(Math.max(page, 0), size, Sort.by("bookingDate").descending()));
    }

    @Override
    public Booking getDetail(Long bookingId) {
        return bookingRepository.findDetailById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn đặt vé", bookingId));
    }

    @Override
    @Transactional
    public void confirm(Long bookingId) {
        Booking booking = getDetail(bookingId);
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException("Chỉ xác nhận được đơn đang ở trạng thái PENDING");
        }
        booking.setStatus(BookingStatus.CONFIRMED);
    }

    @Override
    @Transactional
    public void cancelByAdmin(Long bookingId) {
        Booking booking = getDetail(bookingId);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessException("Đơn đã được huỷ trước đó");
        }
        booking.setStatus(BookingStatus.CANCELLED);
    }

    @Override
    public long countPending() {
        return bookingRepository.countByStatus(BookingStatus.PENDING);
    }
}
