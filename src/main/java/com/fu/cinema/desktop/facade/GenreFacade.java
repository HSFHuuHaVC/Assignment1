package com.fu.cinema.desktop.facade;

import com.fu.cinema.desktop.model.GenreItem;
import com.fu.cinema.desktop.model.GenreOption;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.dto.GenreForm;
import com.fu.cinema.service.GenreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GenreFacade {
    private final GenreService genreService;
    private final DesktopSession session;

    @Transactional(readOnly = true)
    public List<GenreItem> getAllGenres() {
        return genreService.findAll().stream()
                .map(GenreItem::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GenreOption> getGenreOptions() {
        return genreService.findAll().stream()
                .map(GenreOption::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public GenreForm getGenreForm(Long genreId) {
        return genreService.getForm(genreId);
    }

    @Transactional
    public void createGenre(GenreForm form) {
        session.requireAdmin();
        genreService.create(form);
    }

    @Transactional
    public void updateGenre(Long genreId, GenreForm form) {
        session.requireAdmin();
        genreService.update(genreId, form);
    }

    @Transactional
    public void deleteGenre(Long genreId) {
        session.requireAdmin();
        genreService.delete(genreId);
    }
}
