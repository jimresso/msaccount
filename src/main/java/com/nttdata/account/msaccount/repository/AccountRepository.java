package com.nttdata.account.msaccount.repository;

import com.nttdata.account.msaccount.model.AccountEntityDTO;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AccountRepository extends ReactiveMongoRepository<AccountEntityDTO, String> {

    Flux<AccountEntityDTO> findByCustomerId(String customerId);
    Flux<AccountEntityDTO> findByDni(String dni);
    Mono<AccountEntityDTO> findFirstByCustomerId(String customerId);
}
