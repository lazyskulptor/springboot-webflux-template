package my.lazyskulptor.commerce.repo;

import static org.assertj.core.api.Assertions.assertThat;

import my.lazyskulptor.commerce.ContainerExtension;
import my.lazyskulptor.commerce.model.Account;
import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

@SpringBootTest
@ExtendWith(ContainerExtension.class)
public class PersistenceTest {

    @Autowired
    Mutiny.SessionFactory sessionFactory;

    @Test
    void persistTest() {
        Account account = Account.builder()
                .password(RandomStringUtils.randomAlphanumeric(16))
                .enabled(true)
                .build();
        sessionFactory.withSession(session ->
                        session.persist(account)
                                .chain(session::flush))
                        .await().atMost(Duration.ofSeconds(1));

        var persisted = sessionFactory.withSession(session -> session.find(Account.class, account.getId()))
                .await().indefinitely();

        assertThat(persisted).isNotEqualTo(account);
        assertThat(persisted.getPassword()).isEqualTo(account.getPassword());
    }

    @Test
    void testWithFakeData() {
        var persisted = sessionFactory.withSession(session -> session.createQuery("SELECT m FROM Account m", Account.class)
                .getResultList()).await().indefinitely();

        assertThat(persisted).hasSizeGreaterThan(0);
    }
}