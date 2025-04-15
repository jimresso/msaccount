package com.nttdata.account.msaccount.service.impl;

import com.nttdata.account.msaccount.exception.BusinessException;
import com.nttdata.account.msaccount.mapper.ComissionConverter;
import com.nttdata.account.msaccount.repository.ComissionRepository;
import com.nttdata.account.msaccount.service.CommissionService;
import lombok.RequiredArgsConstructor;
import org.openapitools.model.TaxedTransactionLimit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CommissionServiceImpl implements CommissionService {
    private final ComissionRepository comissionRepository;
    private final ComissionConverter comissionConverter;
    private static final Logger logger = LoggerFactory.getLogger(CommissionServiceImpl.class);

    @Override
    public Mono<ResponseEntity<Void>> deleteCommission(String id) {
        return comissionRepository.findById(id)
                .flatMap(existingAccount ->

                        comissionRepository.delete(existingAccount)
                        .doOnSuccess(unused -> logger.info("Account with ID {} successfully deleted", id))
                        .thenReturn(ResponseEntity.noContent().<Void>build()))
                .switchIfEmpty(Mono.fromRunnable(() -> logger.warn("Account with ID {} not found", id))
                        .then(Mono.just(ResponseEntity.notFound().<Void>build()))
                )
                .onErrorResume(e -> {
                    logger.error("Error deleting Account with ID {}: {}", id, e.getMessage());
                    return Mono.error(new Exception("Account not found"));
                });
    }



    @Override
    public Mono<ResponseEntity<TaxedTransactionLimit>> upgradeCommission(String accountType,
                                                                         TaxedTransactionLimit taxedTransactionLimit) {
        return comissionRepository.findByAccountType(accountType)
                .flatMap(existing -> {
                    if (existing != null) {
                        logger.warn("Commission already exists for accounttype: {}",
                                taxedTransactionLimit.getAccountType());
                        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body((TaxedTransactionLimit) null));
                    }
                    return comissionRepository.findAll()
                            .filter(c -> c.getAccountType().name().equalsIgnoreCase(accountType))
                            .next()
                            .flatMap(existingCommission -> {
                                existingCommission.setMonto(taxedTransactionLimit.getMonto());
                                return comissionRepository.save(existingCommission);
                            })
                            .map(comissionConverter::toDto)
                            .map(ResponseEntity::ok)
                            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
                })
                .onErrorResume(e -> {
                    logger.error("Error updating commission for accountType {}: ", accountType, e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(null));
                });
    }


    @Override
    public Mono<ResponseEntity<TaxedTransactionLimit>> createCommission(
            TaxedTransactionLimit taxedTransactionLimit) {
        return comissionRepository.findByAccountType(taxedTransactionLimit.getAccountType().name())
                .hasElement()
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        logger.warn("Commission already exists for account type: {}",
                                taxedTransactionLimit.getAccountType());
                        return Mono.just(
                                ResponseEntity
                                        .status(HttpStatus.BAD_REQUEST)
                                        .<TaxedTransactionLimit>body(null)
                        );
                    }
                    return Mono.just(taxedTransactionLimit)
                            .map(comissionConverter::toEntity)
                            .flatMap(comissionRepository::save)
                            .map(comissionConverter::toDto)
                            .map(ResponseEntity::ok);
                })
                .doOnError(e -> logger.error("Error creating commission: ", e))
                .onErrorResume(e -> {
                    logger.error("Exception while creating commission: ", e);
                    return Mono.just(
                            ResponseEntity
                                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .body(null)
                    );
                });
    }

    @Override
    public Mono<ResponseEntity<Flux<TaxedTransactionLimit>>> findAllCommission() {
        return comissionRepository.findAll()
                .map(comissionConverter::toDto)
                .collectList()
                .flatMap(accounts -> Mono.just(ResponseEntity.ok(Flux.fromIterable(accounts))))
                .onErrorResume(e -> {
                    logger.error("Error retrieving accounts: {}", e.getMessage());
                    return Mono.error(new BusinessException("An error occurred while retrieving accounts"));
                });
    }
}
