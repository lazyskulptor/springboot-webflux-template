package my.lazyskulptor.commerce.repo;

import my.lazyskulptor.commerce.model.Account;

public interface AccountCmdRepository extends CommandRepository<Long, Account>, SessionRepository {
}
