package com.fu.cinema.service;

import com.fu.cinema.dto.ChangePasswordForm;
import com.fu.cinema.dto.CustomerForm;
import com.fu.cinema.dto.ProfileForm;
import com.fu.cinema.entity.Customer;
import org.springframework.data.domain.Page;

public interface CustomerService {

    // ===== Admin =====
    Page<Customer> search(String keyword, int page, int size);

    CustomerForm getForm(Long customerId);

    void create(CustomerForm form);

    void update(Long customerId, CustomerForm form);

    /** @return true nếu đã xoá hẳn, false nếu chỉ khoá tài khoản (BR-04) */
    boolean delete(Long customerId);

    void toggleLock(Long customerId);

    long count();

    // ===== Customer =====
    ProfileForm getProfile(Long customerId);

    void updateProfile(Long customerId, ProfileForm form);

    void changePassword(Long accountId, ChangePasswordForm form);
}
