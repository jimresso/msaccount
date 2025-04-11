package com.nttdata.account.msaccount.controller;

import com.nttdata.account.msaccount.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.openapitools.api.ApiApi;
import org.openapitools.model.Account;
import org.openapitools.model.DepositRequest;
import org.openapitools.model.WithdrawRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class AccountController implements ApiApi {
    private final AccountService accountService;
    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);
    @Override
    public Mono<ResponseEntity<Flux<Account>>> getAllAccounts(ServerWebExchange exchange) {
        logger.info("Starting get find all Clients");
        return accountService.listAccounts();
    }

    @Override
    public Mono<ResponseEntity<Account>> getAccountById(String id, ServerWebExchange exchange) {
        logger.info("Starting get for Account ID:{}", id);
        return accountService.findAccountById(id);
    }
    @Override
    public Mono<ResponseEntity<Account>> createAccount(Mono<Account> account, ServerWebExchange exchange) {
        logger.info("Starting createAccount");
        return account.flatMap(accountService::newAccount);
    }
    @Override
    public Mono<ResponseEntity<Account>> updateAccount(String id, Mono<Account> account, ServerWebExchange exchange) {
        logger.info("Starting updateAccount Id: {}", id);
        return account.flatMap(a -> accountService.upgradeAccount(id, a));
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteAccount(String id, ServerWebExchange exchange) {
        logger.info("Starting deleteAccount Id: {}", id);
        return accountService.removeAccount(id);
    }
    @Override
    public Mono<ResponseEntity<Account>> deposit(String id , Mono<DepositRequest> amount, ServerWebExchange exchange) {
        logger.info("Starting deposit Id: {}", id);
        return amount.flatMap(a -> accountService.depositAmount(id, a));
    }

    @Override
    public Mono<ResponseEntity<Account>> withdraw(String customerId ,
                                                  Mono<WithdrawRequest> amount,
                                                  ServerWebExchange exchange) {
        logger.info("Starting withdraw Id: {}", customerId);
        return amount.flatMap(a -> accountService.withdrawAmount(customerId, a));
    }


}
