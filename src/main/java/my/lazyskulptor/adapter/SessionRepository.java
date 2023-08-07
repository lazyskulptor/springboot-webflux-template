package my.lazyskulptor.adapter;

import org.springframework.data.repository.NoRepositoryBean;
import reactor.core.publisher.Mono;

@NoRepositoryBean
public interface SessionRepository {
    SessionDispatcher getSessionDispatcher();

    default Mono<Void> flush() {
        return this.getSessionDispatcher().apply(ss -> ss.flush());
    }

    default <A> Mono<A> fetch(A association) {
        return this.getSessionDispatcher().apply(ss -> ss.fetch(association));
    }
}
