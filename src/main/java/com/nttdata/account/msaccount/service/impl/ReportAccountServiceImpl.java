package com.nttdata.account.msaccount.service.impl;


import com.nttdata.account.msaccount.controller.AccountController;
import com.nttdata.account.msaccount.model.TransactionDTO;
import com.nttdata.account.msaccount.repository.TransactionRepository;
import com.nttdata.account.msaccount.service.ReportAccountService;
import lombok.RequiredArgsConstructor;

import org.openapitools.model.ReportOperationsRequest;
import org.openapitools.model.ReportOperationsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class ReportAccountServiceImpl implements ReportAccountService {
    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);
    private final TransactionRepository transactionRepository;

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
                .reduce(0.0, Double::sum)
                .map(totalAmount -> {
                    double averageDailyBalance = totalAmount / currentMonth.lengthOfMonth();
                    ReportOperationsResponse response = new ReportOperationsResponse();
                    response.setCustomerId(request);
                    response.setAmount(averageDailyBalance);
                    response.setReportDate(LocalDate.now());
                    return ResponseEntity.ok(response);
                });
    }

}
