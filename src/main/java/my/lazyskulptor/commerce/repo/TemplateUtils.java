package my.lazyskulptor.commerce.repo;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.uni.UniReactorConverters;
import org.hibernate.SessionException;
import org.hibernate.reactive.mutiny.Mutiny;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.function.Function;

public enum TemplateUtils {
    INSTANCE;

    public <R> Mono<R> template(Mutiny.SessionFactory sessionFactory, Function<Mutiny.Session, Uni<R>> f) {
        return Mono.deferContextual(ctx -> ctx.<Mutiny.Session>getOrEmpty("SESSION")
                .map(session -> f.apply(session)
                        .log(new StringBuilder("GLOBAL SESSION[").append(session.hashCode()).append("]").toString()))

                .orElseGet(() -> Optional.ofNullable(sessionFactory)
                        .orElseThrow(() -> new SessionException("Session not exists"))
                        .withSession(session -> f.apply(session).call(session::flush)
                                .log(new StringBuilder("LOCAL SESSION[").append(session.hashCode()).append("]").toString())))
                .convert().with(UniReactorConverters.toMono())
        );
    }
}
