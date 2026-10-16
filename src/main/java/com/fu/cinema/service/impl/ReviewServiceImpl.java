package com.fu.cinema.service.impl;

import com.fu.cinema.dto.RatingSummary;
import com.fu.cinema.dto.ReviewForm;
import com.fu.cinema.entity.Review;
import com.fu.cinema.entity.ReviewId;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.repository.BookingRepository;
import com.fu.cinema.repository.CustomerRepository;
import com.fu.cinema.repository.MovieRepository;
import com.fu.cinema.repository.ReviewRepository;
import com.fu.cinema.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final MovieRepository movieRepository;

    @Override
    public List<Review> findByMovie(Long movieId) {
        return reviewRepository.findByMovie_MovieIdOrderByReviewDateDesc(movieId);
    }

    @Override
    public RatingSummary summarize(Long movieId) {
        return reviewRepository.summarize(movieId);
    }

    @Override
    public boolean canReview(Long customerId, Long movieId) {
        return customerId != null && bookingRepository.countWatched(customerId, movieId, LocalDateTime.now()) > 0;
    }

    @Override
    public ReviewForm getForm(Long customerId, Long movieId) {
        ReviewForm form = new ReviewForm();
        reviewRepository.findById(new ReviewId(customerId, movieId)).ifPresent(r -> {
            form.setRating(r.getRating());
            form.setComment(r.getComment());
        });
        return form;
    }

    @Override
    @Transactional
    public void save(Long customerId, Long movieId, ReviewForm form) {
        if (!canReview(customerId, movieId)) { // BR-16
            throw new BusinessException("Bạn chỉ có thể đánh giá phim đã xem");
        }
        ReviewId id = new ReviewId(customerId, movieId);
        Review review = reviewRepository.findById(id).orElseGet(() -> {
            Review r = new Review();
            r.setId(id);
            r.setCustomer(customerRepository.getReferenceById(customerId));
            r.setMovie(movieRepository.getReferenceById(movieId));
            return r;
        });
        review.setRating(form.getRating());
        review.setComment(form.getComment().trim());
        review.setReviewDate(LocalDateTime.now());
        reviewRepository.save(review);
    }
}
