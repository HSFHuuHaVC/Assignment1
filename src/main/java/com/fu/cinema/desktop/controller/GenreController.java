package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.facade.GenreFacade;
import com.fu.cinema.desktop.model.GenreItem;
import com.fu.cinema.desktop.util.AlertUtil;
import com.fu.cinema.desktop.util.FxAsync;
import com.fu.cinema.desktop.util.FxValidator;
import com.fu.cinema.dto.GenreForm;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class GenreController {
    private final GenreFacade genreFacade;
    private final FxAsync fxAsync;
    private final FxValidator fxValidator;

    @FXML private TableView<GenreItem> tableGenres;
    @FXML private TableColumn<GenreItem, Long> colId;
    @FXML private TableColumn<GenreItem, String> colName;
    @FXML private TableColumn<GenreItem, String> colDescription;

    @FXML private Label lblFormTitle;
    @FXML private TextField txtName;
    @FXML private TextArea txtDescription;
    @FXML private Button btnSave;
    @FXML private Button btnDelete;
    @FXML private Button btnReset;
    @FXML private Label lblCount;

    private final ObservableList<GenreItem> genreList = FXCollections.observableArrayList();
    private GenreItem selectedGenre;

    @FXML
    private void initialize() {
        colId.setCellValueFactory(cell -> new SimpleLongProperty(cell.getValue().genreId()).asObject());
        colName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().genreName()));
        colDescription.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().description()));

        tableGenres.setItems(genreList);
        tableGenres.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> selectGenre(newVal));

        btnDelete.setDisable(true);
        loadGenres();
    }

    public void loadGenres() {
        fxAsync.run(
                genreFacade::getAllGenres,
                this::populateGenres,
                null
        );
    }

    private void populateGenres(List<GenreItem> items) {
        genreList.setAll(items);
        if (lblCount != null) {
            lblCount.setText("Tổng cộng: " + items.size() + " thể loại");
        }
        if (selectedGenre != null) {
            items.stream()
                    .filter(g -> g.genreId().equals(selectedGenre.genreId()))
                    .findFirst()
                    .ifPresent(tableGenres.getSelectionModel()::select);
        }
    }

    private void selectGenre(GenreItem item) {
        selectedGenre = item;
        clearValidationMarks();
        if (item != null) {
            lblFormTitle.setText("Chỉnh sửa thể loại");
            txtName.setText(item.genreName());
            txtDescription.setText(item.description());
            btnSave.setText("Cập nhật");
            btnDelete.setDisable(false);
        } else {
            lblFormTitle.setText("Thêm thể loại mới");
            txtName.clear();
            txtDescription.clear();
            btnSave.setText("Thêm thể loại");
            btnDelete.setDisable(true);
        }
    }

    @FXML
    private void onSave() {
        String name = txtName.getText();
        String desc = txtDescription.getText();

        GenreForm form = new GenreForm();
        form.setGenreName(name);
        form.setDescription(desc);
        if (selectedGenre != null) {
            form.setGenreId(selectedGenre.genreId());
        }

        Map<String, String> errors = fxValidator.validate(form);
        boolean valid = fxValidator.mark(errors, Map.of(
                "genreName", txtName,
                "description", txtDescription
        ));

        if (!valid) {
            return;
        }

        clearValidationMarks();
        if (selectedGenre == null) {
            fxAsync.runVoid(
                    () -> genreFacade.createGenre(form),
                    () -> {
                        AlertUtil.info("Thêm thể loại mới thành công!");
                        onReset();
                        loadGenres();
                    },
                    btnSave
            );
        } else {
            Long id = selectedGenre.genreId();
            fxAsync.runVoid(
                    () -> genreFacade.updateGenre(id, form),
                    () -> {
                        AlertUtil.info("Cập nhật thể loại thành công!");
                        onReset();
                        loadGenres();
                    },
                    btnSave
            );
        }
    }

    @FXML
    private void onDelete() {
        if (selectedGenre == null) return;

        GenreItem target = selectedGenre;
        boolean confirmed = AlertUtil.confirm("Bạn có chắc chắn muốn xóa thể loại \"" + target.genreName() + "\"?");
        if (!confirmed) return;

        fxAsync.runVoid(
                () -> genreFacade.deleteGenre(target.genreId()),
                () -> {
                    AlertUtil.info("Đã xóa thể loại \"" + target.genreName() + "\" thành công!");
                    onReset();
                    loadGenres();
                },
                btnDelete
        );
    }

    @FXML
    private void onReset() {
        tableGenres.getSelectionModel().clearSelection();
        selectGenre(null);
    }

    private void clearValidationMarks() {
        fxValidator.mark(Map.of(), Map.of(
                "genreName", txtName,
                "description", txtDescription
        ));
    }
}
