package com.fu.cinema.desktop;

import com.fu.cinema.desktop.controller.DashboardController;
import com.fu.cinema.desktop.facade.DashboardFacade;
import com.fu.cinema.desktop.model.RoomCount;
import com.fu.cinema.desktop.navigation.SceneManager;
import com.fu.cinema.desktop.navigation.View;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.dto.DashboardStats;
import com.fu.cinema.dto.RoomForm;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.enums.Role;
import com.fu.cinema.enums.RoomStatus;
import com.fu.cinema.enums.RoomType;
import com.fu.cinema.service.MovieService;
import com.fu.cinema.service.RoomService;
import com.fu.cinema.service.ShowtimeService;
import com.fu.cinema.util.SessionUtils;
import javafx.animation.Animation;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/** Opt-in local SQL Server test. Creates and removes only a uniquely named test room and its showtimes. */
@EnabledIfSystemProperty(named = "dashboard.sql.tests", matches = "true")
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.show-sql=false",
        "logging.level.org.hibernate.SQL=WARN"
})
@ActiveProfiles("local")
class DashboardSqlServerUiTests {
    @Autowired private WebApplicationContext context;
    @Autowired private DashboardFacade facade;
    @Autowired private SceneManager scenes;
    @Autowired private DesktopSession session;
    @Autowired private RoomService rooms;
    @Autowired private MovieService movies;
    @Autowired private ShowtimeService showtimes;
    @Autowired private JdbcTemplate jdbc;

    @BeforeAll
    static void startToolkit() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        Platform.startup(() -> { Platform.setImplicitExit(false); ready.countDown(); });
        assertTrue(ready.await(10, TimeUnit.SECONDS));
    }

    @AfterAll
    static void stopToolkit() {
        Platform.exit();
    }

    @Test
    void webCreationRefreshAndRealSixtySecondTimerUseTheSameSqlServerDatabase() throws Exception {
        try (var connection = jdbc.getDataSource().getConnection()) {
            assertTrue(connection.getMetaData().getDatabaseProductName().contains("Microsoft SQL Server"),
                    "Use a SQL Server datasource for this opt-in test");
        }
        MockMvc web = webAppContextSetup(context).apply(springSecurity()).build();
        MockHttpSession admin = new MockHttpSession();
        SessionUser user = new SessionUser(1L, null, "TODO37 test ADMIN", Role.ADMIN);
        admin.setAttribute(SessionUtils.CURRENT_USER, user);

        String roomName = "TODO37 test " + UUID.randomUUID().toString().substring(0, 8);
        RoomForm room = new RoomForm();
        room.setRoomName(roomName);
        room.setRoomType(RoomType.TWO_D);
        room.setTotalRows(1);
        room.setSeatsPerRow(1);
        room.setStatus(RoomStatus.ACTIVE);
        Long movieId = movies.findSchedulable().getFirst().getMovieId();
        LocalDate today = jdbc.queryForObject("SELECT CAST(GETDATE() AS DATE)", LocalDate.class);
        rooms.create(room);
        Long roomId = rooms.findAll().stream().filter(r -> r.getRoomName().equals(roomName))
                .findFirst().orElseThrow().getRoomId();
        Scene scene = null;
        try {
            scene = onFx(() -> {
                session.login(user);
                Scene dashboard = new Scene(scenes.load(View.MAIN).root(), 1200, 720);
                dashboard.getStylesheets().add(getClass().getResource("/desktop/css/desktop.css").toExternalForm());
                return dashboard;
            });
            Scene dashboardScene = scene;
            await(() -> dashboardScene.lookup("#lblUpdated") instanceof Label status
                    && status.getText().startsWith("Cập nhật lúc"));
            assertSameWebStatistics(web, admin);
            createShowtime(web, admin, movieId, roomId, today, "01:00");
            onFx(() -> { ((Button) dashboardScene.lookup("#btnRefresh")).fire(); return null; });
            await(() -> chartCount(dashboardScene, roomName) == 1);
            assertSameWebStatistics(web, admin);
            onFx(() -> { snapshot(dashboardScene, "desktop-dashboard.png"); return null; });

            createShowtime(web, admin, movieId, roomId, today, "12:00");
            // Wait for the production 60-second Timeline; do not accelerate it or press Refresh.
            await(() -> chartCount(dashboardScene, roomName) == 2);
            assertSameWebStatistics(web, admin);
            onFx(() -> { snapshot(dashboardScene, "desktop-dashboard-auto.png"); return null; });

            onFx(() -> {
                var controllerField = DashboardController.class.getDeclaredField("autoRefresh");
                controllerField.setAccessible(true);
                var loaded = scenes.<DashboardController>load(View.DASHBOARD);
                dashboardScene.setRoot(loaded.root());
                Timeline timer = (Timeline) controllerField.get(loaded.controller());
                assertEquals(60000, timer.getKeyFrames().getFirst().getTime().toMillis());
                assertEquals(Animation.Status.RUNNING, timer.getStatus());
                dashboardScene.setRoot(new StackPane());
                assertEquals(Animation.Status.STOPPED, timer.getStatus());
                dashboardScene.setRoot(loaded.root());
                assertEquals(Animation.Status.RUNNING, timer.getStatus());
                return null;
            });
            await(() -> chartCount(dashboardScene, roomName) == 2);
        } finally {
            if (scene != null) {
                Scene previous = scene;
                onFx(() -> { previous.setRoot(new StackPane()); session.logout(); return null; });
            }
            showtimes.search(movieId, roomId, today, 0, 20).getContent()
                    .forEach(s -> assertTrue(showtimes.delete(s.getShowtimeId())));
            assertTrue(rooms.delete(roomId), "Temporary room must be removed after its showtimes");
        }
    }

    private void assertSameWebStatistics(MockMvc web, MockHttpSession admin) throws Exception {
        var result = web.perform(get("/admin").session(admin)).andExpect(status().isOk()).andReturn();
        DashboardStats stats = (DashboardStats) result.getModelAndView().getModel().get("stats");
        var desktop = facade.load();
        assertEquals(stats.getShowingMovies(), desktop.showingMovies());
        assertEquals(stats.getCustomers(), desktop.customers());
        assertEquals(stats.getPendingBookings(), desktop.pendingBookings());
        assertEquals(stats.getTodayShowtimesByRoom().stream()
                .map(r -> new RoomCount(r.getRoomName(), r.getShowtimeCount())).toList(), desktop.todayByRoom());
    }

    private void createShowtime(MockMvc web, MockHttpSession admin, Long movieId, Long roomId,
                                LocalDate today, String time) throws Exception {
        web.perform(post("/admin/showtimes/new").session(admin).with(csrf())
                        .param("movieId", movieId.toString()).param("roomId", roomId.toString())
                        .param("startTime", today + "T" + time).param("ticketPrice", "75000")
                        .param("status", "OPEN"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/showtimes"))
                .andExpect(flash().attribute("success", "Đã thêm suất chiếu"));
    }

    private static long chartCount(Scene scene, String name) {
        @SuppressWarnings("unchecked")
        BarChart<String, Number> chart = (BarChart<String, Number>) scene.lookup("#chartRooms");
        if (chart == null || chart.getData().isEmpty()) return -1;
        return chart.getData().getFirst().getData().stream().filter(d -> d.getXValue().equals(name))
                .mapToLong(d -> d.getYValue().longValue()).findFirst().orElse(-1);
    }

    private static void await(Callable<Boolean> condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(75);
        while (System.nanoTime() < deadline) {
            if (onFx(condition)) return;
            Thread.sleep(100);
        }
        fail("Dashboard state did not update within 75 seconds");
    }

    private static void snapshot(Scene scene, String name) throws Exception {
        scene.getRoot().applyCss();
        scene.getRoot().layout();
        WritableImage image = scene.getRoot().snapshot(null, null);
        int width = (int) image.getWidth(), height = (int) image.getHeight();
        BufferedImage png = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = new int[width * height];
        image.getPixelReader().getPixels(0, 0, width, height,
                javafx.scene.image.PixelFormat.getIntArgbInstance(), pixels, 0, width);
        png.setRGB(0, 0, width, height, pixels, 0, width);
        Path path = Path.of("docs", "images", name);
        Files.createDirectories(path.getParent());
        ImageIO.write(png, "png", path.toFile());
    }

    private static <T> T onFx(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(15, TimeUnit.SECONDS);
    }
}
