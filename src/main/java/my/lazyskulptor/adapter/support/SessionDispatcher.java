package my.lazyskulptor.adapter.support;

import io.smallrye.mutiny.Uni;
import org.hibernate.reactive.mutiny.Mutiny;
import reactor.core.publisher.Mono;

import java.util.function.Function;

public interface SessionDispatcher {

    Mutiny.SessionFactory getSessionFactory();
    <R> Mono<R> apply(Function<Mutiny.Session, Uni<R>> work);
}
