package my.lazyskulptor.commerce.service;

import my.lazyskulptor.commerce.repo.AccountQueryRepository;
import my.lazyskulptor.commerce.spec.Logic;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Transactional
@Service
public class AccountService {
    private final AccountQueryRepository queryRepository;

    public AccountService(AccountQueryRepository queryRepository) {
        this.queryRepository = queryRepository;
    }

    public Mono<Long> count() {
        return queryRepository.count(Logic.TRUE);
    }
}
