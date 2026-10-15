package com.fu.cinema.service.impl;

import com.fu.cinema.dto.ChangePasswordForm;
import com.fu.cinema.dto.CustomerForm;
import com.fu.cinema.dto.ProfileForm;
import com.fu.cinema.entity.Account;
import com.fu.cinema.entity.Customer;
import com.fu.cinema.enums.AccountStatus;
import com.fu.cinema.enums.Role;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.exception.ResourceNotFoundException;
import com.fu.cinema.repository.AccountRepository;
import com.fu.cinema.repository.BookingRepository;
import com.fu.cinema.repository.CustomerRepository;
import com.fu.cinema.service.CustomerService;
import com.fu.cinema.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;

    // ================= Admin =================

    @Override
    public Page<Customer> search(String keyword, int page, int size) {
        return customerRepository.search(ValidationUtils.blankToNull(keyword),
                PageRequest.of(Math.max(page, 0), size, Sort.by("fullName")));
    }

    @Override
    public CustomerForm getForm(Long customerId) {
        Customer c = findWithAccount(customerId);
        CustomerForm form = new CustomerForm();
        form.setCustomerId(c.getCustomerId());
        form.setFullName(c.getFullName());
        form.setEmail(c.getEmail());
        form.setMobile(c.getMobile());
        form.setBirthday(c.getBirthday());
        form.setIdentityCard(c.getIdentityCard());
        form.setStatus(c.getAccount().getStatus());
        return form;
    }

    @Override
    @Transactional
    public void create(CustomerForm form) {
        if (form.getPassword() == null || form.getPassword().isBlank()) {
            throw new BusinessException("Mật khẩu không được để trống khi tạo khách hàng");
        }
        String email = form.getEmail().trim().toLowerCase();
        checkDuplicates(email, form.getMobile(), form.getIdentityCard(), null, null);
        checkAge(form);

        Account account = accountRepository.save(new Account(
                email, passwordEncoder.encode(form.getPassword()), Role.CUSTOMER, form.getStatus()));
        Customer customer = new Customer();
        customer.setAccount(account);
        copy(form, customer, email);
        customerRepository.save(customer);
    }

    @Override
    @Transactional
    public void update(Long customerId, CustomerForm form) {
        Customer customer = findWithAccount(customerId);
        Account account = customer.getAccount();
        String email = form.getEmail().trim().toLowerCase();
        checkDuplicates(email, form.getMobile(), form.getIdentityCard(), customerId, account.getAccountId());
        checkAge(form);

        copy(form, customer, email);
        account.setUsername(email);
        account.setStatus(form.getStatus());
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            account.setPassword(passwordEncoder.encode(form.getPassword()));
        }
    }

    @Override
    @Transactional
    public boolean delete(Long customerId) {
        Customer customer = findWithAccount(customerId);
        if (bookingRepository.existsByCustomer_CustomerId(customerId)) {
            customer.getAccount().setStatus(AccountStatus.LOCKED);
            return false;
        }
        Account account = customer.getAccount();
        customerRepository.delete(customer);
        accountRepository.delete(account);
        return true;
    }

    @Override
    @Transactional
    public void toggleLock(Long customerId) {
        Account account = findWithAccount(customerId).getAccount();
        account.setStatus(account.getStatus() == AccountStatus.ACTIVE ? AccountStatus.LOCKED : AccountStatus.ACTIVE);
    }

    @Override
    public long count() {
        return customerRepository.count();
    }

    // ================= Customer =================

    @Override
    public ProfileForm getProfile(Long customerId) {
        Customer c = findWithAccount(customerId);
        ProfileForm form = new ProfileForm();
        form.setUsername(c.getAccount().getUsername());
        form.setFullName(c.getFullName());
        form.setEmail(c.getEmail());
        form.setMobile(c.getMobile());
        form.setBirthday(c.getBirthday());
        form.setIdentityCard(c.getIdentityCard());
        return form;
    }

    @Override
    @Transactional
    public void updateProfile(Long customerId, ProfileForm form) {
        Customer customer = findWithAccount(customerId);
        String email = form.getEmail().trim().toLowerCase();
        checkDuplicates(email, form.getMobile(), form.getIdentityCard(), customerId, customer.getAccount().getAccountId());
        if (ValidationUtils.age(form.getBirthday()) < 13) {
            throw new BusinessException("Khách hàng phải từ đủ 13 tuổi");
        }
        customer.setFullName(form.getFullName().trim());
        customer.setEmail(email);
        customer.setMobile(form.getMobile());
        customer.setBirthday(form.getBirthday());
        customer.setIdentityCard(form.getIdentityCard());
    }

    @Override
    @Transactional
    public void changePassword(Long accountId, ChangePasswordForm form) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản", accountId));
        if (!passwordEncoder.matches(form.getCurrentPassword(), account.getPassword())) {
            throw new BusinessException("Mật khẩu hiện tại không đúng");
        }
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            throw new BusinessException("Mật khẩu nhập lại không khớp");
        }
        account.setPassword(passwordEncoder.encode(form.getNewPassword()));
    }

    // ================= helpers =================

    private Customer findWithAccount(Long customerId) {
        return customerRepository.findWithAccountById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Khách hàng", customerId));
    }

    private void checkDuplicates(String email, String mobile, String identityCard, Long customerId, Long accountId) {
        boolean isNew = customerId == null;
        boolean emailUsed = isNew
                ? customerRepository.existsByEmail(email) || accountRepository.existsByUsername(email)
                : customerRepository.existsByEmailAndCustomerIdNot(email, customerId)
                  || accountRepository.existsByUsernameAndAccountIdNot(email, accountId);
        if (emailUsed) {
            throw new BusinessException("Email đã được sử dụng");
        }
        if (isNew ? customerRepository.existsByMobile(mobile)
                  : customerRepository.existsByMobileAndCustomerIdNot(mobile, customerId)) {
            throw new BusinessException("Số điện thoại đã được sử dụng");
        }
        if (isNew ? customerRepository.existsByIdentityCard(identityCard)
                  : customerRepository.existsByIdentityCardAndCustomerIdNot(identityCard, customerId)) {
            throw new BusinessException("Số CCCD đã được sử dụng");
        }
    }

    private void checkAge(CustomerForm form) {
        if (ValidationUtils.age(form.getBirthday()) < 13) {
            throw new BusinessException("Khách hàng phải từ đủ 13 tuổi");
        }
    }

    private void copy(CustomerForm form, Customer customer, String email) {
        customer.setFullName(form.getFullName().trim());
        customer.setEmail(email);
        customer.setMobile(form.getMobile());
        customer.setBirthday(form.getBirthday());
        customer.setIdentityCard(form.getIdentityCard());
    }
}
