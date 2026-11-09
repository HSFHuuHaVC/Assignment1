package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.model.GenreOption;
import com.fu.cinema.desktop.util.FxValidator;
import com.fu.cinema.dto.MovieForm;
import com.fu.cinema.enums.AgeRating;
import com.fu.cinema.enums.MovieStatus;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class MovieDialogController {
    private final FxValidator fxValidator;
    @FXML private VBox root;
    @FXML private Label lblDialogTitle;
    @FXML private TextField txtTitle, txtDirector, txtPosterUrl;
    @FXML private Spinner<Integer> spnDuration;
    @FXML private DatePicker dpReleaseDate;
    @FXML private ComboBox<AgeRating> cmbAgeRating;
    @FXML private ComboBox<GenreOption> cmbGenre;
    @FXML private ComboBox<MovieStatus> cmbStatus;
    @FXML private TextArea txtDescription;
    private Long editingMovieId;

    @FXML private void initialize() {
        // Let MovieForm's @Min(30)/@Max(300) report invalid durations, as in the guide.
        spnDuration.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 120));
        spnDuration.setEditable(true);
        spnDuration.getEditor().setOnAction(e -> commitSpinner());
        spnDuration.focusedProperty().addListener((obs, oldV, focused) -> {
            if (!focused) commitSpinner();
        });
        cmbAgeRating.getItems().setAll(AgeRating.values());
        cmbStatus.getItems().setAll(MovieStatus.values());
        cmbStatus.setConverter(new StringConverter<>() {
            @Override public String toString(MovieStatus status) {
                if (status == null) return "";
                return switch (status) {
                    case SHOWING -> "Đang chiếu";
                    case COMING_SOON -> "Sắp chiếu";
                    case STOPPED -> "Ngừng chiếu";
                };
            }
            @Override public MovieStatus fromString(String string) { return null; }
        });
        cmbGenre.setConverter(new StringConverter<>() {
            @Override public String toString(GenreOption option) {
                return option != null ? option.genreName() : "";
            }
            @Override public GenreOption fromString(String string) { return null; }
        });
    }

    /** The caller loads form/options through FxAsync before opening this UI-only dialog. */
    public Optional<MovieForm> open(Window owner, MovieForm form, List<GenreOption> genres) {
        editingMovieId = form.getMovieId();
        cmbGenre.getItems().setAll(genres);
        fill(form);
        Dialog<MovieForm> dialog = new Dialog<>();
        if (owner != null) {
            dialog.initOwner(owner);
            if (owner.getScene() != null) {
                dialog.getDialogPane().getStylesheets().setAll(owner.getScene().getStylesheets());
            }
        }
        dialog.setTitle(editingMovieId == null ? "Thêm phim mới" : "Chỉnh sửa phim");
        lblDialogTitle.setText(dialog.getTitle());
        dialog.getDialogPane().setContent(root);
        ButtonType save = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(save).setId("btnSave");
        dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setId("btnCancel");
        dialog.getDialogPane().lookupButton(save).getStyleClass().add("primary");
        dialog.getDialogPane().lookupButton(save).addEventFilter(ActionEvent.ACTION, event -> {
            commitSpinner();
            if (!fxValidator.mark(fxValidator.validate(toForm()), fields())) event.consume();
        });
        dialog.setResultConverter(button -> button == save ? toForm() : null);
        return dialog.showAndWait();
    }

    private void fill(MovieForm form) {
        boolean creating = editingMovieId == null;
        txtTitle.setText(form.getTitle());
        txtDirector.setText(form.getDirector());
        spnDuration.getValueFactory().setValue(form.getDuration() == null ? 120 : form.getDuration());
        dpReleaseDate.setValue(creating && form.getReleaseDate() == null ? LocalDate.now() : form.getReleaseDate());
        cmbAgeRating.setValue(creating && form.getAgeRating() == null ? AgeRating.P : form.getAgeRating());
        cmbStatus.setValue(creating && form.getStatus() == null ? MovieStatus.SHOWING : form.getStatus());
        txtPosterUrl.setText(creating && form.getPosterUrl() == null ? "/images/posters/" : form.getPosterUrl());
        txtDescription.setText(form.getDescription());
        cmbGenre.setValue(null);
        if (form.getGenreId() != null) {
            cmbGenre.getItems().stream().filter(g -> form.getGenreId().equals(g.genreId()))
                    .findFirst().ifPresent(cmbGenre::setValue);
        } else if (creating && !cmbGenre.getItems().isEmpty()) {
            cmbGenre.getSelectionModel().selectFirst();
        }
        fxValidator.mark(Map.of(), fields());
    }

    private Map<String, Control> fields() {
        return Map.of("title", txtTitle, "director", txtDirector, "duration", spnDuration,
                "releaseDate", dpReleaseDate, "ageRating", cmbAgeRating, "genreId", cmbGenre,
                "status", cmbStatus, "posterUrl", txtPosterUrl, "description", txtDescription);
    }

    private void commitSpinner() {
        try {
            spnDuration.getValueFactory().setValue(Integer.parseInt(spnDuration.getEditor().getText().trim()));
        } catch (NumberFormatException ignored) {
            spnDuration.getEditor().setText(String.valueOf(spnDuration.getValue()));
        }
    }

    private MovieForm toForm() {
        MovieForm form = new MovieForm();
        form.setMovieId(editingMovieId);
        form.setTitle(txtTitle.getText().trim());
        form.setDirector(txtDirector.getText().trim());
        form.setDuration(spnDuration.getValue());
        form.setReleaseDate(dpReleaseDate.getValue());
        form.setAgeRating(cmbAgeRating.getValue());
        form.setGenreId(cmbGenre.getValue() == null ? null : cmbGenre.getValue().genreId());
        form.setStatus(cmbStatus.getValue());
        form.setPosterUrl(txtPosterUrl.getText().trim());
        form.setDescription(txtDescription.getText().trim());
        return form;
    }
}
