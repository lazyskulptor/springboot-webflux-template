package my.lazyskulptor.commerce.repo.impl;

import my.lazyskulptor.commerce.model.Account;
import my.lazyskulptor.commerce.repo.AccountCmdRepository;
import org.hibernate.reactive.mutiny.Mutiny;

public class AccountCmdRepoImpl extends CmdTemplate<Long, Account> implements AccountCmdRepository {
    public AccountCmdRepoImpl(Mutiny.SessionFactory sessionFactory) {
        super(sessionFactory, Account.class);
    }
}
