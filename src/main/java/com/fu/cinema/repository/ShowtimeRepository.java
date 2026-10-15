package com.fu.cinema.repository;

import com.fu.cinema.dto.RoomShowtimeCount;
import com.fu.cinema.entity.Showtime;
import com.fu.cinema.enums.ShowtimeStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {

    // ===== Derived query =====
    boolean existsByMovie_MovieId(Long movieId);

    boolean existsByRoom_RoomId(Long roomId);

    @EntityGraph(attributePaths = "room")
    List<Showtime> findByMovie_MovieIdAndStatusAndStartTimeAfterOrderByStartTimeAsc(
            Long movieId, ShowtimeStatus status, LocalDateTime after);

    // ===== Danh sách suất chiếu cho Admin (lọc tuỳ chọn) =====
    @Query(value = """
            SELECT s FROM Showtime s JOIN FETCH s.movie m JOIN FETCH s.room r
            WHERE (:movieId IS NULL OR m.movieId = :movieId)
              AND (:roomId IS NULL OR r.roomId = :roomId)
              AND (:from IS NULL OR s.startTime >= :from)
              AND (:to IS NULL OR s.startTime < :to)
            """,
            countQuery = """
            SELECT COUNT(s) FROM Showtime s
            WHERE (:movieId IS NULL OR s.movie.movieId = :movieId)
              AND (:roomId IS NULL OR s.room.roomId = :roomId)
              AND (:from IS NULL OR s.startTime >= :from)
              AND (:to IS NULL OR s.startTime < :to)
            """)
    Page<Showtime> search(@Param("movieId") Long movieId,
                          @Param("roomId") Long roomId,
                          @Param("from") LocalDateTime from,
                          @Param("to") LocalDateTime to,
                          Pageable pageable);

    @Query("SELECT s FROM Showtime s JOIN FETCH s.movie JOIN FETCH s.room WHERE s.showtimeId = :id")
    Optional<Showtime> findDetailById(@Param("id") Long id);

    // ===== Q3: suất chiếu chồng giờ trong cùng phòng (BR-08) =====
    @Query("""
            SELECT s FROM Showtime s JOIN FETCH s.movie
            WHERE s.room.roomId = :roomId
              AND s.status <> com.fu.cinema.enums.ShowtimeStatus.CANCELLED
              AND s.startTime < :end
              AND s.endTime > :start
              AND (:excludeId IS NULL OR s.showtimeId <> :excludeId)
            """)
    List<Showtime> findOverlapping(@Param("roomId") Long roomId,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end,
                                   @Param("excludeId") Long excludeId);

    // Khoá dòng suất chiếu khi đặt vé → 2 người đặt cùng lúc sẽ được xử lý tuần tự
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Showtime s WHERE s.showtimeId = :id")
    Optional<Showtime> findByIdForUpdate(@Param("id") Long id);

    // ===== Q11: native SQL Server — số suất chiếu hôm nay theo phòng (dashboard) =====
    @Query(value = """
            SELECT r.room_name AS roomName, COUNT(s.showtime_id) AS showtimeCount
            FROM rooms r
            LEFT JOIN showtimes s
                   ON s.room_id = r.room_id
                  AND s.status = 'OPEN'
                  AND CAST(s.start_time AS DATE) = CAST(GETDATE() AS DATE)
            GROUP BY r.room_name
            ORDER BY r.room_name
            """, nativeQuery = true)
    List<RoomShowtimeCount> countTodayShowtimesByRoom();
}
