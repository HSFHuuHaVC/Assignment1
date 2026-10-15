package com.fu.cinema.controller;

import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.entity.Movie;
import com.fu.cinema.entity.Showtime;
import com.fu.cinema.service.GenreService;
import com.fu.cinema.service.MovieService;
import com.fu.cinema.service.ReviewService;
import com.fu.cinema.service.ShowtimeService;
import com.fu.cinema.util.PageUtils;
import com.fu.cinema.util.SessionUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final MovieService movieService;
    private final GenreService genreService;
    private final ShowtimeService showtimeService;
    private final ReviewService reviewService;

    @GetMapping({"/", "/movies"})
    public String home(@RequestParam(required = false) String kw,
                       @RequestParam(required = false) Long genreId,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        Page<Movie> movies = movieService.searchForCustomer(kw, genreId, page, 12);
        model.addAttribute("movies", movies);
        model.addAttribute("genres", genreService.findAll());
        model.addAttribute("kw", kw);
        model.addAttribute("genreId", genreId);
        model.addAttribute("pageUrl", PageUtils.currentUrlWithoutPage());
        return "index";
    }

    @GetMapping("/movies/{id}")
    public String movieDetail(@PathVariable Long id,
                              HttpServletRequest request,
                              Model model) {
        model.addAttribute("movie", movieService.getById(id));

        // Suat chieu sap toi, nhom theo ngay (giu thu tu thoi gian)
        Map<LocalDate, List<Showtime>> byDate = showtimeService.findUpcomingByMovie(id).stream()
                .collect(Collectors.groupingBy(s -> s.getStartTime().toLocalDate(), LinkedHashMap::new, Collectors.toList()));
        model.addAttribute("showtimesByDate", byDate);

        model.addAttribute("rating", reviewService.summarize(id));
        model.addAttribute("reviews", reviewService.findByMovie(id));
        SessionUser user = SessionUtils.getCurrentUser(request.getSession(false));
        Long customerId = user == null ? null : user.getCustomerId();
        boolean canReview = reviewService.canReview(customerId, id);
        model.addAttribute("canReview", canReview);
        if (canReview) {
            model.addAttribute("reviewForm", reviewService.getForm(customerId, id));
        }
        return "movie-detail";
    }
}
