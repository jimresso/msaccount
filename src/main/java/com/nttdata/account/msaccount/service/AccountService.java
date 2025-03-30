package com.nttdata.account.msaccount.service;

import org.openapitools.model.Account;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AccountService {
    Mono<ResponseEntity<Account>> findAccountById(String id);
    Mono<ResponseEntity<Account>> newAccount(Account account);
    Mono<ResponseEntity<Void>> removeAccount(String id);
    Mono<ResponseEntity<Account>> upgradeAccount(String id, Account account);
    Mono<ResponseEntity<Flux<Account>>> listAccounts();

}
