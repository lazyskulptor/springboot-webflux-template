package my.lazyskulptor.adapter.support;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.uni.UniReactorConverters;
import org.hibernate.reactive.mutiny.Mutiny;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Mono;

import java.util.function.Function;

public class DefaultSessionDispatcher implements InitializingBean, SessionDispatcher {

    @NonNull
    private Mutiny.SessionFactory sessionFactory;
    public DefaultSessionDispatcher(@NonNull Mutiny.SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        afterPropertiesSet();
    }

    @Override
    public void afterPropertiesSet() {
        if (this.sessionFactory == null) {
            throw new IllegalArgumentException("Property 'connectionFactory' is required");
        }
    }

    @Override
    public <R> Mono<R> apply(Function<Mutiny.Session, Uni<R>> work) {
        return sessionFactory.withTransaction(work)
                    .convert().with(UniReactorConverters.toMono());
    }

    @Override
    public Mutiny.SessionFactory getSessionFactory() {
        return this.sessionFactory;
    }
}
