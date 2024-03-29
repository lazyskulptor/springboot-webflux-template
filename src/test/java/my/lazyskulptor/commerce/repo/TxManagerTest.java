package my.lazyskulptor.commerce.repo;

import my.lazyskulptor.commerce.ContainerExtension;
import my.lazyskulptor.commerce.service.AccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.ReactiveTransactionManager;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@SpringBootTest
@ExtendWith(ContainerExtension.class)
public class TxManagerTest {

    @SpyBean
    ReactiveTransactionManager transactionManager;

    @MockBean
    AccountQueryRepository queryRepository;
    @Autowired
    AccountService service;

    @Test
    void verifyCommitIsCalled() {
        final var value = 1L;
        when(queryRepository.count(any())).thenReturn(Mono.just(value));
        var result = service.count().block();

        verify(transactionManager, times(1)).commit(any());
        verify(transactionManager, times(0)).rollback(any());

        assertThat(result).isEqualTo(value);
    }

    @Test
    void verifyRollbackIsCalled() {
        try {
            when(queryRepository.count(any())).thenThrow(new RuntimeException("Test Exception"));

            service.count().block();
        } catch (Exception e) {}

        verify(transactionManager, times(0)).commit(any());
        verify(transactionManager, times(1)).rollback(any());
    }
}
