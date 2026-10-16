package com.fu.cinema.service.impl;

import com.fu.cinema.dto.RoomForm;
import com.fu.cinema.entity.Room;
import com.fu.cinema.enums.RoomStatus;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.exception.ResourceNotFoundException;
import com.fu.cinema.repository.RoomRepository;
import com.fu.cinema.repository.ShowtimeRepository;
import com.fu.cinema.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final ShowtimeRepository showtimeRepository;

    @Override
    public List<Room> findAll() {
        return roomRepository.findAll(Sort.by("roomName"));
    }

    @Override
    public List<Room> findActive() {
        return roomRepository.findByStatusOrderByRoomNameAsc(RoomStatus.ACTIVE);
    }

    @Override
    public RoomForm getForm(Long roomId) {
        Room r = find(roomId);
        RoomForm f = new RoomForm();
        f.setRoomId(r.getRoomId());
        f.setRoomName(r.getRoomName());
        f.setRoomType(r.getRoomType());
        f.setTotalRows(r.getTotalRows());
        f.setSeatsPerRow(r.getSeatsPerRow());
        f.setStatus(r.getStatus());
        return f;
    }

    @Override
    @Transactional
    public void create(RoomForm form) {
        if (roomRepository.existsByRoomNameIgnoreCase(form.getRoomName().trim())) {
            throw new BusinessException("Tên phòng đã tồn tại");
        }
        roomRepository.save(new Room(form.getRoomName().trim(), form.getRoomType(),
                form.getTotalRows(), form.getSeatsPerRow(), form.getStatus()));
    }

    @Override
    @Transactional
    public void update(Long roomId, RoomForm form) {
        Room room = find(roomId);
        if (roomRepository.existsByRoomNameIgnoreCaseAndRoomIdNot(form.getRoomName().trim(), roomId)) {
            throw new BusinessException("Tên phòng đã tồn tại");
        }
        boolean layoutChanged = !room.getTotalRows().equals(form.getTotalRows())
                || !room.getSeatsPerRow().equals(form.getSeatsPerRow());
        if (layoutChanged && showtimeRepository.existsByRoom_RoomId(roomId)) {
            throw new BusinessException("Không thể đổi sơ đồ ghế của phòng đã có suất chiếu");
        }
        room.setRoomName(form.getRoomName().trim());
        room.setRoomType(form.getRoomType());
        room.setTotalRows(form.getTotalRows());
        room.setSeatsPerRow(form.getSeatsPerRow());
        room.setStatus(form.getStatus());
    }

    @Override
    @Transactional
    public boolean delete(Long roomId) {
        Room room = find(roomId);
        if (showtimeRepository.existsByRoom_RoomId(roomId)) {
            room.setStatus(RoomStatus.MAINTENANCE);
            return false;
        }
        roomRepository.delete(room);
        return true;
    }

    private Room find(Long id) {
        return roomRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Phòng chiếu", id));
    }
}