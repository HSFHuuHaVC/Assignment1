package com.fu.cinema.repository;

import com.fu.cinema.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // ===== Derived query =====
    Optional<Customer> findByAccount_AccountId(Long accountId);

    boolean existsByEmail(String email);

    boolean existsByMobile(String mobile);

    boolean existsByIdentityCard(String identityCard);

    boolean existsByEmailAndCustomerIdNot(String email, Long customerId);

    boolean existsByMobileAndCustomerIdNot(String mobile, Long customerId);

    boolean existsByIdentityCardAndCustomerIdNot(String identityCard, Long customerId);
}
