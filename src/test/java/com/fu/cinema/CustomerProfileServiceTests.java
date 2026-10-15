package com.fu.cinema;

import com.fu.cinema.dto.ProfileForm;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.repository.AccountRepository;
import com.fu.cinema.repository.CustomerRepository;
import com.fu.cinema.service.AuthService;
import com.fu.cinema.service.CustomerService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.datasource.url=jdbc:h2:mem:profile_service;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class CustomerProfileServiceTests {
    @Autowired AuthService auth;
    @Autowired CustomerService service;
    @Autowired AccountRepository accounts;
    @MockitoSpyBean CustomerRepository customers;
    @Autowired EntityManager em;
    private CustomerProfileFixture.Data data;
    @BeforeEach void seed() {
        customers.deleteAll();
        accounts.deleteAll();
        data = CustomerProfileFixture.seed(auth);
    }

    @Test
    void normalizesContactFieldsWithoutUpdatingLoginIdentity() {
        var form = service.getProfile(data.an().getCustomerId());
        form.setUsername("cannot-change@example.test");
        form.setFullName("  New Name ");
        form.setEmail(" CONTACT@EXAMPLE.TEST ");
        form.setMobile(" 0987654321 ");
        form.setIdentityCard(" 912345678901 ");
        service.updateProfile(data.an().getCustomerId(), form);
        var saved = service.getProfile(data.an().getCustomerId());
        assertThat(saved.getFullName()).isEqualTo("New Name");
        assertThat(saved.getEmail()).isEqualTo("contact@example.test");
        assertThat(saved.getMobile()).isEqualTo("0987654321");
        assertThat(saved.getIdentityCard()).isEqualTo("912345678901");
        assertThat(saved.getUsername()).isEqualTo("an@example.test");
    }

    @ParameterizedTest
    @ValueSource(strings = {"email", "mobile", "identityCard"})
    void duplicatesRejectOtherCustomersAndExcludeCurrentRecord(String field) {
        var form = service.getProfile(data.an().getCustomerId());
        assertThatCode(() -> service.updateProfile(data.an().getCustomerId(), form)).doesNotThrowAnyException();
        var other = service.getProfile(data.binh().getCustomerId());
        switch (field) {
            case "email" -> form.setEmail(other.getEmail());
            case "mobile" -> form.setMobile(other.getMobile());
            default -> form.setIdentityCard(other.getIdentityCard());
        }
        assertThatThrownBy(() -> service.updateProfile(data.an().getCustomerId(), form)).isInstanceOf(BusinessException.class);
    }

    @Test
    void invalidValuesAndUnderageBirthdayDoNotModifyStoredProfile() {
        List<Consumer<ProfileForm>> changes = List.of(
                form -> form.setBirthday(LocalDate.now().minusYears(13).plusDays(1)),
                form -> form.setBirthday(LocalDate.now().plusDays(1)),
                form -> form.setMobile("123"),
                form -> form.setIdentityCard("123"),
                form -> form.setFullName(" "),
                form -> form.setEmail("bad"),
                form -> form.setEmail("x".repeat(90) + "@example.test"));
        for (var change : changes) {
            var form = service.getProfile(data.an().getCustomerId());
            change.accept(form);
            assertThatThrownBy(() -> service.updateProfile(data.an().getCustomerId(), form)).isInstanceOf(BusinessException.class);
        }
        assertThat(service.getProfile(data.an().getCustomerId()).getFullName()).isEqualTo("Nguyen Van An");
    }

    @Test
    void uniquenessFailureAfterFlushingRollsBackProfileChanges() {
        var form = service.getProfile(data.an().getCustomerId());
        form.setFullName("Must rollback");
        form.setEmail("rollback@example.test");
        doAnswer(invocation -> {
            em.flush();
            throw new DataIntegrityViolationException("simulated concurrent unique conflict");
        }).when(customers).flush();
        try {
            assertThatThrownBy(() -> service.updateProfile(data.an().getCustomerId(), form))
                    .isInstanceOf(BusinessException.class);
            var saved = service.getProfile(data.an().getCustomerId());
            assertThat(saved.getFullName()).isEqualTo("Nguyen Van An");
            assertThat(saved.getEmail()).isEqualTo("an@example.test");
        } finally {
            reset(customers);
        }
    }
}
