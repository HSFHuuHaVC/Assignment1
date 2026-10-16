package com.fu.cinema.service;

import com.fu.cinema.dto.GenreForm;
import com.fu.cinema.entity.Genre;

import java.util.List;

public interface GenreService {

    List<Genre> findAll();

    GenreForm getForm(Long genreId);

    void create(GenreForm form);

    void update(Long genreId, GenreForm form);

    void delete(Long genreId);
}
