package my.lazyskulptor.adapter;

import org.springframework.data.repository.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface CommandRepository<ID, T> extends Repository, SessionRepository {

    <S extends T> Mono<Void> save(S entity);

    <S extends T> Mono<T> saveAndFlush(S entity);

    <S extends T> Mono<Void> saveAll(Flux<S> entity);

    <S extends T> Flux<T> saveAllAndFlush(Flux<S> entity);

    Mono<Void> deleteById(ID id);

    Mono<Void> deleteAllById(Flux<ID> id);
}
