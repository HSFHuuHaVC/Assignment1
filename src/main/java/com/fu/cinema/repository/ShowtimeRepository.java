package com.fu.cinema.repository;

import com.fu.cinema.entity.Showtime;
import com.fu.cinema.enums.ShowtimeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
