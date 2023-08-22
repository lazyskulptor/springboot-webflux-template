package my.lazyskulptor.adapter.support;

import org.hibernate.reactive.mutiny.Mutiny;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.ResourceHolderSupport;

class HrsaSessionHolder extends ResourceHolderSupport {
    @Nullable
    private Mutiny.Session session;

    private boolean isTransactionActive;

    public HrsaSessionHolder(@Nullable Mutiny.Session session) {
        this(session, false);
    }

    public HrsaSessionHolder(@Nullable Mutiny.Session session, boolean isTransactionActive) {
        this.session = session;
        this.isTransactionActive = isTransactionActive;
    }

    @Nullable
    public Mutiny.Session getSession() {
        return session;
    }

    public void setSession(@Nullable Mutiny.Session session) {
        this.session = session;
    }

    public boolean isTransactionActive() {
        return isTransactionActive;
    }

    public void setTransactionActive(boolean transactionActive) {
        isTransactionActive = transactionActive;
    }
}
