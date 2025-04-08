package com.nttdata.account.msaccount.service;

import org.openapitools.model.TaxedTransactionLimit;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CommissionService {
    Mono<ResponseEntity<TaxedTransactionLimit>> upgradeCommission(String accountType,
                                                                 TaxedTransactionLimit taxedTransactionLimit);
    Mono<ResponseEntity<TaxedTransactionLimit>> createCommission(TaxedTransactionLimit taxedTransactionLimit);
    Mono<ResponseEntity<Void>> deleteCommission(String id);
    Mono<ResponseEntity<Flux<TaxedTransactionLimit>>> findAllCommission();
}

