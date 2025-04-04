package com.nttdata.account.msaccount.controller;

import com.nttdata.account.msaccount.service.AccountService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.Account;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
@Disabled
class AccountControllerTest {
    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountController accountController;

    @Test
    void testGetAccountById() {
        String accountId = "123";
        Account mockAccount = new Account();
        mockAccount.setId(accountId);
        ResponseEntity<Account> responseEntity = ResponseEntity.ok(mockAccount);
        when(accountService.findAccountById(accountId)).thenReturn(Mono.just(responseEntity));
        StepVerifier.create(accountController.getAccountById(accountId, null))
                .expectNext(responseEntity)
                .verifyComplete();
        verify(accountService).findAccountById(accountId);
    }
    @Test
    void testGetAllAccounts() {
        Account account1 = new Account();
        account1.setId("123");
        Account account2 = new Account();
        account2.setId("456");
        Flux<Account> accountsFlux = Flux.just(account1, account2);
        ResponseEntity<Flux<Account>> responseEntity = ResponseEntity.ok(accountsFlux);
        when(accountService.listAccounts()).thenReturn(Mono.just(responseEntity));
        StepVerifier.create(accountController.getAllAccounts(null))
                .expectNext(responseEntity)
                .verifyComplete();
        verify(accountService).listAccounts();
    }
    @Test
    void testCreateAccount() {
        Account account = new Account();
        account.setId("789");
        ResponseEntity<Account> responseEntity = ResponseEntity.status(HttpStatus.CREATED).body(account);
        when(accountService.newAccount(any(Account.class))).thenReturn(Mono.just(responseEntity));
        StepVerifier.create(accountController.createAccount(Mono.just(account), null))
                .expectNext(responseEntity)
                .verifyComplete();
        verify(accountService).newAccount(any(Account.class));
    }
    @Test
    void testUpdateAccount() {
        String accountId = "123";
        Account updatedAccount = new Account();
        updatedAccount.setId(accountId);
        ResponseEntity<Account> responseEntity = ResponseEntity.ok(updatedAccount);
        when(accountService.upgradeAccount(eq(accountId), any(Account.class))).thenReturn(Mono.just(responseEntity));
        StepVerifier.create(accountController.updateAccount(accountId, Mono.just(updatedAccount), null))
                .expectNext(responseEntity)
                .verifyComplete();
        verify(accountService).upgradeAccount(eq(accountId), any(Account.class));
    }
    @Test
    void testDeleteAccount() {
        String accountId = "123";
        ResponseEntity<Void> responseEntity = ResponseEntity.noContent().build();
        when(accountService.removeAccount(accountId)).thenReturn(Mono.just(responseEntity));
        StepVerifier.create(accountController.deleteAccount(accountId, null))
                .expectNext(responseEntity)
                .verifyComplete();
        verify(accountService).removeAccount(accountId);
    }


}