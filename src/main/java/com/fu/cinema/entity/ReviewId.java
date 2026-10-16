package com.fu.cinema.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

// Khoá chính kép (CustomerID, MovieID): bắt buộc Serializable + equals/hashCode
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class ReviewId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "movie_id")
    private Long movieId;

    public ReviewId(Long customerId, Long movieId) {
        this.customerId = customerId;
        this.movieId = movieId;
    }
}
