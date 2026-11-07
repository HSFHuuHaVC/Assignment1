package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.facade.BookingFacade;
import com.fu.cinema.desktop.model.BookingDetail;
import com.fu.cinema.desktop.model.BookingRow;
import com.fu.cinema.desktop.util.AlertUtil;
import com.fu.cinema.desktop.util.FxAsync;
import com.fu.cinema.desktop.util.MoneyUtil;
import com.fu.cinema.enums.BookingStatus;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class BookingController {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DateTimeFormatter SHOWTIME_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM");

    private final BookingFacade facade;
    private final FxAsync fxAsync;

    @FXML private ComboBox<BookingStatus> cbStatus;
    @FXML private DatePicker dpFrom;
    @FXML private DatePicker dpTo;
    @FXML private TextField tfKeyword;
    @FXML private TableView<BookingRow> table;
    @FXML private TableColumn<BookingRow, String> colId;
    @FXML private TableColumn<BookingRow, String> colCustomer;
    @FXML private TableColumn<BookingRow, String> colMovie;
    @FXML private TableColumn<BookingRow, String> colRoom;
    @FXML private TableColumn<BookingRow, String> colShowtime;
    @FXML private TableColumn<BookingRow, String> colSeats;
    @FXML private TableColumn<BookingRow, String> colTotal;
    @FXML private TableColumn<BookingRow, BookingStatus> colStatus;
    @FXML private TableColumn<BookingRow, String> colBookingDate;

    @FXML private Pagination pagination;
    @FXML private Button btnConfirm;
    @FXML private Button btnCancel;
    @FXML private Button btnDetail;

    @FXML
    private void initialize() {
        // Setup bộ lọc trạng thái (cho phép null = Tất cả)
        cbStatus.getItems().add(null);
        cbStatus.getItems().addAll(BookingStatus.values());
        cbStatus.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(BookingStatus s) { return s == null ? "Tất cả trạng thái" : s.name(); }
            @Override public BookingStatus fromString(String string) { return null; }
        });
        cbStatus.getSelectionModel().selectFirst();

        // Setup các cột TableView
        colId.setCellValueFactory(c -> new SimpleStringProperty("#" + c.getValue().id()));
        colCustomer.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().customerName()));
        colMovie.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().movieTitle()));
        colRoom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().roomName()));
        colShowtime.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().startTime() == null ? "" : c.getValue().startTime().format(SHOWTIME_FMT)));
        colSeats.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().seatCount())));
        colTotal.setCellValueFactory(c -> new SimpleStringProperty(MoneyUtil.format(c.getValue().total())));
        colBookingDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().bookingDate() == null ? "" : c.getValue().bookingDate().format(TIME_FMT)));

        colStatus.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().status()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BookingStatus st, boolean empty) {
                super.updateItem(st, empty);
                getStyleClass().removeAll("st-pending", "st-confirmed", "st-cancelled");
                setText(empty || st == null ? null : st.name());
                if (!empty && st != null) {
                    getStyleClass().add("st-" + st.name().toLowerCase());
                }
            }
        });

        // Double-click mở chi tiết
        table.setRowFactory(tv -> {
            TableRow<BookingRow> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    onDetail();
                }
            });
            return row;
        });

        // Binding bật/tắt nút theo trạng thái dòng đang chọn
        ReadOnlyObjectProperty<BookingRow> sel = table.getSelectionModel().selectedItemProperty();
        btnConfirm.disableProperty().bind(Bindings.createBooleanBinding(
                () -> sel.get() == null || sel.get().status() != BookingStatus.PENDING, sel));
        btnCancel.disableProperty().bind(Bindings.createBooleanBinding(
                () -> sel.get() == null || sel.get().status() == BookingStatus.CANCELLED, sel));
        btnDetail.disableProperty().bind(sel.isNull());

        // Phân trang
        pagination.currentPageIndexProperty().addListener((obs, oldIdx, newIdx) -> loadPage(newIdx.intValue()));

        loadPage(0);
    }

    @FXML
    private void onSearch() {
        if (pagination.getCurrentPageIndex() == 0) {
            loadPage(0);
        } else {
            pagination.setCurrentPageIndex(0);
        }
    }

    @FXML
    private void onReset() {
        cbStatus.getSelectionModel().selectFirst();
        dpFrom.setValue(null);
        dpTo.setValue(null);
        tfKeyword.clear();
        onSearch();
    }

    private void loadPage(int page) {
        BookingStatus status = cbStatus.getValue();
        String keyword = tfKeyword.getText();
        fxAsync.run(
                () -> facade.search(status, dpFrom.getValue(), dpTo.getValue(), keyword, page),
                result -> {
                    table.setItems(FXCollections.observableArrayList(result.items()));
                    pagination.setPageCount(Math.max(1, result.totalPages()));
                },
                AlertUtil::showError,
                null
        );
    }

    @FXML
    private void onConfirm() {
        BookingRow r = table.getSelectionModel().getSelectedItem();
        if (r == null) return;
        if (AlertUtil.confirm("Xác nhận đơn #" + r.id() + " của khách " + r.customerName() + "?")) {
            fxAsync.runVoid(
                    () -> facade.confirm(r.id()),
                    () -> loadPage(pagination.getCurrentPageIndex()),
                    null
            );
        }
    }

    @FXML
    private void onCancel() {
        BookingRow r = table.getSelectionModel().getSelectedItem();
        if (r == null) return;
        if (AlertUtil.confirm("Huỷ đơn #" + r.id() + "? Toàn bộ vé trong đơn sẽ bị huỷ.")) {
            fxAsync.runVoid(
                    () -> facade.cancel(r.id()),
                    () -> loadPage(pagination.getCurrentPageIndex()),
                    null
            );
        }
    }

    @FXML
    private void onDetail() {
        BookingRow r = table.getSelectionModel().getSelectedItem();
        if (r == null) return;
        fxAsync.run(
                () -> facade.detail(r.id()),
                this::showDetailDialog,
                null
        );
    }

    private void showDetailDialog(BookingDetail detail) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Chi tiết đơn đặt vé #" + detail.id());
        dialog.setHeaderText("Thông tin đơn #" + detail.id() + " — " + detail.status());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        grid.setPadding(new Insets(16));

        grid.add(new Label("Khách hàng:"), 0, 0);
        grid.add(new Label(detail.customerName() + " (" + detail.mobile() + ")"), 1, 0);

        grid.add(new Label("Phim:"), 0, 1);
        grid.add(new Label(detail.movieTitle()), 1, 1);

        grid.add(new Label("Phòng chiếu:"), 0, 2);
        grid.add(new Label(detail.roomName()), 1, 2);

        grid.add(new Label("Suất chiếu:"), 0, 3);
        grid.add(new Label(detail.startTime().format(TIME_FMT)), 1, 3);

        grid.add(new Label("Ghế đã đặt:"), 0, 4);
        grid.add(new Label(String.join(", ", detail.seats())), 1, 4);

        grid.add(new Label("Tổng tiền:"), 0, 5);
        Label lblTotal = new Label(MoneyUtil.format(detail.total()));
        lblTotal.setStyle("-fx-font-weight: bold; -fx-text-fill: #be123c;");
        grid.add(lblTotal, 1, 5);

        grid.add(new Label("Thời điểm đặt:"), 0, 6);
        grid.add(new Label(detail.bookingDate().format(TIME_FMT)), 1, 6);

        dialog.getDialogPane().setContent(grid);
        dialog.showAndWait();
    }
}
