package my.lazyskulptor.commerce.repo.impl;

import io.smallrye.mutiny.Uni;
import my.lazyskulptor.commerce.repo.BasicCmdRepository;
import org.hibernate.reactive.mutiny.Mutiny;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public abstract class CmdTemplate<ID, T> implements BasicCmdRepository<ID, T> {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(CmdTemplate.class);

    private final Mutiny.SessionFactory sessionFactory;

    private final Class<T> classType;

    protected CmdTemplate(Mutiny.SessionFactory sessionFactory, Class<T> classType) {
        this.sessionFactory = sessionFactory;
        this.classType = classType;
    }

    @Override
    public <S extends T> Mono<Void> save(S entity) {
        return TemplateUtils.INSTANCE
                .template(sessionFactory, ss ->  ss.persist(entity));
    }

    @Override
    public <S extends T> Mono<T> saveAndFlush(S entity) {
        return TemplateUtils.INSTANCE
                .template(sessionFactory, ss ->  ss.persist(entity).call(ss::flush)
                        .chain(() -> Uni.createFrom().item(entity)));
    }

    @Override
    public <S extends T> Mono<Void> saveAll(Flux<S> entity) {
        return entity.collectList().flatMap(list -> TemplateUtils.INSTANCE
                .template(sessionFactory, ss -> ss.persistAll(list.toArray())));
    }

    @Override
    public <S extends T> Flux<T> saveAllAndFlush(Flux<S> entity) {
        return entity.collectList().flatMap(list -> TemplateUtils.INSTANCE
                .template(sessionFactory, ss -> ss.persistAll(list.toArray()).call(ss::flush)
                        .chain(() -> Uni.createFrom().item(list))))
                .flatMapMany(list -> Flux.fromIterable(list));
    }

    @Override
    public Mono<Void> flush() {
        return TemplateUtils.INSTANCE.template(sessionFactory, ss -> ss.flush());
    }

    public <T> Mono<T> fetch(T association) {
        return TemplateUtils.INSTANCE.template(sessionFactory, ss -> ss.fetch(association));
    }

    @Override
    public Mono<Void> deleteById(ID id) {
        return TemplateUtils.INSTANCE
                .template(sessionFactory, ss -> ss.find(classType, id).flatMap(ss::remove));
    }

    @Override
    public Mono<Void> deleteAllById(Flux<ID> id) {
        return id.collectList().flatMap(list ->
                TemplateUtils.INSTANCE
                        .template(sessionFactory, ss -> ss.find(classType, list.toArray())
                                .flatMap(ss::removeAll)));
    }
}
