package com.fu.cinema.repository;

import com.fu.cinema.dto.RatingSummary;
import com.fu.cinema.entity.Review;
import com.fu.cinema.entity.ReviewId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, ReviewId> {

    @EntityGraph(attributePaths = "customer")
    List<Review> findByMovie_MovieIdOrderByReviewDateDesc(Long movieId);

    // ===== Q10: điểm trung bình & số lượt đánh giá (interface projection) =====
    @Query("""
            SELECT AVG(r.rating) AS average, COUNT(r) AS total
            FROM Review r WHERE r.movie.movieId = :movieId
            """)
    RatingSummary summarize(@Param("movieId") Long movieId);
}
