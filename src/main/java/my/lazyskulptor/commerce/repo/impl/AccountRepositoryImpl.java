package my.lazyskulptor.commerce.repo.impl;

import my.lazyskulptor.adapter.QueryTemplate;
import my.lazyskulptor.adapter.SessionDispatcher;
import my.lazyskulptor.commerce.model.Account;
import my.lazyskulptor.commerce.repo.AccountQueryRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
public class AccountRepositoryImpl extends QueryTemplate<Account, Long> implements AccountQueryRepository {
    public AccountRepositoryImpl(SessionDispatcher dispatcher) {
        super(Account.class, dispatcher);
    }
}
