package my.lazyskulptor.commerce.repo.impl;

import my.lazyskulptor.commerce.model.Account;
import my.lazyskulptor.commerce.repo.AccountQueryRepository;
import org.hibernate.reactive.mutiny.Mutiny;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRepositoryImpl extends QueryTemplate<Account> implements AccountQueryRepository {
    public AccountRepositoryImpl(Mutiny.SessionFactory sessionFactory) {
        super(sessionFactory, Account.class);
    }
}
