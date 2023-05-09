package my.lazyskulptor.commerce.repo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BasicCmdRepository<ID, T> {

    <S extends T> Mono<Void> save(S entity);

    <S extends T> Mono<T> saveAndFlush(S entity);

    <S extends T> Mono<Void> saveAll(Flux<S> entity);

    <S extends T> Flux<T> saveAllAndFlush(Flux<S> entity);

    Mono<Void> flush();

    <T> Mono<T> fetch(T association);

    Mono<Void> deleteById(ID id);

    Mono<Void> deleteAllById(Flux<ID> id);
}
