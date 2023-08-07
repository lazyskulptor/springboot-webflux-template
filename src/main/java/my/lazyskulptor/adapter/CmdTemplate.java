package my.lazyskulptor.adapter;

import io.smallrye.mutiny.Uni;
import org.hibernate.reactive.mutiny.Mutiny;
import org.springframework.data.repository.NoRepositoryBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@NoRepositoryBean
public abstract class CmdTemplate<ID, T> implements CommandRepository<ID, T> {
    private final Mutiny.SessionFactory sessionFactory;

    private final Class<T> classType;

    private final SessionDispatcher dispatcher;

    protected CmdTemplate(Class<T> classType, SessionDispatcher dispatcher) {
        this.sessionFactory = dispatcher.getSessionFactory();
        this.classType = classType;
        this.dispatcher = dispatcher;
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
    public SessionDispatcher getSessionDispatcher() {
        return this.dispatcher;
    }
}
