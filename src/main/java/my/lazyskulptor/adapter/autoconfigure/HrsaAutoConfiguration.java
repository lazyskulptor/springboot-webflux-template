package my.lazyskulptor.adapter.autoconfigure;

import my.lazyskulptor.adapter.support.DefaultSessionDispatcher;
import my.lazyskulptor.adapter.support.HrsaTransactionManager;
import my.lazyskulptor.adapter.support.SessionDispatcher;
import org.hibernate.reactive.mutiny.Mutiny;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.NonNull;
import org.springframework.transaction.TransactionManager;

@Configuration
@ConditionalOnClass(Mutiny.SessionFactory.class)
public class HrsaAutoConfiguration {


    private HrsaTransactionManager manager;
    @Bean
    public HrsaTransactionManager transactionManager(@NonNull Mutiny.SessionFactory sessionFactory) {
        return getManager(sessionFactory);
    }

    @Bean
    @Primary
    public SessionDispatcher sessionDispatcher(@NonNull Mutiny.SessionFactory sessionFactory, TransactionManager transactionManager) {
        if (!(transactionManager instanceof HrsaTransactionManager)) {
            return new DefaultSessionDispatcher(sessionFactory);
        }
        return getManager(sessionFactory);
    }

    private synchronized HrsaTransactionManager getManager(Mutiny.SessionFactory sessionFactory) {
        if (manager == null) {
            manager = new HrsaTransactionManager(sessionFactory);
        }
        return manager;
    }
}
