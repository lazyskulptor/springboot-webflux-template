package my.lazyskulptor.commerce.repo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BasicCmdRepository<ID, T> {

    <S extends T> Mono<S> save(S entity);

    <S extends T> Flux<S> saveAll(Flux<S> entity);

    Mono<Void> deleteById(ID id);

    Mono<Void> deleteAllById(Flux<ID> id);
}
