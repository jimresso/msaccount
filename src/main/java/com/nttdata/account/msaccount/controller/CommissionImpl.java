package com.nttdata.account.msaccount.controller;

import com.nttdata.account.msaccount.service.CommissionService;
import lombok.RequiredArgsConstructor;
import org.openapitools.api.CommissionApi;
import org.openapitools.model.TaxedTransactionLimit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class CommissionImpl implements CommissionApi {
    private final CommissionService commissionService;
    private static final Logger logger = LoggerFactory.getLogger(CommissionImpl.class);

    @Override
    public Mono<ResponseEntity<TaxedTransactionLimit>> commission(String accountType,
                                                                  Mono<TaxedTransactionLimit> taxedTransactionLimit,
                                                                  ServerWebExchange exchange) {
        logger.info("Starting update commission Id: {}", accountType);
        return taxedTransactionLimit.flatMap(a ->
                commissionService.upgradeCommission(accountType, a));
    }

    @Override
    public Mono<ResponseEntity<TaxedTransactionLimit>> createCommission(
            Mono<TaxedTransactionLimit> taxedTransactionLimit,
            ServerWebExchange exchange) {
        logger.info("Starting createCommission");
        return taxedTransactionLimit.flatMap(commissionService::createCommission);
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteCommission(String id, ServerWebExchange exchange) {
        logger.info("Starting delete Commission {}", id);
        return commissionService.deleteCommission(id);
    }

    @Override
    public Mono<ResponseEntity<Flux<TaxedTransactionLimit>>> getCommission(ServerWebExchange exchange) {
        logger.info("Starting find all Commission");
        return commissionService.findAllCommission();
    }
}
