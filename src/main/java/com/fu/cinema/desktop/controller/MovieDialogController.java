package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.facade.MovieFacade;
import com.fu.cinema.desktop.model.GenreOption;
import com.fu.cinema.desktop.util.AlertUtil;
import com.fu.cinema.desktop.util.FxAsync;
import com.fu.cinema.desktop.util.FxValidator;
import com.fu.cinema.dto.MovieForm;
import com.fu.cinema.enums.AgeRating;
import com.fu.cinema.enums.MovieStatus;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class MovieDialogController {
    private final MovieFacade movieFacade;
    private final FxValidator fxValidator;
    private final FxAsync fxAsync;

    @FXML private Label lblDialogTitle;
    @FXML private TextField txtTitle;
    @FXML private TextField txtDirector;
    @FXML private Spinner<Integer> spnDuration;
    @FXML private DatePicker dpReleaseDate;
    @FXML private ComboBox<AgeRating> cmbAgeRating;
    @FXML private ComboBox<GenreOption> cmbGenre;
    @FXML private ComboBox<MovieStatus> cmbStatus;
    @FXML private TextField txtPosterUrl;
    @FXML private TextArea txtDescription;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;

    private Stage dialogStage;
    private Long editingMovieId;
    @Getter private boolean saved = false;

    @FXML
    private void initialize() {
        // Spinner 30 - 300 minutes, default 120, step 5
        SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(30, 300, 120, 5);
        spnDuration.setValueFactory(valueFactory);
        spnDuration.setEditable(true);

        // Allow direct manual text typing in Spinner editor
        spnDuration.getEditor().setOnAction(e -> commitSpinner());
        spnDuration.focusedProperty().addListener((obs, oldV, focused) -> {
            if (!focused) commitSpinner();
        });

        // ComboBox values
        cmbAgeRating.setItems(FXCollections.observableArrayList(AgeRating.values()));
        cmbStatus.setItems(FXCollections.observableArrayList(MovieStatus.values()));

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

    public void init(Stage stage, Long movieId) {
        this.dialogStage = stage;
        this.editingMovieId = movieId;
        this.saved = false;

        List<GenreOption> genres = movieFacade.getGenreOptions();
        cmbGenre.setItems(FXCollections.observableArrayList(genres));

        if (movieId == null) {
            lblDialogTitle.setText("Thêm phim mới");
            btnSave.setText("Thêm mới");
            dpReleaseDate.setValue(LocalDate.now());
            cmbAgeRating.setValue(AgeRating.P);
            cmbStatus.setValue(MovieStatus.SHOWING);
            if (!genres.isEmpty()) cmbGenre.setValue(genres.getFirst());
            txtPosterUrl.setText("/images/posters/");
        } else {
            lblDialogTitle.setText("Chỉnh sửa phim");
            btnSave.setText("Lưu thay đổi");
            loadExistingMovie(movieId);
        }
    }

    private void loadExistingMovie(Long movieId) {
        try {
            MovieForm form = movieFacade.getMovieForm(movieId);
            txtTitle.setText(form.getTitle());
            txtDirector.setText(form.getDirector());
            if (form.getDuration() != null) {
                spnDuration.getValueFactory().setValue(Math.clamp(form.getDuration(), 30, 300));
            }
            dpReleaseDate.setValue(form.getReleaseDate());
            cmbAgeRating.setValue(form.getAgeRating());
            cmbStatus.setValue(form.getStatus());
            txtPosterUrl.setText(form.getPosterUrl());
            txtDescription.setText(form.getDescription());

            if (form.getGenreId() != null) {
                cmbGenre.getItems().stream()
                        .filter(g -> form.getGenreId().equals(g.genreId()))
                        .findFirst()
                        .ifPresent(cmbGenre::setValue);
            }
        } catch (Exception ex) {
            AlertUtil.showError(ex);
            dialogStage.close();
        }
    }

    private void commitSpinner() {
        try {
            spnDuration.commitValue();
        } catch (Exception ex) {
            try {
                String text = spnDuration.getEditor().getText().trim();
                int parsed = Integer.parseInt(text);
                spnDuration.getValueFactory().setValue(parsed);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @FXML
    private void onSave() {
        commitSpinner();

        MovieForm form = new MovieForm();
        form.setMovieId(editingMovieId);
        form.setTitle(txtTitle.getText() != null ? txtTitle.getText().trim() : "");
        form.setDirector(txtDirector.getText() != null ? txtDirector.getText().trim() : "");
        form.setDuration(spnDuration.getValue());
        form.setReleaseDate(dpReleaseDate.getValue());
        form.setAgeRating(cmbAgeRating.getValue());
        form.setGenreId(cmbGenre.getValue() != null ? cmbGenre.getValue().genreId() : null);
        form.setStatus(cmbStatus.getValue());
        form.setPosterUrl(txtPosterUrl.getText() != null ? txtPosterUrl.getText().trim() : "");
        form.setDescription(txtDescription.getText() != null ? txtDescription.getText().trim() : "");

        Map<String, String> errors = fxValidator.validate(form);

        Map<String, Control> fieldMap = new HashMap<>();
        fieldMap.put("title", txtTitle);
        fieldMap.put("director", txtDirector);
        fieldMap.put("duration", spnDuration);
        fieldMap.put("releaseDate", dpReleaseDate);
        fieldMap.put("ageRating", cmbAgeRating);
        fieldMap.put("genreId", cmbGenre);
        fieldMap.put("status", cmbStatus);
        fieldMap.put("posterUrl", txtPosterUrl);
        fieldMap.put("description", txtDescription);

        boolean valid = fxValidator.mark(errors, fieldMap);
        if (!valid) {
            // Keep dialog open on validation error (TC-D09)
            AlertUtil.warn("Vui lòng kiểm tra và sửa các thông tin chưa hợp lệ.");
            return;
        }

        fxValidator.mark(Map.of(), fieldMap);

        if (editingMovieId == null) {
            fxAsync.runVoid(
                    () -> movieFacade.createMovie(form),
                    () -> {
                        saved = true;
                        AlertUtil.info("Thêm phim mới thành công!");
                        dialogStage.close();
                    },
                    btnSave
            );
        } else {
            fxAsync.runVoid(
                    () -> movieFacade.updateMovie(editingMovieId, form),
                    () -> {
                        saved = true;
                        AlertUtil.info("Cập nhật thông tin phim thành công!");
                        dialogStage.close();
                    },
                    btnSave
            );
        }
    }

    @FXML
    private void onCancel() {
        saved = false;
        dialogStage.close();
    }
}
