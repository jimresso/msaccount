package com.nttdata.account.msaccount.repository;


import com.nttdata.account.msaccount.model.TaxedTransactionLimitDTO;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;


public interface ComissionRepository   extends ReactiveMongoRepository<TaxedTransactionLimitDTO, String> {

    Mono<TaxedTransactionLimitDTO> findByAccountType(String name);
}
