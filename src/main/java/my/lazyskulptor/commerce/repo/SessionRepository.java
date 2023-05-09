package my.lazyskulptor.commerce.repo;

import org.hibernate.reactive.mutiny.Mutiny;

public interface SessionRepository {
    Mutiny.SessionFactory getSessionFactory();
}
