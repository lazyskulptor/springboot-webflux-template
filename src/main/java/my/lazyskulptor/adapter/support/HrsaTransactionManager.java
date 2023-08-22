package my.lazyskulptor.adapter.support;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.uni.UniReactorConverters;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.mutiny.impl.MutinySessionImpl;
import org.hibernate.reactive.pool.ReactiveConnection;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.reactive.AbstractReactiveTransactionManager;
import org.springframework.transaction.reactive.GenericReactiveTransaction;
import org.springframework.transaction.reactive.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Function;

public class HrsaTransactionManager extends AbstractReactiveTransactionManager implements InitializingBean, SessionDispatcher {

    private Mutiny.SessionFactory sessionFactory;

    public HrsaTransactionManager(@NonNull Mutiny.SessionFactory sessionFactory) {
        setSessionFactory(sessionFactory);
        afterPropertiesSet();
    }

    @Override
    public void afterPropertiesSet() {
        if (getSessionFactory() == null) {
            throw new IllegalArgumentException("Property 'connectionFactory' is required");
        }
    }

    @Override
    public <R> Mono<R> apply(Function<Mutiny.Session, Uni<R>> work) {
        return TransactionSynchronizationManager.forCurrentTransaction()
                .map(syncManager -> (HrsaSessionHolder)syncManager.getResource(sessionFactory))
                .map(HrsaSessionHolder::getSession)
                .map(work)
                .switchIfEmpty(Mono.just(obtainSessionFactory()
                        .withSession(session -> work.apply(session).call(session::flush)
                                .log("LOCAL SESSION[" + session.hashCode() + "]"))))
                .flatMap(uni -> uni.convert().with(UniReactorConverters.toMono()));
    }

    @Override
    protected Object doGetTransaction(TransactionSynchronizationManager synchronizationManager) throws TransactionException {
        SessionFactoryTransactionObject txObject = new SessionFactoryTransactionObject();
        HrsaSessionHolder holder = (HrsaSessionHolder) synchronizationManager.getResource(obtainSessionFactory());
        txObject.setSessionHolder(holder, false);
        return txObject;
    }

    @Override
    protected boolean isExistingTransaction(Object transaction) throws TransactionException {
        SessionFactoryTransactionObject txObject = (SessionFactoryTransactionObject)transaction;
        return txObject.hasSessionHolder() && txObject.getSessionHolder().isTransactionActive();
    }

    @Override
    protected Mono<Void> doBegin(TransactionSynchronizationManager synchronizationManager, Object transaction, TransactionDefinition definition) throws TransactionException {
        SessionFactoryTransactionObject txObject = (SessionFactoryTransactionObject) transaction;
        return Mono.defer(() -> {
            Uni<Mutiny.Session> sessionUni;

            if(!txObject.hasSessionHolder() || txObject.getSessionHolder().isSynchronizedWithTransaction()) {
                var newConn = getSessionFactory().openSession();
                sessionUni = newConn.invoke(sess -> {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Acquired Session [" + newConn + "] for Hibernate Reactive transaction");
                    }
                    txObject.setSessionHolder(new HrsaSessionHolder(sess), true);
                });
            } else {
                txObject.getSessionHolder().setSynchronizedWithTransaction(true);
                sessionUni = Uni.createFrom().item(txObject.getSessionHolder().getSession());
            }
            return sessionUni.chain(session -> {
                var sessImpl = (MutinySessionImpl) session;
                var conn = sessImpl.getReactiveConnection();

                return prepareTransactionalConnection(conn, definition, transaction)
                        .replaceWith(Uni.createFrom().completionStage(conn.beginTransaction()))
                        .invoke(() -> {
                            txObject.getSessionHolder().setTransactionActive(true);
                            Duration timeout = determineTimeout(definition);
                            if (!timeout.isNegative() && !timeout.isZero()) {
                                txObject.getSessionHolder().setTimeoutInMillis(timeout.toMillis());
                            }
                            // Bind the connection holder to the thread.
                            if (txObject.isNewSessionHolder()) {
                                synchronizationManager.bindResource(obtainSessionFactory(), txObject.getSessionHolder());
                            }
                        })
                        .replaceWith(session)
                        .onFailure().call(() -> {
                            if (txObject.isNewSessionHolder()) {
                                return sessImpl.close()
                                        .eventually(() -> txObject.setSessionHolder(null, false));
                            }
                            return Uni.createFrom().voidItem();
                        });
            }).convert().with(UniReactorConverters.toMono())
                    .onErrorResume(e -> {
                        CannotCreateTransactionException ex = new CannotCreateTransactionException(
                                "Could not open Hibernate Reactive Mutiny.Session for transaction", e);
                        return Mono.error(ex);
                    });
        }).then();
    }

    protected Uni<Void> prepareTransactionalConnection(ReactiveConnection con, TransactionDefinition definition, Object transaction) {
        SessionFactoryTransactionObject txObejct = (SessionFactoryTransactionObject) transaction;
        if (definition.isReadOnly()) {
            // TODO: set ReadONly
        }
        // TODO: set Isolation Level;
        return Uni.createFrom().voidItem();
    }

    protected Duration determineTimeout(TransactionDefinition definition) {
        if (definition.getTimeout() != TransactionDefinition.TIMEOUT_DEFAULT) {
            return Duration.ofSeconds(definition.getTimeout());
        }
        return Duration.ZERO;
    }

    @Override
    protected Mono<Void> doCommit(TransactionSynchronizationManager synchronizationManager, GenericReactiveTransaction status) throws TransactionException {
        SessionFactoryTransactionObject txObject = (SessionFactoryTransactionObject) status.getTransaction();
        Mutiny.Session session = txObject.getSessionHolder().getSession();
        if (status.isDebug()) {
            logger.debug("Committing Hibernate Reactive transaction on Mutiny.Session [" + session + "]");
        }
        var ssImpl = (MutinySessionImpl) txObject.getSessionHolder().getSession();
        return Uni.createFrom().completionStage(ssImpl.getReactiveConnection().commitTransaction())
                .convert().with(UniReactorConverters.toMono())
                .onErrorMap(RuntimeException.class, ex -> new RuntimeException("Hibernate Commit", ex));
    }

    @Override
    protected Mono<Void> doRollback(TransactionSynchronizationManager synchronizationManager, GenericReactiveTransaction status) throws TransactionException {
        SessionFactoryTransactionObject txObject = (SessionFactoryTransactionObject) status.getTransaction();
        var ssImpl = (MutinySessionImpl) txObject.getSessionHolder().getSession();
        return Uni.createFrom().completionStage(ssImpl.getReactiveConnection().rollbackTransaction())
                .convert().with(UniReactorConverters.toMono())
                .onErrorMap(RuntimeException.class, ex -> new RuntimeException("Hibernate rollback", ex));
    }

    public void setSessionFactory(@NonNull Mutiny.SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @NonNull
    @Override
    public Mutiny.SessionFactory getSessionFactory() {
        return this.sessionFactory;
    }

    public Mutiny.SessionFactory obtainSessionFactory() {
        Mutiny.SessionFactory factory = getSessionFactory();
        Assert.state(factory != null, "No SessionFactory set");
        return factory;
    }

    /**
     * SessionFactory transaction object, representing a SessionHolder.
     * Used as transaction object by CustomReactiveTransactionManager.
     */
    private static class SessionFactoryTransactionObject {

        @Nullable
        private HrsaSessionHolder sessionHolder;

        @Nullable
        private int previousIsolationLevel;

        private boolean newSessionHolder;

        void setSessionHolder(@Nullable HrsaSessionHolder sessionHolder, boolean newSessionHolder) {
            setSessionHolder(sessionHolder);
            this.newSessionHolder = newSessionHolder;
        }

        boolean isNewSessionHolder() {
            return this.newSessionHolder;
        }

        void setRollbackOnly() {
            getSessionHolder().setRollbackOnly();
        }

        public void setSessionHolder(@Nullable HrsaSessionHolder sessionHolder) {
            this.sessionHolder = sessionHolder;
        }

        public HrsaSessionHolder getSessionHolder() {
            Assert.state(this.sessionHolder != null, "No SessionHolder available");
            return this.sessionHolder;
        }

        public boolean hasSessionHolder() {
            return (this.sessionHolder != null);
        }

        public void setPreviousIsolationLevel(@Nullable int previousIsolationLevel) {
            this.previousIsolationLevel = previousIsolationLevel;
        }

        public int getPreviousIsolationLevel() {
            return this.previousIsolationLevel;
        }
    }
}
