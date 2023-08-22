package my.lazyskulptor.adapter;

import io.smallrye.mutiny.Uni;
import my.lazyskulptor.commerce.spec.Spec;
import org.hibernate.reactive.mutiny.Mutiny;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import javax.persistence.criteria.*;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
public class SimpleAdapterRepository<T, ID> implements AdapterRepository<T, ID> {
    private final Mutiny.SessionFactory sessionFactory;

    private final SessionDispatcher dispatcher;

    private final Class<T> classType;

    public SimpleAdapterRepository(Mutiny.SessionFactory sessionFactory, SessionDispatcher dispatcher, Class<T> classType) {
        this.sessionFactory = sessionFactory;
        this.dispatcher = dispatcher;
        this.classType = classType;
    }

    @Override
    public <S extends T> Mono<Void> save(S entity) {

        return this.dispatcher.apply(ss -> ss.persist(entity));
    }

    @Override
    public <S extends T> Mono<T> saveAndFlush(S entity) {
        return this.dispatcher.apply(ss -> ss.persist(entity).call(ss::flush)
                .chain(() -> Uni.createFrom().item(entity)));
    }

    @Override
    public <S extends T> Mono<Void> saveAll(Flux<S> entity) {
        return entity.collectList().flatMap(list -> this.dispatcher
                .apply(ss -> ss.persistAll(list.toArray())));
    }

    @Override
    public <S extends T> Flux<T> saveAllAndFlush(Flux<S> entity) {
        return entity.collectList().flatMap(list -> this.dispatcher
                        .apply(ss -> ss.persistAll(list.toArray()).call(ss::flush)
                                .replaceWith(Uni.createFrom().item(list)))
                        .map(li -> li))
                .flatMapMany(Flux::fromIterable);
    }

    @Override
    public Mono<Void> deleteById(ID id) {
        return this.dispatcher.apply(ss -> ss.find(classType, id).flatMap(ss::remove));
    }

    @Override
    public Mono<Void> deleteAllById(Flux<ID> id) {
        return id.collectList().flatMap(list ->
                this.dispatcher.apply(ss -> ss.find(classType, list.toArray())
                        .flatMap(ss::removeAll)));
    }

    @Override
    public Mono<T> findOne(Spec<?> spec) {
        return this.dispatcher.apply(ss -> ss.createQuery(toQuery(spec)).getSingleResult());
    }

    @Override
    public Mono<Long> count(Spec<?> spec) {
        return this.dispatcher.apply(ss -> ss.createQuery(toCounter(spec)).getSingleResult());
    }

    @Override
    public Mono<List<T>> findList(Spec<?> spec, Pageable page) {
        final Pageable p = ensurePage(page);
        final CriteriaQuery<T> query = toQuery(spec, p);

        return this.dispatcher.apply(ss -> ss.createQuery(query)
                .setMaxResults(p.getPageSize())
                .setFirstResult((int) p.getOffset())
                .getResultList());
    }
    @Override
    public Mono<Page<T>> findPage(Spec<?> spec, Pageable page) {
        final Pageable p = ensurePage(page);
        return Mono.zip(this.findList(spec, page), this.count(spec))
                .map(t -> new PageImpl<>(t.getT1(), p, t.getT2()));
    }
    @Override
    public Mono<Boolean> exists(Spec<?> spec) {
        return this.count(spec).map(cnt -> cnt > 0);
    }

    private Pageable ensurePage(Pageable page) {
        int DEFAULT_PAGE_SIZE = 20;
        return Objects.requireNonNullElse(page, Pageable.ofSize(DEFAULT_PAGE_SIZE));
    }

    private CriteriaQuery<T> toQuery(Spec<?> spec) {
        return toQuery(spec, null);
    }

    private CriteriaQuery<T> toQuery(Spec<?> spec, Pageable page) {
        var tuple3 = criteriaTemplate(spec, page);
        return tuple3.getT1().where(tuple3.getT2()).select(tuple3.getT3());
    }

    private CriteriaQuery<Long> toCounter(Spec<?> spec) {
        var tuple3 = criteriaTemplate(b -> b.createQuery(Long.class), spec, null);
        Expression<Long> counter = sessionFactory.getCriteriaBuilder().count(tuple3.getT3());
        return tuple3.getT1().where(tuple3.getT2()).select(counter);
    }

    protected Tuple3<CriteriaQuery<T>, Predicate, Root<T>> criteriaTemplate(Spec<?> spec) {
        return criteriaTemplate(spec, null);
    }

    protected Tuple3<CriteriaQuery<T>, Predicate, Root<T>> criteriaTemplate(Spec<?> spec, Pageable page) {
        return criteriaTemplate((b) -> b.createQuery(this.classType), spec, page);
    }

    protected <S> Tuple3<CriteriaQuery<S>, Predicate, Root<T>> criteriaTemplate(Function<CriteriaBuilder, CriteriaQuery<S>> applier, Spec<?> spec) {
        return criteriaTemplate(applier, spec, null);
    }

    protected <S> Tuple3<CriteriaQuery<S>, Predicate, Root<T>> criteriaTemplate(Function<CriteriaBuilder, CriteriaQuery<S>> applier, Spec<?> spec, Pageable page) {
        CriteriaBuilder builder = sessionFactory.getCriteriaBuilder();
        CriteriaQuery<S> query = applier.apply(builder);
        Root<T> root = query.from(this.classType);

        if (!Objects.isNull(page)) {
            query = query.orderBy(page.getSort()
                    .stream()
                    .map(s -> {
                        Expression<?> expression = root.get(s.getProperty());
                        return s.isAscending() ? builder.asc(expression) : builder.desc(expression);
                    })
                    .collect(Collectors.toList()));
        }

        Predicate predicate = spec.toPredicate(root, query, builder);
        return Tuples.of(query, predicate, root);
    }


    @Override
    public Flux<T> findAll(Sort sort) {
        // FIXME:
        return null;
    }

    @Override
    public SessionDispatcher getSessionDispatcher() {
        return this.dispatcher;
    }
}
