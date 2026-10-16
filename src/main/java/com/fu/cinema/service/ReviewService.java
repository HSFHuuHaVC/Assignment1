package com.fu.cinema.service;

import com.fu.cinema.dto.RatingSummary;
import com.fu.cinema.dto.ReviewForm;
import com.fu.cinema.entity.Review;

import java.util.List;

public interface ReviewService {

    List<Review> findByMovie(Long movieId);

    RatingSummary summarize(Long movieId);

    boolean canReview(Long customerId, Long movieId);

    ReviewForm getForm(Long customerId, Long movieId);

    void save(Long customerId, Long movieId, ReviewForm form);
}
