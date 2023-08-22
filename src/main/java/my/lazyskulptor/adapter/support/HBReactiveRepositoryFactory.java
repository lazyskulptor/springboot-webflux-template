package my.lazyskulptor.adapter.support;

import my.lazyskulptor.adapter.repository.SimpleHrsaRepository;
import org.hibernate.reactive.mutiny.Mutiny;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.ReactiveRepositoryFactorySupport;
import org.springframework.util.Assert;

import javax.persistence.metamodel.Metamodel;


class HBReactiveRepositoryFactory extends ReactiveRepositoryFactorySupport {

    private final Mutiny.SessionFactory sessionFactory;

    private final SessionDispatcher dispatcher;

    public HBReactiveRepositoryFactory(Mutiny.SessionFactory sessionFactory, SessionDispatcher dispatcher) {
        this.sessionFactory = sessionFactory;
        this.dispatcher = dispatcher;
    }

    @Override
    public final  <T, ID> EntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
        Assert.notNull(domainClass, "Domain class must not be null!");
        Assert.notNull(sessionFactory, "Mutiny.SessionFactory must not be null!");
        Metamodel metamodel = sessionFactory.getMetamodel();
        return new HrsaEntityInformation<>(domainClass, metamodel);
    }

    @Override
    protected final Object getTargetRepository(RepositoryInformation information) {
        var domainClass = information.getDomainType();
        return getTargetRepositoryViaReflection(information, sessionFactory, dispatcher, information.getDomainType());
    }

    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
        return SimpleHrsaRepository.class;
    }
}
