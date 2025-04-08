package com.nttdata.account.msaccount.service.impl;


import com.nttdata.account.msaccount.controller.AccountController;
import com.nttdata.account.msaccount.exception.BusinessException;
import com.nttdata.account.msaccount.model.TransactionDTO;
import com.nttdata.account.msaccount.repository.AccountRepository;
import com.nttdata.account.msaccount.repository.TransactionRepository;
import com.nttdata.account.msaccount.service.ReportAccountService;
import lombok.RequiredArgsConstructor;

import org.openapitools.model.ReportOperationsRequest;
import org.openapitools.model.ReportOperationsResponse;
import org.openapitools.model.ReportProductoRequest;
import org.openapitools.model.ReportProductoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class ReportAccountServiceImpl implements ReportAccountService {
    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    @Override
    public Mono<ResponseEntity<ReportOperationsResponse>> reportAccount(String request) {
        YearMonth currentMonth = YearMonth.now();
        LocalDate startOfMonth = currentMonth.atDay(1);
        LocalDate endOfMonth = currentMonth.atEndOfMonth();
        return transactionRepository.findByDni(request)
                .filter(transaction -> {
                    LocalDate txDate = transaction.getTransactionDate();
                    return !txDate.isBefore(startOfMonth) && !txDate.isAfter(endOfMonth);
                })
                .map(TransactionDTO::getAmount)
                .switchIfEmpty(Mono.error(new BusinessException("No transactions found for current month")))
                .reduce(0.0, Double::sum)
                .map(totalAmount -> {
                    double averageDailyBalance = totalAmount / currentMonth.lengthOfMonth();
                    ReportOperationsResponse response = new ReportOperationsResponse();
                    response.setDni(request);
                    response.setAmount(averageDailyBalance);
                    response.setReportDate(LocalDate.now());
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(ex -> {
                    if (ex instanceof BusinessException) {
                        logger.warn("Business error: {}", ex.getMessage());
                        return Mono.just(ResponseEntity.badRequest().build());
                    }
                    logger.error("Internal error generating the report: {}", ex.getMessage(), ex);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    @Override
    public Mono<ResponseEntity<Flux<ReportProductoResponse>>> reportProduct(ReportProductoRequest c) {
        LocalDate start = LocalDate.parse(c.getStartDate().toString());
        LocalDate end = LocalDate.parse(c.getEndDate().toString());
        return accountRepository.findAll()
                .flatMap(account -> {
                    String customerId = account.getCustomerId();
                    return transactionRepository.findByCustomerIdOrigin(customerId)
                            .filter(transaction -> {
                                LocalDate txDate = transaction.getTransactionDate();
                                return (txDate.isEqual(start) || txDate.isAfter(start)) &&
                                        (txDate.isEqual(end) || txDate.isBefore(end)) &&
                                        transaction.getCommissionAmount() > 0;
                            })
                            .map(transaction -> {
                                ReportProductoResponse response = new ReportProductoResponse();
                                response.setAccountType(account.getAccountType().name());
                                response.setCustomerId(transaction.getCustomerIdOrigin());
                                response.setCommissionAmount(transaction.getCommissionAmount());
                                return response;
                            });
                })
                .collectList()
                .map(responses -> {
                    if (responses.isEmpty()) {
                        logger.info("No commissionable transactions were found in the specified period");
                        Flux<ReportProductoResponse> emptyFlux = Flux.empty();
                        return ResponseEntity.ok(emptyFlux);
                    }
                    return ResponseEntity.ok(Flux.fromIterable(responses));
                })
                .onErrorResume(ex -> {
                    if (ex instanceof BusinessException) {
                        logger.warn("Business error: {}", ex.getMessage());
                        return Mono.just(ResponseEntity.badRequest().build());
                    }
                    logger.error("Internal error generating the report: {}", ex.getMessage(), ex);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }
}
