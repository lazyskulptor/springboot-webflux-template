package my.lazyskulptor.adapter;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.uni.UniReactorConverters;
import my.lazyskulptor.adapter.support.DefaultSessionDispatcher;
import org.hibernate.reactive.mutiny.Mutiny;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class DemoTxManager extends DefaultSessionDispatcher {

    public static final String SESSION_KEY = "GLOBAL_SESSION";
    public DemoTxManager(@NonNull Mutiny.SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public <R> Mono<R> apply(Function<Mutiny.Session, Uni<R>> work) {
        return Mono.deferContextual(ctx -> ctx.<AtomicReference<Mutiny.Session>>getOrEmpty(SESSION_KEY)
                    .map(ref -> work.apply(ref.get())
                            .convert().with(UniReactorConverters.toMono()))
                    .orElseGet(() -> super.apply(work)));
    }
}