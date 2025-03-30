package com.nttdata.account.msaccount.repository;

import com.nttdata.account.msaccount.model.AccountEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface AccountRepository extends ReactiveMongoRepository<AccountEntity, String> {

    Flux<AccountEntity> findByCustomerId(String customerId);
}
