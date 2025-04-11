package com.nttdata.account.msaccount.controller;

import com.nttdata.account.msaccount.service.AccountService;
import com.nttdata.account.msaccount.service.CommissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.Account;
import org.openapitools.model.TaxedTransactionLimit;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommissionControllerTest {

    @Mock
    private CommissionService commissionService;

    @InjectMocks
    private CommissionController commissionController;

    @Test
    void testCommission_SuccessfulUpdate() {
        String accountType = "CORRIENTE";
        TaxedTransactionLimit input = new TaxedTransactionLimit();
        input.setMonto(10.0);
        Mono<TaxedTransactionLimit> inputMono = Mono.just(input);
        TaxedTransactionLimit updated = new TaxedTransactionLimit();
        updated.setMonto(15.0);
        ResponseEntity<TaxedTransactionLimit> responseEntity = ResponseEntity.ok(updated);
        doReturn(Mono.just(responseEntity))
                .when(commissionService)
                .upgradeCommission(eq(accountType), any(TaxedTransactionLimit.class));
        Mono<ResponseEntity<TaxedTransactionLimit>> result = commissionController.commission(accountType, inputMono, null);
        StepVerifier.create(result)
                .expectNextMatches(response ->
                        response.getStatusCode() == HttpStatus.OK &&
                                response.getBody() != null &&
                                response.getBody().getMonto() == 15.0)
                .verifyComplete();
        verify(commissionService).upgradeCommission(eq(accountType), any(TaxedTransactionLimit.class));
    }
//    @Test
//    void createCommission() {
//    }
//
//    @Test
//    void deleteCommission() {
//    }
//
//    @Test
//    void getCommission() {
//    }
}