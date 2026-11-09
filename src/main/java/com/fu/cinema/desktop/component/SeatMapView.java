package com.fu.cinema.desktop.component;

import com.fu.cinema.desktop.model.SeatMapData;
import com.fu.cinema.desktop.util.AlertUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class SeatMapView extends VBox {

    public static final int MAX_SEATS = 8;                                       // BR-10
    private static final PseudoClass SOLD = PseudoClass.getPseudoClass("sold");

    private final GridPane grid = new GridPane();
    private final ObservableList<String> selected = FXCollections.observableArrayList();
    private final ObservableList<String> selectedView = FXCollections.unmodifiableObservableList(selected);

    public SeatMapView() {                                   // constructor rỗng → dùng được trong FXML
        getStyleClass().add("seat-map");
        setSpacing(12);
        setAlignment(Pos.TOP_CENTER);
        Label screen = new Label("MÀN HÌNH");
        screen.getStyleClass().add("screen");
        screen.setMaxWidth(Double.MAX_VALUE);
        grid.setHgap(4);
        grid.setVgap(4);
        grid.setAlignment(Pos.CENTER);
        getChildren().addAll(screen, grid, legend());
    }

    /** Danh sách ghế đang chọn — màn hình khác chỉ được ĐỌC/bind. */
    public ObservableList<String> getSelectedSeats() { return selectedView; }

    public void setData(SeatMapData data) {
        selected.clear();
        grid.getChildren().clear();
        if (data == null) return;
        for (int r = 0; r < data.rowLabels().size(); r++) {
            String row = data.rowLabels().get(r);
            Label rowLabel = new Label(row);
            rowLabel.getStyleClass().add("row-label");
            grid.add(rowLabel, 0, r);
            for (int c = 1; c <= data.seatsPerRow(); c++) {
                String code = row + c;                                          // A1, B12…
                ToggleButton seat = new ToggleButton(String.valueOf(c));
                seat.getStyleClass().add("seat");
                seat.setTooltip(new Tooltip(code));
                boolean sold = data.soldSeats().contains(code);
                seat.pseudoClassStateChanged(SOLD, sold);
                seat.setDisable(sold);
                seat.selectedProperty().addListener((obs, was, is) -> onToggle(seat, code, is));
                grid.add(seat, c, r);
            }
        }
    }

    private void onToggle(ToggleButton seat, String code, boolean isSelected) {
        if (!isSelected) {
            selected.remove(code);
            return;
        }
        if (selected.size() >= MAX_SEATS) {
            seat.setSelected(false);                                            // listener chạy lại với false → remove (không có) → OK
            Platform.runLater(() -> AlertUtil.warn("Mỗi đơn tối đa " + MAX_SEATS + " ghế"));
            return;
        }
        selected.add(code);
    }

    private HBox legend() {
        HBox box = new HBox(16, legendItem("Trống", "legend-free"),
                legendItem("Đang chọn", "legend-selected"), legendItem("Đã bán", "legend-sold"));
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private Label legendItem(String text, String styleClass) {
        Region swatch = new Region();
        swatch.getStyleClass().addAll("legend-swatch", styleClass);
        Label l = new Label(text, swatch);
        return l;
    }
}

