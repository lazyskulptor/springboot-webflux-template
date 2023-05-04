package my.lazyskulptor.commerce.repo;

import my.lazyskulptor.commerce.spec.Spec;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

import java.util.List;

public interface BasicQueryRepository<T> {
    Mono<T> findOne(Spec<T> spec);

    Mono<List<T>> findList(Spec<T> spec, Pageable page);

    Mono<Page<T>> findPage(Spec<T> spec, Pageable page);

    Mono<Boolean> exists(Spec<T> spec);

    Mono<Long> count(Spec<T> spec);
}
