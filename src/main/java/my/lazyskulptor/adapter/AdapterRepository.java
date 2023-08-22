package my.lazyskulptor.adapter;

import my.lazyskulptor.commerce.spec.Spec;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@NoRepositoryBean
public interface AdapterRepository<T, ID> extends ReactiveSortingRepository<T, ID>, Repository<T, ID>, SessionRepository {

    <S extends T> Mono<Void> save(S entity);

    <S extends T> Mono<T> saveAndFlush(S entity);

    <S extends T> Mono<Void> saveAll(Flux<S> entity);

    <S extends T> Flux<T> saveAllAndFlush(Flux<S> entity);

    Mono<Void> deleteById(ID id);

    Mono<Void> deleteAllById(Flux<ID> id);

    Mono<T> findOne(Spec<?> spec);

    Mono<List<T>> findList(Spec<?> spec, Pageable page);

    Mono<Page<T>> findPage(Spec<?> spec, Pageable page);

    Mono<Boolean> exists(Spec<?> spec);

    Mono<Long> count(Spec<?> spec);
}
