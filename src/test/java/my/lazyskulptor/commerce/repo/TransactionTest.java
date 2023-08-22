package my.lazyskulptor.commerce.repo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.uni.UniReactorConverters;
import my.lazyskulptor.adapter.DemoTxManager;
import my.lazyskulptor.adapter.SimpleAdapterRepository;
import my.lazyskulptor.commerce.DataHBTest;
import my.lazyskulptor.commerce.IdEqualsSpec;
import my.lazyskulptor.commerce.model.Account;
import my.lazyskulptor.commerce.spec.Logic;
import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.mutiny.impl.MutinySessionImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

//@DataHBTest
@SpringBootTest
public class TransactionTest {

    @SpyBean
    private Mutiny.SessionFactory sessionFactory;

    private SimpleAdapterRepository<Account, Long> accountRepository;

    private Supplier<Account> accountFixture = () -> {
        String email = RandomStringUtils.randomAlphanumeric(10) +
                "@" +
                RandomStringUtils.randomAlphanumeric(10);
        String password = RandomStringUtils.randomAlphanumeric(16);

        return Account.builder()
                .email(email)
                .enabled(true)
                .password(password).build();
    };

    @BeforeEach
    void setup() {
        this.accountRepository = new SimpleAdapterRepository<>(sessionFactory, new DemoTxManager(sessionFactory), Account.class);
//        this.demoDispatcher = new DemoTxManager(sessionFactory);
    }

    @Test
    void testSave() {
        Account entity = accountFixture.get();

        accountRepository.save(entity).block();

        assertThat(entity.getId()).isNotNull();
    }

    @Test
    void testTxLateFlush() {
        Account entity = accountFixture.get();

        var persisted = sessionFactory.withTransaction(session -> {
            var mono = accountRepository.save(entity).log("AFTER SAVE")
                    .then(accountRepository.flush().singleOptional()).log("ON FLUSH")
                    .flatMap(_saved -> accountRepository.findOne(new IdEqualsSpec(entity.getId()))).log("IN FLATMAP")
                    .contextWrite(c -> c.put(DemoTxManager.SESSION_KEY, new AtomicReference(session)));
            return Uni.createFrom().converter(UniReactorConverters.fromMono(), mono);
        }).await().indefinitely();

        assertThat(entity.getId()).isNotNull();
        assertThat(persisted.getId()).isNotNull();
    }

    @Test
    void testTxFlush() {
        Account entity = accountFixture.get();

        var persisted = sessionFactory.withTransaction(session -> {
            var mono = accountRepository.saveAndFlush(entity).log("AFTER SAVE")
                    .flatMap(saved -> accountRepository.findOne(new IdEqualsSpec(saved.getId()))).log("IN FLATMAP")
                    .contextWrite(c -> c.put(DemoTxManager.SESSION_KEY, new AtomicReference(session)));
            return Uni.createFrom().converter(UniReactorConverters.fromMono(), mono);
        }).await().indefinitely();

        assertThat(entity.getId()).isNotNull();
        assertThat(persisted.getId()).isNotNull();
    }

    @Test
    void testTxDeferred() {
        Account entity = accountFixture.get();

        Account persisted = sessionFactory.withTransaction(session -> {
            var mono = accountRepository.saveAndFlush(entity).log("AFTER SAVE")
                    .then(Mono.defer(() -> accountRepository.findOne(new IdEqualsSpec(entity.getId())).log("IN THEN")))
                    .contextWrite(c -> c.put(DemoTxManager.SESSION_KEY, new AtomicReference(session)));
            return Uni.createFrom().converter(UniReactorConverters.fromMono(), mono);
        }).await().indefinitely();

        assertThat(entity.getId()).isNotNull();
        assertThat(persisted.getId()).isNotNull();
    }

    @Test
    void testTxByOpen() {
        Account entity = accountFixture.get();
        AtomicReference<Mutiny.Session> sessionAtomicReference = new AtomicReference<>();
        var sessionUni = sessionFactory.openSession()
                .convert().with(UniReactorConverters.toMono())
                .flatMap(ss -> {
                    var impl = (MutinySessionImpl) ss;
                    sessionAtomicReference.set(spy(ss));
                    return Uni.createFrom()
                            .completionStage(impl.getReactiveConnection().beginTransaction())
                                    .replaceWith(sessionAtomicReference)
                            .convert().with(UniReactorConverters.toMono());
                });


        var prevCnt = ((List<Account>) accountRepository.<Account>findList(Logic.TRUE, null).block()).size();
        Account persisted = Mono.usingWhen(sessionUni,
                        ssRefInCtx -> {
                            var localsss = accountRepository.saveAndFlush(entity).log("AFTER SAVE")
                                    .then(Mono.defer(() -> accountRepository.findOne(new IdEqualsSpec(entity.getId())).log("IN THEN")));
                            return localsss;
                        },
                        ref -> ref.get().close().log("DO_FIANLLY").convert().with(UniReactorConverters.toMono()))
                .contextWrite(c -> c.put(DemoTxManager.SESSION_KEY, sessionAtomicReference))
                .block();
//        var afterCnt = ((List<Account>) accountRepository.<Account>findList(Values.TRUE, null).block()).size();

        verify(sessionAtomicReference.get(), times(1)).close();
//        assertThat(afterCnt).isGreaterThan(prevCnt);
        assertThat(entity.getId()).isNotNull();
        assertThat(persisted.getId()).isNotNull();
    }

    @Test
    void testTxFailByOpen() {
        Account entity = accountFixture.get();
        AtomicReference<Mutiny.Session> sessionAtomicReference = new AtomicReference<>();
        var sessionUni = sessionFactory.openSession()
                .chain(sess -> {
                    var ssImpl = (MutinySessionImpl) sess;
                    sessionAtomicReference.set(spy(sess));
                    return Uni.createFrom()
                            .completionStage(ssImpl.getReactiveConnection().beginTransaction())
                                    .replaceWith(sessionAtomicReference);
                })
                .convert().with(UniReactorConverters.toMono());

        Account persisted = null;
        Exception exception = null;
        var prevCnt = ((List<Account>) accountRepository.<Account>findList(Logic.TRUE, null).block()).size();
        try {
            persisted = Mono.usingWhen(sessionUni,
                            sessRefInCtx -> {
                                var localSess = accountRepository.saveAndFlush(entity)
                                        // Mono#then can't be used here to reference entity ID. Because IdEqualsSpec is already evaluated.
                                        .then(accountRepository.findOne(new IdEqualsSpec(entity.getId())));
                                return localSess;
                            },
                            ref -> {
                                var localSess = (MutinySessionImpl) ref.get();
                                var conn = localSess.getReactiveConnection();
                                return Uni.createFrom().completionStage(conn.commitTransaction())
                                        .chain(localSess::close)
                                        .convert().with(UniReactorConverters.toMono());
                            },
                            (ref, ex) -> {
                                var localSess = (MutinySessionImpl) ref.get();
                                var conn = localSess.getReactiveConnection();
                                return Uni.createFrom().completionStage(conn.rollbackTransaction())
                                        .chain(localSess::close)
                                        .convert().with(UniReactorConverters.toMono());
                            },
                            ref -> {
                                var localSess = (MutinySessionImpl) ref.get();
                                var conn = localSess.getReactiveConnection();
                                return Uni.createFrom().completionStage(conn.rollbackTransaction())
                                        .chain(localSess::close)
                                        .convert().with(UniReactorConverters.toMono());
                            })
                    .contextWrite(c -> c.put(DemoTxManager.SESSION_KEY, sessionAtomicReference))
                    .block();
        } catch (Exception e) {
            exception = e;
        }

        var afterCnt = ((List<Account>) accountRepository.<Account>findList(Logic.TRUE, null).block()).size();

        verify(sessionAtomicReference.get(), times(1)).close();
        assertThat(prevCnt).isEqualTo(afterCnt);
        assertThat(persisted).isNull();
        assertThat(exception).isNotNull();
    }

    @Test
    void testTxFail() {
        Account entity = accountFixture.get();

        Account persisted = null;
        Exception exception = null;
        var prevCnt = ((List<Account>) accountRepository.<Account>findList(Logic.TRUE, null).block()).size();
        try {
            persisted = sessionFactory.withTransaction(session -> {
                var mono = accountRepository.saveAndFlush(entity).log("AFTER SAVE")
                        // Mono#then can't be used here to reference entity ID. Because IdEqualsSpec is already evaluated.
                        .then(accountRepository.findOne(new IdEqualsSpec(entity.getId())).log("IN THEN"))
                        .contextWrite(c -> c.put(DemoTxManager.SESSION_KEY, new AtomicReference(session)));
                return Uni.createFrom().converter(UniReactorConverters.fromMono(), mono);
            }).await().indefinitely();
        } catch (Exception e) {
            exception = e;
        }
        var afterCnt = ((List<Account>) accountRepository.<Account>findList(Logic.TRUE, null).block()).size();

        assertThat(entity.getId()).isNotNull();
        assertThat(afterCnt).isEqualTo(prevCnt);
        assertThat(persisted).isNull();
    }

    @Test
    void testTxRollback() {
        Account entity = accountFixture.get();
        Account persisted = null;

        try {
            persisted = sessionFactory.withTransaction(session -> {
                        var mono = accountRepository.saveAndFlush(entity)
                                .flatMap(saved -> accountRepository.findOne(new IdEqualsSpec(entity.getId())))
                                .contextWrite(c -> c.put(DemoTxManager.SESSION_KEY, new AtomicReference(session)));
                        if (true)
                            throw new RuntimeException("Exception to Rollback");

                        return Uni.createFrom().converter(UniReactorConverters.fromMono(), mono);
                    })
                    .onFailure().recoverWithItem(entity)
                    .await().indefinitely();
        } catch (Exception e) {
        }

        assertThat(entity.getId()).isNull();
        assertThat(persisted).isEqualTo(entity);
    }
}
