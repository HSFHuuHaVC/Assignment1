package com.fu.cinema.repository;

import com.fu.cinema.entity.Room;
import com.fu.cinema.enums.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByStatus(RoomStatus status);

    List<Room> findByStatusOrderByRoomNameAsc(RoomStatus status);

    boolean existsByRoomNameIgnoreCase(String roomName);

    boolean existsByRoomNameIgnoreCaseAndRoomIdNot(String roomName, Long roomId);
}
