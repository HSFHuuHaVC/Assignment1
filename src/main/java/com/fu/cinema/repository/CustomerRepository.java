package com.fu.cinema.repository;

import com.fu.cinema.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // ===== Q1: tìm theo tên / email / SĐT + phân trang (JOIN FETCH account để hiện trạng thái) =====
    @Query(value = """
            SELECT c FROM Customer c JOIN FETCH c.account a
            WHERE :kw IS NULL
               OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :kw, '%'))
               OR LOWER(c.email)    LIKE LOWER(CONCAT('%', :kw, '%'))
               OR c.mobile          LIKE CONCAT('%', :kw, '%')
            """,
            countQuery = """
            SELECT COUNT(c) FROM Customer c
            WHERE :kw IS NULL
               OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :kw, '%'))
               OR LOWER(c.email)    LIKE LOWER(CONCAT('%', :kw, '%'))
               OR c.mobile          LIKE CONCAT('%', :kw, '%')
            """)
    Page<Customer> search(@Param("kw") String keyword, Pageable pageable);

    @Query("SELECT c FROM Customer c JOIN FETCH c.account WHERE c.customerId = :id")
    Optional<Customer> findWithAccountById(@Param("id") Long id);
}
