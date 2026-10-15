package com.fu.cinema.repository;

import com.fu.cinema.dto.MovieRevenueDTO;
import com.fu.cinema.entity.Booking;
import com.fu.cinema.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // ===== Derived query =====
    boolean existsByCustomer_CustomerId(Long customerId);

    boolean existsByShowtime_ShowtimeId(Long showtimeId);

    long countByStatus(BookingStatus status);

    @EntityGraph(attributePaths = {"showtime", "showtime.movie", "showtime.room"})
    Page<Booking> findByCustomer_CustomerId(Long customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"showtime", "showtime.movie", "showtime.room"})
    Page<Booking> findByCustomer_CustomerIdAndStatus(Long customerId, BookingStatus status, Pageable pageable);

    // ===== Q5: chi tiết đơn kèm khách, suất chiếu, phim, phòng, vé =====
    @Query("""
            SELECT DISTINCT b FROM Booking b
            JOIN FETCH b.customer c
            JOIN FETCH b.showtime s
            JOIN FETCH s.movie
            JOIN FETCH s.room
            LEFT JOIN FETCH b.tickets
            WHERE b.bookingId = :id
            """)
    Optional<Booking> findDetailById(@Param("id") Long id);

    // ===== Q6: lọc đơn cho Admin (trạng thái, khoảng ngày, từ khoá khách hàng) =====
    @Query(value = """
            SELECT b FROM Booking b
            JOIN FETCH b.customer c
            JOIN FETCH b.showtime s
            JOIN FETCH s.movie m
            WHERE (:status IS NULL OR b.status = :status)
              AND (:from IS NULL OR b.bookingDate >= :from)
              AND (:to IS NULL OR b.bookingDate < :to)
              AND (:kw IS NULL
                   OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :kw, '%'))
                   OR LOWER(c.email)    LIKE LOWER(CONCAT('%', :kw, '%'))
                   OR c.mobile          LIKE CONCAT('%', :kw, '%'))
            ORDER BY b.bookingDate DESC, b.bookingId DESC
            """,
            countQuery = """
            SELECT COUNT(b) FROM Booking b JOIN b.customer c
            WHERE (:status IS NULL OR b.status = :status)
              AND (:from IS NULL OR b.bookingDate >= :from)
              AND (:to IS NULL OR b.bookingDate < :to)
              AND (:kw IS NULL
                   OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :kw, '%'))
                   OR LOWER(c.email)    LIKE LOWER(CONCAT('%', :kw, '%'))
                   OR c.mobile          LIKE CONCAT('%', :kw, '%'))
            """)
    Page<Booking> search(@Param("status") BookingStatus status,
                         @Param("from") LocalDateTime from,
                         @Param("to") LocalDateTime to,
                         @Param("kw") String keyword,
                         Pageable pageable);

    // ===== Q7: doanh thu theo phim trong kỳ, giảm dần (constructor expression → DTO) =====
    @Query("""
            SELECT new com.fu.cinema.dto.MovieRevenueDTO(m.title, COUNT(t), SUM(t.price))
            FROM Ticket t
            JOIN t.booking b
            JOIN t.showtime s
            JOIN s.movie m
            WHERE b.status = com.fu.cinema.enums.BookingStatus.CONFIRMED
              AND b.bookingDate >= :from AND b.bookingDate < :to
            GROUP BY m.movieId, m.title
            ORDER BY SUM(t.price) DESC
            """)
    List<MovieRevenueDTO> revenueByMovie(@Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);

    // ===== Q8: đơn CONFIRMED trong kỳ, mới nhất trước =====
    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.customer
            JOIN FETCH b.showtime s
            JOIN FETCH s.movie
            WHERE b.status = com.fu.cinema.enums.BookingStatus.CONFIRMED
              AND b.bookingDate >= :from AND b.bookingDate < :to
            ORDER BY b.bookingDate DESC
            """)
    List<Booking> findConfirmedInPeriod(@Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to);

    // ===== Q9: huỷ hàng loạt đơn của một suất chiếu (BR-09) =====
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Booking b SET b.status = com.fu.cinema.enums.BookingStatus.CANCELLED
            WHERE b.showtime.showtimeId = :showtimeId
              AND b.status <> com.fu.cinema.enums.BookingStatus.CANCELLED
            """)
    int cancelAllByShowtime(@Param("showtimeId") Long showtimeId);

    // ===== Q12: khách đã xem phim chưa (BR-16) =====
    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.customer.customerId = :customerId
              AND b.showtime.movie.movieId = :movieId
              AND b.status = com.fu.cinema.enums.BookingStatus.CONFIRMED
              AND b.showtime.endTime < :now
            """)
    long countWatched(@Param("customerId") Long customerId,
                       @Param("movieId") Long movieId,
                       @Param("now") LocalDateTime now);
}
