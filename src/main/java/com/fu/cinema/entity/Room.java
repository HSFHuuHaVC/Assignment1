package com.fu.cinema.entity;

import com.fu.cinema.enums.RoomStatus;
import com.fu.cinema.enums.RoomType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roomId;

    @Column(nullable = false, unique = true, length = 50)
    private String roomName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RoomType roomType;

    @Column(nullable = false)
    private Integer totalRows;               // 1..26 (A..Z)

    @Column(nullable = false)
    private Integer seatsPerRow;             // 1..30

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomStatus status;

    public Room(String roomName, RoomType roomType, int totalRows, int seatsPerRow, RoomStatus status) {
        this.roomName = roomName;
        this.roomType = roomType;
        this.totalRows = totalRows;
        this.seatsPerRow = seatsPerRow;
        this.status = status;
    }

    public int getCapacity() {
        return totalRows * seatsPerRow;
    }
}
