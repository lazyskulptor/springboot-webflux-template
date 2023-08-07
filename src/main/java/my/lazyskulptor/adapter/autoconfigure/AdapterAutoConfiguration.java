package my.lazyskulptor.adapter.autoconfigure;

import my.lazyskulptor.adapter.DefaultSessionDispatcher;
import my.lazyskulptor.adapter.HibernateReactiveTransactionManager;
import my.lazyskulptor.adapter.SessionDispatcher;
import org.hibernate.reactive.mutiny.Mutiny;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.NonNull;
import org.springframework.transaction.TransactionManager;

@Configuration
public class AdapterAutoConfiguration {


    private HibernateReactiveTransactionManager manager;
    @Bean
    public HibernateReactiveTransactionManager transactionManager(@NonNull Mutiny.SessionFactory sessionFactory) {
        return getManager(sessionFactory);
    }

    @Bean
    @Primary
    public SessionDispatcher sessionDispatcher(@NonNull Mutiny.SessionFactory sessionFactory, TransactionManager transactionManager) {
        if (!(transactionManager instanceof HibernateReactiveTransactionManager)) {
            return new DefaultSessionDispatcher(sessionFactory);
        }
        return getManager(sessionFactory);
    }

    private synchronized HibernateReactiveTransactionManager getManager(Mutiny.SessionFactory sessionFactory) {
        if (manager == null) {
            manager = new HibernateReactiveTransactionManager(sessionFactory);
        }
        return manager;
    }
}
