package my.lazyskulptor.commerce.repo;

import static org.assertj.core.api.Assertions.assertThat;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.uni.UniReactorConverters;
import my.lazyskulptor.commerce.ContainerExtension;
import my.lazyskulptor.commerce.IdEqualsSpec;
import my.lazyskulptor.commerce.model.Account;
import my.lazyskulptor.commerce.repo.impl.AccountCmdRepoImpl;
import my.lazyskulptor.commerce.repo.impl.AccountRepositoryImpl;
import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

@SpringBootTest
@ExtendWith(ContainerExtension.class)
public class TransactionTest {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(TransactionTest.class);
    @SpyBean
    private Mutiny.SessionFactory sessionFactory;
    private AccountQueryRepository accountRepository;
    private AccountCmdRepository accountCmd;

    private Supplier<Account> accountFixture = () -> {
        String email = new StringBuilder(RandomStringUtils.randomAlphanumeric(10))
                .append("@")
                .append(RandomStringUtils.randomAlphanumeric(10))
                .toString();
        String password = RandomStringUtils.randomAlphanumeric(16);

        return Account.builder()
                .email(email)
                .enabled(true)
                .password(password).build();
    };

    @BeforeEach
    void setup() {
        this.accountRepository = new AccountRepositoryImpl(sessionFactory);
        this.accountCmd = new AccountCmdRepoImpl(sessionFactory);
    }

    @Test
    void testSave() {
        Account entity = accountFixture.get();

        accountCmd.save(entity).block();

        assertThat(entity.getId()).isNotNull();
    }

    @Test
    void testTxLateFlush() {
        Account entity = accountFixture.get();

        var persisted = sessionFactory.withTransaction(session -> {
            var mono = accountCmd.save(entity).log("AFTER SAVE")
                    .then(CommandRepository.flush(accountCmd).singleOptional())
                    .flatMap(_saved -> accountRepository.findOne(new IdEqualsSpec(entity.getId()))).log("IN FLATMAP")
                    .contextWrite(c -> c.put("SESSION", session));
            return Uni.createFrom().converter(UniReactorConverters.fromMono(), mono);
        }).await().indefinitely();

        assertThat(entity.getId()).isNotNull();
        assertThat(persisted.getId()).isNotNull();
    }

    @Test
    void testTxFlush() {
        Account entity = accountFixture.get();

        var persisted = sessionFactory.withTransaction(session -> {
            var mono = accountCmd.saveAndFlush(entity).log("AFTER SAVE")
                    .flatMap(saved -> accountRepository.findOne(new IdEqualsSpec(saved.getId()))).log("IN FLATMAP")
                    .contextWrite(c -> c.put("SESSION", session));
            return Uni.createFrom().converter(UniReactorConverters.fromMono(), mono);
        }).await().indefinitely();

        assertThat(entity.getId()).isNotNull();
        assertThat(persisted.getId()).isNotNull();
    }

    @Test
    void testTxDeferred() {
        Account entity = accountFixture.get();

        Account persisted = sessionFactory.withTransaction(session -> {
            var mono = accountCmd.saveAndFlush(entity).log("AFTER SAVE")
                    .then(Mono.defer(() -> accountRepository.findOne(new IdEqualsSpec(entity.getId())).log("IN THEN")))
                    .contextWrite(c -> c.put("SESSION", session));
            return Uni.createFrom().converter(UniReactorConverters.fromMono(), mono);
        }).await().indefinitely();

        assertThat(entity.getId()).isNotNull();
        assertThat(persisted.getId()).isNotNull();
    }

    @Test
    void testTxFail() {
        Account entity = accountFixture.get();

        Account persisted = null;
        Exception exception = null;
        try {
            persisted = sessionFactory.withTransaction(session -> {
                var mono = accountCmd.saveAndFlush(entity).log("AFTER SAVE")
                        // Mono#then can't be used here to reference entity ID. Because query execution is run deferred.
                        .then(accountRepository.findOne(new IdEqualsSpec(entity.getId())).log("IN THEN"))
                        .contextWrite(c -> c.put("SESSION", session));
                return Uni.createFrom().converter(UniReactorConverters.fromMono(), mono);
            }).await().indefinitely();
        } catch (Exception e) {
            exception = e;
        }

        System.out.println(exception.getMessage());
        assertThat(entity.getId()).isNotNull();
        assertThat(persisted).isNull();
    }

    @Test
    void testTxRollback() {
        Account entity = accountFixture.get();
        Account persisted = null;

        try {
            persisted = sessionFactory.withTransaction(session -> {
                var mono = accountCmd.saveAndFlush(entity)
                        .flatMap(saved -> accountRepository.findOne(new IdEqualsSpec(entity.getId())))
                        .contextWrite(c -> c.put("SESSION", session));
                if (true)
                    throw new RuntimeException("Exception to Rollback");

                return Uni.createFrom().converter(UniReactorConverters.fromMono(), mono);
            })
                    .onFailure().recoverWithItem(entity)
                    .await().indefinitely();
        } catch (Exception e) {}

        assertThat(entity.getId()).isNull();
        assertThat(persisted).isEqualTo(entity);
    }
}
