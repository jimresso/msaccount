package com.nttdata.account.msaccount.service.impl;

import com.nttdata.account.msaccount.exception.BusinessException;
import com.nttdata.account.msaccount.exception.EntityNotFoundException;
import com.nttdata.account.msaccount.exception.InternalServerErrorException;
import com.nttdata.account.msaccount.mapper.AccountConverter;
import com.nttdata.account.msaccount.model.AccountEntity;
import com.nttdata.account.msaccount.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.Account;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Spy
    @InjectMocks
    private AccountServiceImpl accountService;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountConverter accountConverter;
    @Mock
    private Logger logger;

    @Test
    void findAccountById_WhenAccountExists() {
        String accountId = "123";
        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setId(accountId);
        Account accountDto = new Account();
        accountDto.setId(accountId);
        when(accountRepository.findById(accountId)).thenReturn(Mono.just(accountEntity));
        when(accountConverter.toDto(accountEntity)).thenReturn(accountDto);
        StepVerifier.create(accountService.findAccountById(accountId))
                .expectNext(ResponseEntity.ok(accountDto))
                .verifyComplete();
        verify(accountRepository).findById(accountId);
        verify(accountConverter).toDto(accountEntity);
    }

    @Test
    void findAccountById_WhenAccountDoesNotExist() {
        String accountId = "123";
        when(accountRepository.findById(accountId)).thenReturn(Mono.empty());
        StepVerifier.create(accountService.findAccountById(accountId))
                .expectErrorMatches(throwable -> throwable instanceof EntityNotFoundException &&
                        throwable.getMessage().equals("Failed to retrieve the account with ID: " + accountId))
                .verify();
        verify(accountRepository).findById(accountId);
    }

    @Test
    void findAccountById_WhenRepositoryFails() {
        String accountId = "123";
        when(accountRepository.findById(accountId)).thenReturn(Mono.error(new RuntimeException("Database error")));
        StepVerifier.create(accountService.findAccountById(accountId))
                .expectErrorMatches(throwable -> throwable instanceof InternalServerErrorException &&
                        throwable.getMessage().equals("Unexpected error occurred while retrieving account"))
                .verify();
        verify(accountRepository).findById(accountId);
    }

    @Test
    void newAccount_WhenValidAccountPersonal() {
        Account account = new Account()
                .customerId("123")
                .customerType(Account.CustomerTypeEnum.PERSONAL)
                .accountType(Account.AccountTypeEnum.AHORRO);
        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setCustomerId("123");
        accountEntity.setCustomerType(AccountEntity.CustomerType.PERSONAL);
        accountEntity.setAccountType(AccountEntity.AccountType.AHORRO);
        Account savedAccount = new Account()
                .customerId("123")
                .customerType(Account.CustomerTypeEnum.PERSONAL)
                .accountType(Account.AccountTypeEnum.AHORRO);
        when(accountConverter.toEntity(account)).thenReturn(accountEntity);
        when(accountRepository.findByCustomerId("123")).thenReturn(Flux.empty());
        when(accountRepository.save(accountEntity)).thenReturn(Mono.just(accountEntity));
        when(accountConverter.toDto(accountEntity)).thenReturn(savedAccount);
        StepVerifier.create(accountService.newAccount(account))
                .expectNextMatches(response -> response.getStatusCode() == HttpStatus.CREATED &&
                        response.getBody().equals(savedAccount))
                .verifyComplete();
        verify(accountRepository).save(accountEntity);
        verify(accountRepository).findByCustomerId("123");
    }

    @Test
    void newAccount_WhenCustomerIsEmpresarial() {
        Account account = new Account()
                .customerId("456")
                .customerType(Account.CustomerTypeEnum.EMPRESARIAL)
                .accountType(Account.AccountTypeEnum.CORRIENTE)
                .holders(List.of("holder1", "holder2"));
        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setCustomerId("456");
        accountEntity.setCustomerType(AccountEntity.CustomerType.EMPRESARIAL);
        accountEntity.setAccountType(AccountEntity.AccountType.CORRIENTE);
        accountEntity.setHolders(List.of("holder1", "holder2"));
        Account savedAccount = new Account()
                .customerId("456")
                .customerType(Account.CustomerTypeEnum.EMPRESARIAL)
                .accountType(Account.AccountTypeEnum.CORRIENTE)
                .holders(List.of("holder1", "holder2"));
        when(accountConverter.toEntity(account)).thenReturn(accountEntity);
        when(accountRepository.findByCustomerId("456")).thenReturn(Flux.empty());
        when(accountRepository.save(accountEntity)).thenReturn(Mono.just(accountEntity));
        when(accountConverter.toDto(accountEntity)).thenReturn(savedAccount);
        StepVerifier.create(accountService.newAccount(account))
                .expectNextMatches(response -> response.getStatusCode() == HttpStatus.CREATED &&
                        response.getBody().equals(savedAccount))
                .verifyComplete();
        verify(accountRepository).save(accountEntity);
    }

    @Test
    void newAccount_WhenValidPlazoFijoForPersonal() {
        Account account = new Account()
                .customerId("789")
                .customerType(Account.CustomerTypeEnum.PERSONAL)
                .accountType(Account.AccountTypeEnum.PLAZO_FIJO);
        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setCustomerId("789");
        accountEntity.setCustomerType(AccountEntity.CustomerType.PERSONAL);
        accountEntity.setAccountType(AccountEntity.AccountType.PLAZO_FIJO);
        Account savedAccount = new Account()
                .customerId("789")
                .customerType(Account.CustomerTypeEnum.PERSONAL)
                .accountType(Account.AccountTypeEnum.PLAZO_FIJO);
        when(accountConverter.toEntity(account)).thenReturn(accountEntity);
        when(accountRepository.findByCustomerId("789")).thenReturn(Flux.empty());
        when(accountRepository.save(accountEntity)).thenReturn(Mono.just(accountEntity));
        when(accountConverter.toDto(accountEntity)).thenReturn(savedAccount);
        StepVerifier.create(accountService.newAccount(account))
                .expectNextMatches(response -> response.getStatusCode() == HttpStatus.CREATED &&
                        response.getBody().equals(savedAccount))
                .verifyComplete();
        verify(accountRepository).save(accountEntity);
    }

    @Test
    void newAccount_WhenInvalidPlazoFijoForEmpresarial() {
        Account account = new Account()
                .customerId("999")
                .customerType(Account.CustomerTypeEnum.EMPRESARIAL)
                .accountType(Account.AccountTypeEnum.PLAZO_FIJO);
        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setCustomerId("999");
        accountEntity.setCustomerType(AccountEntity.CustomerType.EMPRESARIAL);
        accountEntity.setAccountType(AccountEntity.AccountType.PLAZO_FIJO);
        when(accountConverter.toEntity(account)).thenReturn(accountEntity);
        when(accountRepository.findByCustomerId("999")).thenReturn(Flux.empty());
        StepVerifier.create(accountService.newAccount(account))
                .expectErrorMatches(throwable ->
                        throwable instanceof BusinessException &&
                                throwable.getMessage().equals("Account creation does not meet business rules"))
                .verify();
        verify(accountRepository, never()).save(any());
    }

    @Test
    void newAccount_ThrowsInternalServerErrorException() {
        Account account = new Account()
                .customerId("456")
                .customerType(Account.CustomerTypeEnum.PERSONAL)
                .accountType(Account.AccountTypeEnum.AHORRO);
        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setCustomerId("456");
        accountEntity.setCustomerType(AccountEntity.CustomerType.PERSONAL);
        accountEntity.setAccountType(AccountEntity.AccountType.AHORRO);
        when(accountConverter.toEntity(account)).thenReturn(accountEntity);
        when(accountRepository.findByCustomerId("456")).thenReturn(Flux.empty());
        when(accountRepository.save(accountEntity)).thenReturn(Mono.error(new RuntimeException("Database error")));
        StepVerifier.create(accountService.newAccount(account))
                .expectError(InternalServerErrorException.class)
                .verify();
        verify(accountRepository).save(accountEntity);
    }

    @Test
    void removeAccount_ReturnsNoContent() {
        String accountId = "123";
        AccountEntity existingAccount = new AccountEntity();
        existingAccount.setId(accountId);
        when(accountRepository.findById(accountId)).thenReturn(Mono.just(existingAccount));
        when(accountRepository.delete(existingAccount)).thenReturn(Mono.empty());
        StepVerifier.create(accountService.removeAccount(accountId))
                .expectNextMatches(response -> response.getStatusCode() == HttpStatus.NO_CONTENT)
                .verifyComplete();
        verify(accountRepository).findById(accountId);
        verify(accountRepository).delete(existingAccount);
    }

    @Test
    void removeAccount_ReturnsNotFound() {
        String accountId = "456";
        when(accountRepository.findById(accountId)).thenReturn(Mono.empty());
        StepVerifier.create(accountService.removeAccount(accountId))
                .expectNextMatches(response -> response.getStatusCode() == HttpStatus.NOT_FOUND)
                .verifyComplete();
        verify(accountRepository).findById(accountId);
        verify(accountRepository, never()).delete(any());
    }

    @Test
    void removeAccount_ThrowsException() {
        String accountId = "789";
        AccountEntity existingAccount = new AccountEntity();
        existingAccount.setId(accountId);
        when(accountRepository.findById(accountId)).thenReturn(Mono.just(existingAccount));
        when(accountRepository.delete(existingAccount)).thenReturn(Mono.error(new RuntimeException("Database error")));
        StepVerifier.create(accountService.removeAccount(accountId))
                .expectErrorMatches(e -> e instanceof Exception && e.getMessage().equals("Account not found"))
                .verify();
        verify(accountRepository).findById(accountId);
        verify(accountRepository).delete(existingAccount);
    }

    @Test
    void listAccounts_ReturnsList() {
        AccountEntity accountEntity1 = new AccountEntity();
        accountEntity1.setId("1");
        accountEntity1.setCustomerId("123");
        accountEntity1.setAccountType(AccountEntity.AccountType.AHORRO);
        AccountEntity accountEntity2 = new AccountEntity();
        accountEntity2.setId("2");
        accountEntity2.setCustomerId("456");
        accountEntity2.setAccountType(AccountEntity.AccountType.CORRIENTE);
        Account accountDto1 = new Account().id("1").customerId("123").accountType(Account.AccountTypeEnum.AHORRO);
        Account accountDto2 = new Account().id("2").customerId("456").accountType(Account.AccountTypeEnum.CORRIENTE);
        when(accountRepository.findAll()).thenReturn(Flux.just(accountEntity1, accountEntity2));
        when(accountConverter.toDto(accountEntity1)).thenReturn(accountDto1);
        when(accountConverter.toDto(accountEntity2)).thenReturn(accountDto2);
        StepVerifier.create(accountService.listAccounts())
                .expectNextMatches(response -> response.getStatusCode() == HttpStatus.OK)
                .verifyComplete();
        verify(accountRepository).findAll();
        verify(accountConverter, times(2)).toDto(any());
    }

    @Test
    void listAccounts_ReturnsEmptyList() {
        when(accountRepository.findAll()).thenReturn(Flux.empty());
        StepVerifier.create(accountService.listAccounts())
                .expectNextMatches(response -> response.getStatusCode() == HttpStatus.OK)
                .verifyComplete();
        verify(accountRepository).findAll();
        verify(accountConverter, never()).toDto(any());
    }

    @Test
    void listAccounts_ThrowsBusinessException() {
        when(accountRepository.findAll()).thenReturn(Flux.error(new RuntimeException("Database error")));
        StepVerifier.create(accountService.listAccounts())
                .expectErrorMatches(e -> e instanceof BusinessException &&
                        e.getMessage().equals("An error occurred while retrieving accounts"))
                .verify();
        verify(accountRepository).findAll();
    }

    @Test
    void upgradeAccount_ShouldReturnUpdatedAccount() {
        String accountId = "123";
        String customerId = "456";
        Account updatedAccount = new Account();
        updatedAccount.setAccountType(Account.AccountTypeEnum.AHORRO);
        AccountEntity existingEntity = new AccountEntity();
        existingEntity.setId(accountId);
        existingEntity.setCustomerId(customerId);
        existingEntity.setCustomerType(AccountEntity.CustomerType.PERSONAL);
        existingEntity.setAccountType(AccountEntity.AccountType.AHORRO);
        AccountEntity updatedEntity = new AccountEntity();
        updatedEntity.setId(accountId);
        updatedEntity.setCustomerId(customerId);
        updatedEntity.setCustomerType(AccountEntity.CustomerType.PERSONAL);
        updatedEntity.setAccountType(AccountEntity.AccountType.AHORRO);
        Account updatedDto = new Account();
        updatedDto.setAccountType(Account.AccountTypeEnum.AHORRO);
        when(accountRepository.findById(accountId)).thenReturn(Mono.just(existingEntity));
        when(accountConverter.toEntity(updatedAccount)).thenReturn(updatedEntity);
        when(accountRepository.save(updatedEntity)).thenReturn(Mono.just(updatedEntity));
        when(accountConverter.toDto(updatedEntity)).thenReturn(updatedDto);
        StepVerifier.create(accountService.upgradeAccount(accountId, updatedAccount))
                .expectNextMatches(response ->
                        response.getStatusCode() == HttpStatus.OK && response.getBody().equals(updatedDto)
                )
                .verifyComplete();
    }

    @Test
    void upgradeAccount_ShouldThrowBusinessException() {
        String accountId = "123";
        String customerId = "456";
        Account updatedAccount = new Account();
        updatedAccount.setAccountType(Account.AccountTypeEnum.AHORRO);
        AccountEntity existingEntity = new AccountEntity();
        existingEntity.setId(accountId);
        existingEntity.setCustomerId(customerId);
        existingEntity.setCustomerType(AccountEntity.CustomerType.PERSONAL);
        existingEntity.setAccountType(AccountEntity.AccountType.CORRIENTE);
        AccountEntity updatedEntity = new AccountEntity();
        updatedEntity.setId(accountId);
        updatedEntity.setCustomerId(customerId);
        updatedEntity.setCustomerType(AccountEntity.CustomerType.PERSONAL);
        updatedEntity.setAccountType(AccountEntity.AccountType.AHORRO);
        AccountEntity anotherExistingAccount = new AccountEntity();
        anotherExistingAccount.setId("789");
        anotherExistingAccount.setCustomerId(customerId);
        anotherExistingAccount.setCustomerType(AccountEntity.CustomerType.PERSONAL);
        anotherExistingAccount.setAccountType(AccountEntity.AccountType.AHORRO);
        when(accountRepository.findById(accountId)).thenReturn(Mono.just(existingEntity));
        when(accountConverter.toEntity(updatedAccount)).thenReturn(updatedEntity);
        when(accountRepository.findByCustomerId(customerId)).thenReturn(Flux.just(anotherExistingAccount));
        StepVerifier.create(accountService.upgradeAccount(accountId, updatedAccount))
                .expectErrorMatches(e -> e instanceof BusinessException &&
                        e.getMessage().equals("Account update does not meet business rules"))
                .verify();
        verify(accountRepository).findById(accountId);
        verify(accountRepository).findByCustomerId(customerId);
        verifyNoMoreInteractions(accountRepository);
    }

    @Test
    void upgradeAccount_ShouldThrowEntityNotFoundException() {
        String accountId = "123";
        Account updatedAccount = new Account();
        updatedAccount.setAccountType(Account.AccountTypeEnum.AHORRO);
        when(accountRepository.findById(accountId)).thenReturn(Mono.empty());
        StepVerifier.create(accountService.upgradeAccount(accountId, updatedAccount))
                .expectErrorMatches(e -> e instanceof EntityNotFoundException &&
                        e.getMessage().equals("Account not found"))
                .verify();
        verify(accountRepository).findById(accountId);
        verifyNoMoreInteractions(accountRepository);
    }

    @Test
    void upgradeAccount_WhenUnexpectedErrorOccurs_ShouldThrowInternalServerErrorException() {
        String accountId = "123";
        Account updatedAccount = new Account();
        updatedAccount.setAccountType(Account.AccountTypeEnum.AHORRO);
        when(accountRepository.findById(accountId)).thenReturn(Mono.error(new RuntimeException("Unexpected database error")));
        StepVerifier.create(accountService.upgradeAccount(accountId, updatedAccount))
                .expectErrorMatches(e -> e instanceof InternalServerErrorException &&
                        e.getMessage().equals("Unexpected error updating account"))
                .verify();
        verify(accountRepository).findById(accountId);
        verifyNoMoreInteractions(accountRepository);
    }

    @Test
    void upgradeAccount_WhenUpdatingToPlazoFijo() {
        String accountId = "123";
        String customerId = "456";
        AccountEntity existingEntity = new AccountEntity();
        existingEntity.setId(accountId);
        existingEntity.setCustomerId(customerId);
        existingEntity.setCustomerType(AccountEntity.CustomerType.PERSONAL);
        existingEntity.setAccountType(AccountEntity.AccountType.AHORRO);
        Account updatedAccount = new Account();
        updatedAccount.setAccountType(Account.AccountTypeEnum.PLAZO_FIJO);
        AccountEntity updatedEntity = new AccountEntity();
        updatedEntity.setId(accountId);
        updatedEntity.setCustomerId(customerId);
        updatedEntity.setCustomerType(AccountEntity.CustomerType.PERSONAL);
        updatedEntity.setAccountType(AccountEntity.AccountType.PLAZO_FIJO);
        Account updatedDto = new Account();
        updatedDto.setAccountType(Account.AccountTypeEnum.PLAZO_FIJO);
        when(accountRepository.findById(accountId)).thenReturn(Mono.just(existingEntity));
        when(accountRepository.findByCustomerId(customerId)).thenReturn(Flux.just(existingEntity));
        when(accountConverter.toEntity(updatedAccount)).thenReturn(updatedEntity);
        when(accountRepository.save(updatedEntity)).thenReturn(Mono.just(updatedEntity));
        when(accountConverter.toDto(updatedEntity)).thenReturn(updatedDto);
        StepVerifier.create(accountService.upgradeAccount(accountId, updatedAccount))
                .expectNextMatches(response ->
                        response.getStatusCode() == HttpStatus.OK && response.getBody().equals(updatedDto)
                )
                .verifyComplete();
    }
    @Test
    void upgradeAccount_WhenUpdatingToCorrienteForEmpresarial_ShouldSucceed() {
        String accountId = "123";
        String customerId = "456";
        AccountEntity existingEntity = new AccountEntity();
        existingEntity.setId(accountId);
        existingEntity.setCustomerId(customerId);
        existingEntity.setCustomerType(AccountEntity.CustomerType.EMPRESARIAL);
        existingEntity.setAccountType(AccountEntity.AccountType.AHORRO);
        existingEntity.setHolders(Collections.singletonList("holder1")); // Tiene titulares
        Account updatedAccount = new Account();
        updatedAccount.setAccountType(Account.AccountTypeEnum.CORRIENTE);
        AccountEntity updatedEntity = new AccountEntity();
        updatedEntity.setId(accountId);
        updatedEntity.setCustomerId(customerId);
        updatedEntity.setCustomerType(AccountEntity.CustomerType.EMPRESARIAL);
        updatedEntity.setAccountType(AccountEntity.AccountType.CORRIENTE);
        updatedEntity.setHolders(Collections.singletonList("holder1")); // Mantiene titulares
        Account updatedDto = new Account();
        updatedDto.setAccountType(Account.AccountTypeEnum.CORRIENTE);
        when(accountRepository.findById(accountId)).thenReturn(Mono.just(existingEntity));
        when(accountRepository.findByCustomerId(customerId)).thenReturn(Flux.just(existingEntity)); // Simula otras cuentas del cliente
        when(accountConverter.toEntity(updatedAccount)).thenReturn(updatedEntity);
        when(accountRepository.save(updatedEntity)).thenReturn(Mono.just(updatedEntity));
        when(accountConverter.toDto(updatedEntity)).thenReturn(updatedDto);
        StepVerifier.create(accountService.upgradeAccount(accountId, updatedAccount))
                .expectNextMatches(response ->
                        response.getStatusCode() == HttpStatus.OK && response.getBody().equals(updatedDto)
                )
                .verifyComplete();
    }
}