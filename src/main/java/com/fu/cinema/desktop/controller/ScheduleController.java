package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.facade.RoomFacade;
import com.fu.cinema.desktop.facade.ShowtimeFacade;
import com.fu.cinema.desktop.model.PageResult;
import com.fu.cinema.desktop.model.RoomItem;
import com.fu.cinema.desktop.model.RoomRow;
import com.fu.cinema.desktop.model.ScheduleMovieItem;
import com.fu.cinema.desktop.model.ShowtimeRow;
import com.fu.cinema.desktop.util.AlertUtil;
import com.fu.cinema.desktop.util.FxAsync;
import com.fu.cinema.desktop.util.FxValidator;
import com.fu.cinema.desktop.util.MoneyUtil;
import com.fu.cinema.dto.RoomForm;
import com.fu.cinema.dto.ShowtimeForm;
import com.fu.cinema.enums.RoomStatus;
import com.fu.cinema.enums.RoomType;
import com.fu.cinema.enums.ShowtimeStatus;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class ScheduleController {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM");

    private final ShowtimeFacade showtimeFacade;
    private final RoomFacade roomFacade;
    private final FxAsync fxAsync;
    private final FxValidator validator;

    // --- Tab Suất chiếu ---
    @FXML private ComboBox<ScheduleMovieItem> cbFilterMovie;
    @FXML private ComboBox<RoomItem> cbFilterRoom;
    @FXML private DatePicker dpFilterDate;

    @FXML private TableView<ShowtimeRow> tableShowtimes;
    @FXML private TableColumn<ShowtimeRow, Long> colStId;
    @FXML private TableColumn<ShowtimeRow, String> colStMovie;
    @FXML private TableColumn<ShowtimeRow, String> colStRoom;
    @FXML private TableColumn<ShowtimeRow, String> colStStart;
    @FXML private TableColumn<ShowtimeRow, String> colStEnd;
    @FXML private TableColumn<ShowtimeRow, String> colStPrice;
    @FXML private TableColumn<ShowtimeRow, ShowtimeStatus> colStStatus;
    @FXML private Pagination paginationShowtimes;

    @FXML private VBox formBoxShowtime;
    @FXML private Label lblShowtimeFormTitle;
    @FXML private ComboBox<ScheduleMovieItem> cbMovie;
    @FXML private ComboBox<RoomItem> cbRoom;
    @FXML private DatePicker dpDate;
    @FXML private Spinner<Integer> spHour;
    @FXML private Spinner<Integer> spMinute;
    @FXML private TextField tfPrice;
    @FXML private Label lblEnd;
    @FXML private ComboBox<ShowtimeStatus> cbStatus;
    @FXML private Button btnSaveShowtime;
    @FXML private Button btnResetShowtime;
    @FXML private Button btnDeleteShowtime;

    // --- Tab Phòng chiếu ---
    @FXML private TableView<RoomRow> tableRooms;
    @FXML private TableColumn<RoomRow, Long> colRoomId;
    @FXML private TableColumn<RoomRow, String> colRoomName;
    @FXML private TableColumn<RoomRow, RoomType> colRoomType;
    @FXML private TableColumn<RoomRow, Integer> colRoomRows;
    @FXML private TableColumn<RoomRow, Integer> colRoomSeatsPerRow;
    @FXML private TableColumn<RoomRow, Integer> colRoomCapacity;
    @FXML private TableColumn<RoomRow, RoomStatus> colRoomStatus;

    @FXML private VBox formBoxRoom;
    @FXML private Label lblRoomFormTitle;
    @FXML private TextField tfRoomName;
    @FXML private ComboBox<RoomType> cbRoomType;
    @FXML private Spinner<Integer> spRoomRows;
    @FXML private Spinner<Integer> spRoomSeatsPerRow;
    @FXML private ComboBox<RoomStatus> cbRoomStatus;
    @FXML private Button btnSaveRoom;
    @FXML private Button btnResetRoom;
    @FXML private Button btnDeleteRoom;

    private final ObservableList<ShowtimeRow> showtimeList = FXCollections.observableArrayList();
    private final ObservableList<RoomRow> roomList = FXCollections.observableArrayList();

    private Long editingShowtimeId;
    private Long editingRoomId;

    @FXML
    private void initialize() {
        initShowtimeTab();
        initRoomTab();
        loadDropdownData();
        loadShowtimes(0);
        loadRooms();
    }

    // ==========================================
    // 1. SETUP TAB SUẤT CHIẾU
    // ==========================================
    private void initShowtimeTab() {
        colStId.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().id()));
        colStMovie.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().movieTitle()));
        colStRoom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().roomName()));
        colStStart.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().start() != null ? c.getValue().start().format(TIME_FMT) : ""));
        colStEnd.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().end() != null ? c.getValue().end().format(TIME_FMT) : ""));
        colStPrice.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().price() != null ? MoneyUtil.format(c.getValue().price()) : ""));
        colStStatus.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().status()));
        colStStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(ShowtimeStatus st, boolean empty) {
                super.updateItem(st, empty);
                setText(empty || st == null ? null : st.name());
            }
        });

        tableShowtimes.setItems(showtimeList);
        tableShowtimes.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> selectShowtime(newV));

        // Form controls setup
        spHour.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 19));
        spMinute.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 55, 0, 5));
        tfPrice.setTextFormatter(new TextFormatter<>(c -> c.getControlNewText().matches("\\d{0,9}") ? c : null));

        dpDate.setDayCellFactory(p -> new DateCell() {
            @Override
            public void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setDisable(empty || d.isBefore(LocalDate.now()));
            }
        });

        cbStatus.getItems().setAll(ShowtimeStatus.values());
        cbStatus.setValue(ShowtimeStatus.OPEN);

        lblEnd.textProperty().bind(Bindings.createStringBinding(() -> {
            LocalDateTime end = suggestedEnd();
            return end == null ? "—" : "Kết thúc (gợi ý): " + end.format(DateTimeFormatter.ofPattern("HH:mm dd/MM"));
        }, cbMovie.valueProperty(), dpDate.valueProperty(), spHour.valueProperty(), spMinute.valueProperty()));

        paginationShowtimes.currentPageIndexProperty().addListener((obs, oldIdx, newIdx) -> loadShowtimes(newIdx.intValue()));

        btnDeleteShowtime.setDisable(true);
    }

    private LocalDateTime startTime() {
        return dpDate.getValue() == null ? null
                : LocalDateTime.of(dpDate.getValue(), LocalTime.of(spHour.getValue(), spMinute.getValue()));
    }

    private LocalDateTime suggestedEnd() {
        LocalDateTime start = startTime();
        return (start == null || cbMovie.getValue() == null) ? null
                : start.plusMinutes(cbMovie.getValue().duration() + 15L);
    }

    private void loadDropdownData() {
        fxAsync.run(showtimeFacade::schedulableMovies, movies -> {
            cbFilterMovie.getItems().clear();
            cbFilterMovie.getItems().add(null);
            cbFilterMovie.getItems().addAll(movies);

            cbMovie.getItems().setAll(movies);
        }, null);

        fxAsync.run(showtimeFacade::activeRooms, rooms -> {
            cbFilterRoom.getItems().clear();
            cbFilterRoom.getItems().add(null);
            cbFilterRoom.getItems().addAll(rooms);

            cbRoom.getItems().setAll(rooms);
        }, null);
    }

    public void loadShowtimes(int page) {
        Long movieId = cbFilterMovie.getValue() != null ? cbFilterMovie.getValue().id() : null;
        Long roomId = cbFilterRoom.getValue() != null ? cbFilterRoom.getValue().id() : null;
        LocalDate date = dpFilterDate.getValue();

        fxAsync.run(() -> showtimeFacade.search(movieId, roomId, date, page), result -> {
            showtimeList.setAll(result.items());
            int totalPages = Math.max(1, result.totalPages());
            paginationShowtimes.setPageCount(totalPages);
            paginationShowtimes.setCurrentPageIndex(result.page());
        }, tableShowtimes);
    }

    @FXML
    private void onFilterShowtimes() {
        loadShowtimes(0);
    }

    @FXML
    private void onFilterReset() {
        cbFilterMovie.setValue(null);
        cbFilterRoom.setValue(null);
        dpFilterDate.setValue(null);
        loadShowtimes(0);
    }

    private void selectShowtime(ShowtimeRow row) {
        if (row == null) {
            onResetShowtime();
            return;
        }
        editingShowtimeId = row.id();
        lblShowtimeFormTitle.setText("Sửa suất chiếu #" + row.id());
        btnSaveShowtime.setText("Cập nhật");
        btnDeleteShowtime.setDisable(false);

        // Load detail form
        fxAsync.run(() -> showtimeFacade.getForm(row.id()), form -> {
            if (form == null) return;
            cbMovie.getItems().stream()
                    .filter(m -> m.id().equals(form.getMovieId()))
                    .findFirst().ifPresent(cbMovie::setValue);

            cbRoom.getItems().stream()
                    .filter(r -> r.id().equals(form.getRoomId()))
                    .findFirst().ifPresent(cbRoom::setValue);

            if (form.getStartTime() != null) {
                dpDate.setValue(form.getStartTime().toLocalDate());
                spHour.getValueFactory().setValue(form.getStartTime().getHour());
                spMinute.getValueFactory().setValue(form.getStartTime().getMinute());
            }
            if (form.getTicketPrice() != null) {
                tfPrice.setText(form.getTicketPrice().toPlainString());
            }
            if (form.getStatus() != null) {
                cbStatus.setValue(form.getStatus());
            }
        }, formBoxShowtime);
    }

    @FXML
    private void onResetShowtime() {
        editingShowtimeId = null;
        lblShowtimeFormTitle.setText("Thêm suất chiếu mới");
        cbMovie.setValue(null);
        cbRoom.setValue(null);
        dpDate.setValue(null);
        spHour.getValueFactory().setValue(19);
        spMinute.getValueFactory().setValue(0);
        tfPrice.clear();
        cbStatus.setValue(ShowtimeStatus.OPEN);
        btnSaveShowtime.setText("Thêm suất");
        btnDeleteShowtime.setDisable(true);
        tableShowtimes.getSelectionModel().clearSelection();
    }

    @FXML
    private void onSaveShowtime() {
        ShowtimeForm f = new ShowtimeForm();
        f.setShowtimeId(editingShowtimeId);
        f.setMovieId(cbMovie.getValue() == null ? null : cbMovie.getValue().id());
        f.setRoomId(cbRoom.getValue() == null ? null : cbRoom.getValue().id());
        f.setStartTime(startTime());
        f.setEndTime(suggestedEnd());
        f.setTicketPrice(tfPrice.getText().isBlank() ? null : new BigDecimal(tfPrice.getText().trim()));
        f.setStatus(cbStatus.getValue() != null ? cbStatus.getValue() : ShowtimeStatus.OPEN);

        if (!validator.mark(validator.validate(f), Map.of(
                "movieId", cbMovie,
                "roomId", cbRoom,
                "startTime", dpDate,
                "ticketPrice", tfPrice
        ))) {
            return;
        }

        fxAsync.runVoid(() -> showtimeFacade.save(f), () -> {
            AlertUtil.info("Đã lưu suất chiếu thành công.");
            onResetShowtime();
            loadShowtimes(paginationShowtimes.getCurrentPageIndex());
        }, formBoxShowtime);
    }

    @FXML
    private void onDeleteShowtime() {
        if (editingShowtimeId == null) return;
        if (!AlertUtil.confirm("Bạn có chắc chắn muốn xoá/huỷ suất chiếu #" + editingShowtimeId + " không?")) {
            return;
        }

        Long id = editingShowtimeId;
        fxAsync.run(() -> showtimeFacade.delete(id), deletedCompletely -> {
            if (Boolean.TRUE.equals(deletedCompletely)) {
                AlertUtil.info("Đã xoá suất chiếu thành công.");
            } else {
                AlertUtil.warn("Suất chiếu đã có vé đặt: Đã huỷ suất chiếu và huỷ các đơn liên quan (BR-09).");
            }
            onResetShowtime();
            loadShowtimes(paginationShowtimes.getCurrentPageIndex());
        }, formBoxShowtime);
    }

    // ==========================================
    // 2. SETUP TAB PHÒNG CHIẾU
    // ==========================================
    private void initRoomTab() {
        colRoomId.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().id()));
        colRoomName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().name()));
        colRoomType.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().type()));
        colRoomRows.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().rows()));
        colRoomSeatsPerRow.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().seatsPerRow()));
        colRoomCapacity.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().capacity()));
        colRoomStatus.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().status()));

        tableRooms.setItems(roomList);
        tableRooms.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> selectRoom(newV));

        cbRoomType.getItems().setAll(RoomType.values());
        cbRoomType.setValue(RoomType.TWO_D);

        cbRoomStatus.getItems().setAll(RoomStatus.values());
        cbRoomStatus.setValue(RoomStatus.ACTIVE);

        spRoomRows.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 26, 10));
        spRoomSeatsPerRow.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30, 12));

        btnDeleteRoom.setDisable(true);
    }

    public void loadRooms() {
        fxAsync.run(roomFacade::findAll, rooms -> {
            roomList.setAll(rooms);
            loadDropdownData();
        }, tableRooms);
    }

    private void selectRoom(RoomRow row) {
        if (row == null) {
            onResetRoom();
            return;
        }
        editingRoomId = row.id();
        lblRoomFormTitle.setText("Sửa phòng chiếu #" + row.id());
        btnSaveRoom.setText("Cập nhật");
        btnDeleteRoom.setDisable(false);

        fxAsync.run(() -> roomFacade.getForm(row.id()), form -> {
            if (form == null) return;
            tfRoomName.setText(form.getRoomName());
            if (form.getRoomType() != null) cbRoomType.setValue(form.getRoomType());
            if (form.getTotalRows() != null) spRoomRows.getValueFactory().setValue(form.getTotalRows());
            if (form.getSeatsPerRow() != null) spRoomSeatsPerRow.getValueFactory().setValue(form.getSeatsPerRow());
            if (form.getStatus() != null) cbRoomStatus.setValue(form.getStatus());
        }, formBoxRoom);
    }

    @FXML
    private void onResetRoom() {
        editingRoomId = null;
        lblRoomFormTitle.setText("Thêm phòng chiếu mới");
        tfRoomName.clear();
        cbRoomType.setValue(RoomType.TWO_D);
        spRoomRows.getValueFactory().setValue(10);
        spRoomSeatsPerRow.getValueFactory().setValue(12);
        cbRoomStatus.setValue(RoomStatus.ACTIVE);
        btnSaveRoom.setText("Thêm phòng");
        btnDeleteRoom.setDisable(true);
        tableRooms.getSelectionModel().clearSelection();
    }

    @FXML
    private void onSaveRoom() {
        RoomForm f = new RoomForm();
        f.setRoomId(editingRoomId);
        f.setRoomName(tfRoomName.getText().trim());
        f.setRoomType(cbRoomType.getValue());
        f.setTotalRows(spRoomRows.getValue());
        f.setSeatsPerRow(spRoomSeatsPerRow.getValue());
        f.setStatus(cbRoomStatus.getValue());

        if (!validator.mark(validator.validate(f), Map.of(
                "roomName", tfRoomName,
                "roomType", cbRoomType,
                "totalRows", spRoomRows,
                "seatsPerRow", spRoomSeatsPerRow,
                "status", cbRoomStatus
        ))) {
            return;
        }

        fxAsync.runVoid(() -> roomFacade.save(f), () -> {
            AlertUtil.info("Đã lưu phòng chiếu thành công.");
            onResetRoom();
            loadRooms();
        }, formBoxRoom);
    }

    @FXML
    private void onDeleteRoom() {
        if (editingRoomId == null) return;
        if (!AlertUtil.confirm("Bạn có chắc chắn muốn xoá phòng chiếu #" + editingRoomId + " không?")) {
            return;
        }

        Long id = editingRoomId;
        fxAsync.run(() -> roomFacade.delete(id), deletedCompletely -> {
            if (Boolean.TRUE.equals(deletedCompletely)) {
                AlertUtil.info("Đã xoá phòng chiếu thành công.");
            } else {
                AlertUtil.warn("Phòng đã có suất chiếu trong hệ thống: Đã chuyển sang trạng thái BẢO TRÌ (MAINTENANCE).");
            }
            onResetRoom();
            loadRooms();
        }, formBoxRoom);
    }
}

