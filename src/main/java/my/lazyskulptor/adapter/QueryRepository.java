package my.lazyskulptor.adapter;

import my.lazyskulptor.commerce.spec.Spec;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;
import reactor.core.publisher.Mono;

import java.util.List;

@NoRepositoryBean
public interface QueryRepository<T, ID> extends Repository<T, ID>, SessionRepository {
    Mono<T> findOne(Spec<?> spec);

    Mono<List<T>> findList(Spec<?> spec, Pageable page);

    Mono<Page<T>> findPage(Spec<?> spec, Pageable page);

    Mono<Boolean> exists(Spec<?> spec);

    Mono<Long> count(Spec<?> spec);
}
