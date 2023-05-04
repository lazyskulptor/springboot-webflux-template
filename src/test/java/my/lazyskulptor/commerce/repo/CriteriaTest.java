package my.lazyskulptor.commerce.repo;

import static org.assertj.core.api.Assertions.assertThat;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.uni.UniReactorConverters;
import my.lazyskulptor.commerce.ContainerExtension;
import my.lazyskulptor.commerce.model.Account;
import my.lazyskulptor.commerce.repo.impl.AccountRepositoryImpl;
import my.lazyskulptor.commerce.spec.Spec;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.function.Function;

@SpringBootTest
@ExtendWith(ContainerExtension.class)
public class CriteriaTest {

    @SpyBean
    private Mutiny.SessionFactory sessionFactory;
    private AccountQueryRepository accountRepository;

    @BeforeEach
    void setup() {
        this.accountRepository = new AccountRepositoryImpl(sessionFactory);
    }

    private Spec<Account> idEquals = new Spec<>() {
        @Override
        public boolean isSatisfiedBy(Account o) {
            return false;
        }

        @Override
        public Predicate toPredicate(Root<Account> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
            return criteriaBuilder.equal(root.get("id"), 1L);
        }
    };

    @Test
    void testCriteria() {
        System.out.println("ATTENTION");
        Account account = accountRepository.findOne(idEquals).block();

        System.out.println(account);
    }

    @Test
    void testWithMutiny() {
        Mono<Mutiny.Session> tx = sessionFactory.openSession()
                .log("Open Session in TX")
                .convert().with(UniReactorConverters.toMono());

        var result = Mono.zip(accountRepository.findList(idEquals, Pageable.ofSize(20)),
                        accountRepository.findList(idEquals, null))
                .flatMap(t -> {
                    var acc = t.getT1().stream().findFirst().get();
                    System.out.println("Total : " + t.getT2());

                    return Mutiny.fetch(acc.getAuthorities())
                            .convert().with(UniReactorConverters.toMono())
                            .thenReturn(acc);
                })
                .doFinally(_s -> tx.flatMap(ss -> ss.close().log("Close Session").convert().with(UniReactorConverters.toMono())).subscribe())
                .contextWrite(c -> c.put("SESSION", tx))
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
            Mono.zip(accountRepository.findList(idEquals, Pageable.ofSize(20)),
                            accountRepository.findList(idEquals, null))
                    .flatMap(t -> {
                        var acc = t.getT1().stream().findFirst().get();
                        System.out.println("Total : " + t.getT2());

                        // using fetch method in session not work
                        return tx.flatMap(ss -> ss.fetch(acc.getAuthorities())
                                        .convert().with(UniReactorConverters.toMono()))
                                .thenReturn(acc);
                    })
                    .doFinally(_s -> tx.flatMap(ss -> ss.close().log("Close Session").convert().with(UniReactorConverters.toMono())).subscribe())
                    .contextWrite(c -> c.put("SESSION", tx))
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
                        var acc = t.getT1().stream().findFirst().get();
                        System.out.println("Total : " + t.getT2());

                        // using fetch method in session not work
                        return session.fetch(acc.getAuthorities())
                                .convert().with(UniReactorConverters.toMono())
                                .thenReturn(acc);
                    })
                    .contextWrite(c -> c.put("SESSION", Mono.just(session)));
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
                        var acc = t.getT1().stream().findFirst().get();
                        System.out.println("Total : " + t.getT2());

                        // using fetch method in session not work
                        return acc;
                    })
                    .transformDeferredContextual((accountMono, contextView) -> {
                        return accountMono.flatMap(acc -> {
                            Mono<Mutiny.Session> session1 = contextView.get("SESSION");
                            return session1.flatMap(ss -> ss.fetch(acc.getAuthorities())
                                            .convert().with(UniReactorConverters.toMono()))
                                    .thenReturn(acc);
                        });
                    })
                    .contextWrite(c -> c.put("SESSION", Mono.just(session)));
            return Uni.createFrom().converter(UniReactorConverters.fromMono(), mono);
        }).await().indefinitely();

        Mockito.verify(sessionFactory, Mockito.times(1)).openSession();
        Mockito.verify(sessionFactory, Mockito.times(1)).withTransaction(Mockito.any(Function.class));
        assertThat(result.getEmail()).isNotBlank();
        assertThat(result.getAuthorities()).hasSizeGreaterThan(0);
    }
}
