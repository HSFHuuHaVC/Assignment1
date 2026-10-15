package com.fu.cinema.service;

import com.fu.cinema.dto.RoomForm;
import com.fu.cinema.entity.Room;

import java.util.List;

public interface RoomService {

    List<Room> findAll();

    List<Room> findActive();

    RoomForm getForm(Long roomId);

    void create(RoomForm form);

    void update(Long roomId, RoomForm form);

    /** @return true nếu đã xoá hẳn, false nếu chỉ chuyển MAINTENANCE */
    boolean delete(Long roomId);
}
