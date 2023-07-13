package my.lazyskulptor.commerce.repo.impl;

import my.lazyskulptor.adapter.CmdTemplate;
import my.lazyskulptor.adapter.SessionDispatcher;
import my.lazyskulptor.commerce.model.Account;
import my.lazyskulptor.commerce.repo.AccountCmdRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
public class AccountCmdRepoImpl extends CmdTemplate<Long, Account> implements AccountCmdRepository {
    public AccountCmdRepoImpl(SessionDispatcher dispatcher) {
        super(Account.class, dispatcher);
    }
}
