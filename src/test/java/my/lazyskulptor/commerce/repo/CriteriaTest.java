package my.lazyskulptor.commerce.repo;

import static org.assertj.core.api.Assertions.assertThat;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.uni.UniReactorConverters;
import my.lazyskulptor.adapter.AdapterRepository;
import my.lazyskulptor.adapter.DemoTxManager;
import my.lazyskulptor.adapter.SessionDispatcher;
import my.lazyskulptor.adapter.SimpleAdapterRepository;
import my.lazyskulptor.commerce.ContainerExtension;
import my.lazyskulptor.commerce.IdEqualsSpec;
import my.lazyskulptor.commerce.model.Account;
import my.lazyskulptor.commerce.spec.Spec;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@SpringBootTest
@ExtendWith(ContainerExtension.class)
public class CriteriaTest {

    @SpyBean
    private Mutiny.SessionFactory sessionFactory;

    private AdapterRepository<Account, Long> accountRepository;

    @BeforeEach
    void setup() {
        this.accountRepository = new SimpleAdapterRepository<>(sessionFactory, new DemoTxManager(sessionFactory), Account.class);
    }

    private final Spec<Account> idEquals = new IdEqualsSpec(1L);

    @Test
    void testCriteria() {
        Account account = accountRepository.findOne(idEquals).block();

        System.out.println(account);
    }

    @Test
    void testWithMutiny() {
        Mono<Mutiny.Session> tx = sessionFactory.openSession()
                .log("Open Session in TX")
                .convert().with(UniReactorConverters.toMono());

        var result = tx.flatMap(session -> Mono.zip(accountRepository.findList(idEquals, Pageable.ofSize(20)),
                        accountRepository.findList(idEquals, null))
                .flatMap(t -> {
                    Account acc = t.getT1().stream().findFirst().get();
                    System.out.println("Total : " + t.getT2());

                    return Mutiny.fetch(acc.getAuthorities())
                            .convert().with(UniReactorConverters.toMono())
                            .thenReturn(acc);
                })
                .contextWrite(c -> c.put(DemoTxManager.SESSION_KEY, new AtomicReference<>(session)))
                        .doFinally(_s -> session.close().log("Close Session").convert().with(UniReactorConverters.toMono()).subscribe()))
                .block();

        assertThat(result.getEmail()).isNotBlank();
        assertThat(result.getAuthorities()).hasSizeGreaterThan(0);
    }

    @Test
    void testWithCtx() {
        Mono<Mutiny.Session> tx = sessionFactory.openSession()
                .log("Open Session in TX")
                .convert().with(UniReactorConverters.toMono());

        Exception thrown = null;
        try {
            tx.flatMap(session -> Mono.zip(accountRepository.findList(idEquals, Pageable.ofSize(20)),
                            accountRepository.findList(idEquals, null))
                    .flatMap(t -> {
                        Account acc = t.getT1().stream().findFirst().get();
                        System.out.println("Total : " + t.getT2());

                        // using fetch method in session not work
                        return tx.flatMap(ss -> ss.fetch(acc.getAuthorities())
                                        .convert().with(UniReactorConverters.toMono()))
                                .thenReturn(acc);
                    })
                    .doFinally(_s -> session.close().log("Close Session").convert().with(UniReactorConverters.toMono()).subscribe())
                    .contextWrite(c -> c.put(DemoTxManager.SESSION_KEY, new AtomicReference<>(session))))
                    .block();
        } catch (Exception e) {
            thrown = e;
        }
        assertThat(thrown).isNotNull();
    }

    @Test
    void testWithSession() {
        var result = sessionFactory.withSession(session -> {
            var mono = Mono.zip(accountRepository.findList(idEquals, Pageable.ofSize(20)),
                            accountRepository.findList(idEquals, null))
                    .flatMap(t -> {
                        Account acc = t.getT1().stream().findFirst().get();
                        System.out.println("Total : " + t.getT2());

                        // using fetch method in session not work
                        return session.fetch(acc.getAuthorities())
                                .convert().with(UniReactorConverters.toMono())
                                .thenReturn(acc);
                    })
                    .contextWrite(c -> c.put(DemoTxManager.SESSION_KEY, new AtomicReference<>(session)));
            return Uni.createFrom().converter(UniReactorConverters.fromMono(), mono);
        }).await().indefinitely();

        Mockito.verify(sessionFactory, Mockito.times(1)).openSession();
        Mockito.verify(sessionFactory, Mockito.times(1)).withSession(Mockito.any());
        assertThat(result.getEmail()).isNotBlank();
        assertThat(result.getAuthorities()).hasSizeGreaterThan(0);
    }

    @Test
    void testTransaction() {
        var result = sessionFactory.withTransaction(session -> {
            var mono = Mono.zip(accountRepository.findList(idEquals, Pageable.ofSize(20)),
                            accountRepository.findList(idEquals, null))
                    .map(t -> {
                        Account acc = t.getT1().stream().findFirst().get();
                        System.out.println("Total : " + t.getT2());

                        // using fetch method in session not work
                        return acc;
                    })
                    .transformDeferredContextual((accountMono, contextView) -> {
                        return accountMono.flatMap(acc -> {
                            Mutiny.Session session1 = contextView.<AtomicReference<Mutiny.Session>>get(DemoTxManager.SESSION_KEY).get();
                            return session1.fetch(acc.getAuthorities())
                                            .convert().with(UniReactorConverters.toMono())
                                    .thenReturn(acc);
                        });
                    })
                    .contextWrite(c -> c.put(DemoTxManager.SESSION_KEY, new AtomicReference<>(session)));
            return Uni.createFrom().converter(UniReactorConverters.fromMono(), mono);
        }).await().indefinitely();

        Mockito.verify(sessionFactory, Mockito.times(1)).openSession();
        Mockito.verify(sessionFactory, Mockito.times(1)).withTransaction(Mockito.any(Function.class));
        assertThat(result.getEmail()).isNotBlank();
        assertThat(result.getAuthorities()).hasSizeGreaterThan(0);
    }
}
