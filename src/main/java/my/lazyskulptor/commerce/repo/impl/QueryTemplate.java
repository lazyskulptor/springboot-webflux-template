package my.lazyskulptor.commerce.repo.impl;

import my.lazyskulptor.commerce.repo.QueryRepository;
import my.lazyskulptor.commerce.repo.SessionRepository;
import my.lazyskulptor.commerce.repo.TemplateUtils;
import my.lazyskulptor.commerce.spec.Spec;
import org.hibernate.reactive.mutiny.Mutiny;
import org.slf4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import javax.persistence.criteria.*;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class QueryTemplate<T> implements QueryRepository<T> , SessionRepository {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(QueryTemplate.class);
    private final Mutiny.SessionFactory sessionFactory;

    private final Class<T> classType;
    public QueryTemplate(Mutiny.SessionFactory sessionFactory, Class<T> classType) {
        this.sessionFactory = sessionFactory;
        this.classType = classType;
    }

    @Override
    public Mono<T> findOne(Spec<T> spec) {
        return TemplateUtils.INSTANCE.template(sessionFactory, ss -> ss.createQuery(toQuery(spec)).getSingleResult());
    }

    @Override
    public Mono<Long> count(Spec<T> spec) {
        return TemplateUtils.INSTANCE.template(sessionFactory, ss -> ss.createQuery(toCounter(spec)).getSingleResult());
    }

    @Override
    public Mono<List<T>> findList(Spec<T> spec, Pageable page) {
        final Pageable p = ensurePage(page);
        final CriteriaQuery<T> query = toQuery(spec, p);

        return TemplateUtils.INSTANCE.template(sessionFactory, ss -> ss.createQuery(query)
                .setMaxResults(p.getPageSize())
                .setFirstResult((int) p.getOffset())
                .getResultList());
    }
    @Override
    public Mono<Page<T>> findPage(Spec<T> spec, Pageable page) {
        final Pageable p = ensurePage(page);
        return Mono.zip(this.findList(spec, page), this.count(spec))
                .map(t -> new PageImpl<>(t.getT1(), p, t.getT2()));
    }
    @Override
    public Mono<Boolean> exists(Spec<T> spec) {
        return this.count(spec).map(cnt -> cnt > 0);
    }

    private Pageable ensurePage(Pageable page) {
        int DEFAULT_PAGE_SIZE = 20;
        final Pageable p = Objects.requireNonNullElse(page, Pageable.ofSize(DEFAULT_PAGE_SIZE));
        return p;
    }

    private CriteriaQuery<T> toQuery(Spec<T> spec) {
        return toQuery(spec, null);
    }

    private CriteriaQuery<T> toQuery(Spec<T> spec, Pageable page) {
        var tuple3 = criteriaTemplate(spec, page);
        return tuple3.getT1().where(tuple3.getT2()).select(tuple3.getT3());
    }

    private CriteriaQuery<Long> toCounter(Spec<T> spec) {
        var tuple3 = criteriaTemplate(b -> b.createQuery(Long.class), spec, null);
        Expression<Long> counter = sessionFactory.getCriteriaBuilder().count(tuple3.getT3());
        return tuple3.getT1().where(tuple3.getT2()).select(counter);
    }

    protected Tuple3<CriteriaQuery<T>, Predicate, Root<T>> criteriaTemplate(Spec<T> spec) {
        return criteriaTemplate(spec, null);
    }

    protected Tuple3<CriteriaQuery<T>, Predicate, Root<T>> criteriaTemplate(Spec<T> spec, Pageable page) {
        return criteriaTemplate((b) -> b.createQuery(this.classType), spec, page);
    }

    protected <S> Tuple3<CriteriaQuery<S>, Predicate, Root<T>> criteriaTemplate(Function<CriteriaBuilder, CriteriaQuery<S>> applier, Spec<T> spec) {
        return criteriaTemplate(applier, spec, null);
    }

    protected <S> Tuple3<CriteriaQuery<S>, Predicate, Root<T>> criteriaTemplate(Function<CriteriaBuilder, CriteriaQuery<S>> applier, Spec<T> spec, Pageable page) {
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

    public Mutiny.SessionFactory getSessionFactory() {
        return sessionFactory;
    }
}
