package com.fu.cinema.repository;

import com.fu.cinema.entity.Customer;
import com.fu.cinema.service.CustomerService;
import com.fu.cinema.service.impl.CustomerServiceImpl;
import com.fu.cinema.config.PasswordConfig;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import static org.assertj.core.api.Assertions.*;

@Import({CustomerServiceImpl.class, PasswordConfig.class, CustomerSearchServiceTests.ValidationConfig.class})
class CustomerSearchServiceTests extends JpaFixture {
    @org.springframework.beans.factory.annotation.Autowired CustomerService service;

    @TestConfiguration
    static class ValidationConfig {
        @Bean
        org.springframework.validation.beanvalidation.LocalValidatorFactoryBean validator() {
            return new org.springframework.validation.beanvalidation.LocalValidatorFactoryBean();
        }
    }

    @Test
    void normalizesEmptyKeywordAndPaginatesStablyByNameThenId() {
        Customer first = customer("Same name", 1);
        Customer second = customer("Same name", 2);
        customer("Zed", 3);
        flushAndClear();
        var page = service.search("  ", -1, 1);
        assertThat(page.getNumber()).isZero();
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).extracting(Customer::getCustomerId).containsExactly(first.getCustomerId());
        assertThat(service.search(null, 1, 1).getContent()).extracting(Customer::getCustomerId)
                .containsExactly(second.getCustomerId());
        assertThat(service.search("Same", 0, 0).getSize()).isEqualTo(10);
        assertThat(service.search("   SAME   ", 0, 10)).hasSize(2);
        assertThat(service.search("missing", 0, 10)).isEmpty();
        assertThat(service.search(null, 99, 10)).isEmpty();
    }

    @Test
    void accountStatusesAreFetchedWithoutNPlusOneQueries() {
        for (int i = 1; i <= 12; i++) customer("Customer " + i, i);
        flushAndClear();
        var statistics = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        boolean previous = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            var page = service.search(null, 0, 10);
            em.clear();
            page.forEach(c -> assertThat(c.getAccount().getStatus()).isNotNull());
            assertThat(page.getTotalElements()).isEqualTo(12);
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(2); // page + count
        } finally {
            statistics.setStatisticsEnabled(previous);
        }
    }
}
