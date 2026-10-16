package com.fu.cinema.repository;

import com.fu.cinema.entity.Movie;
import com.fu.cinema.enums.MovieStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    // ===== Derived query =====
    boolean existsByGenre_GenreId(Long genreId);

    List<Movie> findByStatusInOrderByReleaseDateDesc(Collection<MovieStatus> statuses);

    long countByStatus(MovieStatus status);

    List<Movie> findByStatusNotOrderByTitleAsc(MovieStatus status);      // dropdown tạo suất chiếu

    // ===== Q2: tìm theo tên + thể loại + danh sách trạng thái (tham số tuỳ chọn), phân trang =====
    @Query(value = """
            SELECT m FROM Movie m JOIN FETCH m.genre g
            WHERE (:kw IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', :kw, '%')))
              AND (:genreId IS NULL OR g.genreId = :genreId)
              AND m.status IN :statuses
            """,
            countQuery = """
            SELECT COUNT(m) FROM Movie m
            WHERE (:kw IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', :kw, '%')))
              AND (:genreId IS NULL OR m.genre.genreId = :genreId)
              AND m.status IN :statuses
            """)
    Page<Movie> search(@Param("kw") String keyword,
                       @Param("genreId") Long genreId,
                       @Param("statuses") Collection<MovieStatus> statuses,
                       Pageable pageable);

    @Query("SELECT m FROM Movie m JOIN FETCH m.genre WHERE m.movieId = :id")
    Optional<Movie> findWithGenreById(@Param("id") Long id);
}
