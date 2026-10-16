package com.fu.cinema.repository;

import com.fu.cinema.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GenreRepository extends JpaRepository<Genre, Long> {

    boolean existsByGenreNameIgnoreCase(String genreName);

    boolean existsByGenreNameIgnoreCaseAndGenreIdNot(String genreName, Long genreId);

    List<Genre> findAllByOrderByGenreNameAsc();
}
