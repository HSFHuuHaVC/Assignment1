package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.facade.MovieFacade;
import com.fu.cinema.desktop.model.GenreOption;
import com.fu.cinema.desktop.model.MovieItem;
import com.fu.cinema.desktop.model.PageResult;
import com.fu.cinema.desktop.navigation.SceneManager;
import com.fu.cinema.desktop.navigation.View;
import com.fu.cinema.desktop.util.AlertUtil;
import com.fu.cinema.desktop.util.FxAsync;
import com.fu.cinema.dto.MovieForm;
import com.fu.cinema.enums.MovieStatus;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.util.Duration;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class MovieController {
    private static final int PAGE_SIZE = 10;
    private static final PseudoClass STOPPED_CLASS = PseudoClass.getPseudoClass("stopped");

    private final MovieFacade movieFacade;
    private final FxAsync fxAsync;
    private final SceneManager sceneManager;

    @FXML private TextField txtKeyword;
    @FXML private ComboBox<GenreOption> cmbGenre;
    @FXML private ComboBox<MovieStatusOption> cmbStatus;
    @FXML private ComboBox<SortOption> cmbSort;
    @FXML private Button btnAdd;
    @FXML private Button btnResetFilter;
    @FXML private Label lblSummary;

    @FXML private TableView<MovieItem> tableMovies;
    @FXML private TableColumn<MovieItem, Long> colId;
    @FXML private TableColumn<MovieItem, String> colTitle;
    @FXML private TableColumn<MovieItem, String> colGenre;
    @FXML private TableColumn<MovieItem, String> colDuration;
    @FXML private TableColumn<MovieItem, String> colReleaseDate;
    @FXML private TableColumn<MovieItem, String> colAgeRating;
    @FXML private TableColumn<MovieItem, String> colStatus;

    @FXML private Pagination pagination;

    private final ObservableList<MovieItem> movieList = FXCollections.observableArrayList();
    private final AtomicLong searchSequence = new AtomicLong(0);
    private final PauseTransition debounce = new PauseTransition(Duration.millis(300));
    private boolean updatingPagination = false;
    private boolean initializing = true;

    @FXML
    private void initialize() {
        initializing = true;
        setupColumns();
        setupRowFactory();
        setupFilters();
        setupDebounce();
        setupPagination();

        loadGenreOptions();
        initializing = false;
        loadPage(0);
    }

    private void setupColumns() {
        colId.setCellValueFactory(cell -> new SimpleLongProperty(cell.getValue().movieId()).asObject());
        colTitle.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().title()));
        colGenre.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().genreName()));
        colDuration.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().formattedDuration()));
        colReleaseDate.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().formattedReleaseDate()));
        colAgeRating.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().ageRating() != null ? cell.getValue().ageRating().name() : ""
        ));
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().formattedStatus()));

        // Status badge cell factory
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    MovieItem movie = getTableRow() != null ? getTableRow().getItem() : null;
                    Label badge = new Label(item);
                    badge.getStyleClass().add("badge-status");
                    if (movie != null && movie.status() != null) {
                        switch (movie.status()) {
                            case SHOWING -> badge.getStyleClass().add("badge-showing");
                            case COMING_SOON -> badge.getStyleClass().add("badge-coming");
                            case STOPPED -> badge.getStyleClass().add("badge-stopped");
                        }
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        tableMovies.setItems(movieList);
    }

    private void setupRowFactory() {
        tableMovies.setRowFactory(tv -> {
            TableRow<MovieItem> row = new TableRow<>();

            // Stopped pseudo-class
            row.itemProperty().addListener((obs, oldVal, newVal) -> {
                boolean isStopped = newVal != null && newVal.status() == MovieStatus.STOPPED;
                row.pseudoClassStateChanged(STOPPED_CLASS, isStopped);
            });

            // Context Menu & Double click
            ContextMenu contextMenu = new ContextMenu();
            MenuItem editItem = new MenuItem("Chỉnh sửa");
            editItem.setOnAction(e -> {
                MovieItem item = row.getItem();
                if (item != null) onEditMovie(item);
            });

            MenuItem deleteItem = new MenuItem("Xóa");
            deleteItem.setOnAction(e -> {
                MovieItem item = row.getItem();
                if (item != null) onDeleteMovie(item);
            });
            contextMenu.getItems().addAll(editItem, deleteItem);

            row.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(contextMenu)
            );

            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && !row.isEmpty()) {
                    onEditMovie(row.getItem());
                }
            });

            return row;
        });
    }

    private void setupFilters() {
        // Status filter
        cmbStatus.getItems().setAll(
                new MovieStatusOption(null, "Tất cả trạng thái"),
                new MovieStatusOption(MovieStatus.SHOWING, "Đang chiếu"),
                new MovieStatusOption(MovieStatus.COMING_SOON, "Sắp chiếu"),
                new MovieStatusOption(MovieStatus.STOPPED, "Ngừng chiếu")
        );
        cmbStatus.getSelectionModel().selectFirst();
        cmbStatus.valueProperty().addListener((obs, o, n) -> {
            if (!initializing) resetPageAndLoad();
        });

        // Sort filter
        cmbSort.getItems().setAll(
                new SortOption("releaseDate", "Ngày khởi chiếu (mới nhất)"),
                new SortOption("title", "Tên phim (A-Z)"),
                new SortOption("duration", "Thời lượng (giảm dần)")
        );
        cmbSort.getSelectionModel().selectFirst();
        cmbSort.valueProperty().addListener((obs, o, n) -> {
            if (!initializing) resetPageAndLoad();
        });

        // Genre combobox converter & listener
        cmbGenre.setConverter(new StringConverter<>() {
            @Override
            public String toString(GenreOption option) {
                return option != null ? option.genreName() : "Tất cả thể loại";
            }
            @Override
            public GenreOption fromString(String string) { return null; }
        });
        cmbGenre.valueProperty().addListener((obs, o, n) -> {
            if (!initializing) resetPageAndLoad();
        });
    }

    private void setupDebounce() {
        debounce.setOnFinished(event -> resetPageAndLoad());
        txtKeyword.textProperty().addListener((obs, oldText, newText) -> debounce.playFromStart());
    }

    private void setupPagination() {
        pagination.setPageFactory(pageIndex -> {
            if (!updatingPagination) {
                loadPage(pageIndex);
            }
            return new Label(); // Dummy control as TableView is outside Pagination
        });
    }

    private void loadGenreOptions() {
        fxAsync.run(
                movieFacade::getGenreOptions,
                options -> {
                    ObservableList<GenreOption> items = FXCollections.observableArrayList();
                    items.add(new GenreOption(null, "Tất cả thể loại"));
                    items.addAll(options);
                    cmbGenre.setItems(items);
                    cmbGenre.getSelectionModel().selectFirst();
                }
        );
    }

    private void resetPageAndLoad() {
        updatingPagination = true;
        pagination.setCurrentPageIndex(0);
        updatingPagination = false;
        loadPage(0);
    }

    public void loadPage(int pageIndex) {
        String kw = txtKeyword.getText();
        GenreOption genreOpt = cmbGenre.getValue();
        Long genreId = (genreOpt != null && genreOpt.genreId() != null) ? genreOpt.genreId() : null;

        MovieStatusOption statusOpt = cmbStatus.getValue();
        MovieStatus status = (statusOpt != null) ? statusOpt.status() : null;

        SortOption sortOpt = cmbSort.getValue();
        String sort = (sortOpt != null) ? sortOpt.code() : "releaseDate";

        long currentSeq = searchSequence.incrementAndGet();

        fxAsync.run(
                () -> movieFacade.searchMovies(kw, genreId, status, pageIndex, PAGE_SIZE, sort),
                result -> {
                    // Out-of-order response check
                    if (currentSeq == searchSequence.get()) {
                        renderPageResult(result);
                    }
                },
                null
        );
    }

    private void renderPageResult(PageResult<MovieItem> pageResult) {
        movieList.setAll(pageResult.items());
        updatingPagination = true;
        pagination.setPageCount(Math.max(1, pageResult.totalPages()));
        pagination.setCurrentPageIndex(pageResult.page());
        updatingPagination = false;

        if (lblSummary != null) {
            lblSummary.setText("Tổng cộng: " + pageResult.totalElements() + " phim | Trang "
                    + (pageResult.page() + 1) + "/" + Math.max(1, pageResult.totalPages()));
        }
    }

    @FXML
    private void onResetFilter() {
        txtKeyword.clear();
        cmbGenre.getSelectionModel().selectFirst();
        cmbStatus.getSelectionModel().selectFirst();
        cmbSort.getSelectionModel().selectFirst();
        resetPageAndLoad();
    }

    @FXML
    public void onAddMovie() {
        openMovieDialog(null);
    }

    public void onEditMovie(MovieItem item) {
        if (item == null) return;
        openMovieDialog(item.movieId());
    }

    public void onDeleteMovie(MovieItem item) {
        if (item == null) return;

        boolean confirmed = AlertUtil.confirm("Bạn có chắc chắn muốn xóa phim \"" + item.title() + "\"?");
        if (!confirmed) return;

        fxAsync.run(
                () -> movieFacade.deleteMovie(item.movieId()),
                hardDeleted -> {
                    if (hardDeleted) {
                        AlertUtil.info("Đã xóa vĩnh viễn phim \"" + item.title() + "\" thành công!");
                    } else {
                        AlertUtil.warn("Phim \"" + item.title() + "\" đã có suất chiếu nên được chuyển sang trạng thái Ngừng chiếu (STOPPED).");
                    }
                    loadPage(pagination.getCurrentPageIndex());
                },
                null
        );
    }

    private void openMovieDialog(Long movieId) {
        var owner = tableMovies.getScene().getWindow();
        fxAsync.run(() -> new MovieDialogData(movieFacade.getGenreOptions(),
                movieId == null ? new MovieForm() : movieFacade.getMovieForm(movieId)),
                data -> showMovieDialog(owner, data), tableMovies);
    }

    private void showMovieDialog(javafx.stage.Window owner, MovieDialogData data) {
        try {
            SceneManager.Loaded<MovieDialogController> loaded = sceneManager.load(View.MOVIE_DIALOG);
            loaded.controller().open(owner, data.form(), data.genres()).ifPresent(form ->
                    fxAsync.runVoid(() -> {
                        if (form.getMovieId() == null) movieFacade.createMovie(form);
                        else movieFacade.updateMovie(form.getMovieId(), form);
                    }, () -> {
                        AlertUtil.info("Đã lưu phim thành công!");
                        loadPage(pagination.getCurrentPageIndex());
                    }, tableMovies));
        } catch (Exception ex) {
            AlertUtil.showError(ex);
        }
    }

    private record MovieDialogData(List<GenreOption> genres, MovieForm form) { }

    public record MovieStatusOption(MovieStatus status, String label) {
        @Override public String toString() { return label; }
    }

    public record SortOption(String code, String label) {
        @Override public String toString() { return label; }
    }
}
