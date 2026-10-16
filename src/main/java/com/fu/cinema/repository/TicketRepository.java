package com.fu.cinema.repository;

import com.fu.cinema.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // ===== Q4: ghế đã bán của một suất chiếu (không tính đơn đã huỷ) =====
    @Query("""
            SELECT DISTINCT t.seatCode FROM Ticket t
            WHERE t.showtime.showtimeId = :showtimeId
              AND t.booking.status <> com.fu.cinema.enums.BookingStatus.CANCELLED
            ORDER BY t.seatCode
            """)
    List<String> findSoldSeatCodes(@Param("showtimeId") Long showtimeId);
}
