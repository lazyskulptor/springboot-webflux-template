package my.lazyskulptor.adapter.repository;

import my.lazyskulptor.adapter.support.SessionDispatcher;
import org.hibernate.reactive.mutiny.Mutiny;
import org.springframework.data.repository.NoRepositoryBean;
import reactor.core.publisher.Mono;

@NoRepositoryBean
public interface HibernateReactiveSessionRepository {
    SessionDispatcher getSessionDispatcher();

    default Mono<Void> flush() {
        return this.getSessionDispatcher().apply(Mutiny.Session::flush);
    }

    default <A> Mono<A> fetch(A association) {
        return this.getSessionDispatcher().apply(ss -> ss.fetch(association));
    }
}
