package com.fu.cinema.repository;

import com.fu.cinema.entity.Room;
import com.fu.cinema.enums.RoomStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    /** Serialize overlap checks and writes for the same room until transaction completion. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.roomId = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);

    List<Room> findByStatus(RoomStatus status);

    List<Room> findByStatusOrderByRoomNameAsc(RoomStatus status);

    boolean existsByRoomNameIgnoreCase(String roomName);

    boolean existsByRoomNameIgnoreCaseAndRoomIdNot(String roomName, Long roomId);
}
