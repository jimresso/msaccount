package com.nttdata.account.msaccount.service.impl;

import com.nttdata.account.msaccount.exception.BusinessException;
import com.nttdata.account.msaccount.exception.EntityNotFoundException;
import com.nttdata.account.msaccount.exception.InternalServerErrorException;
import com.nttdata.account.msaccount.mapper.AccountConverter;
import com.nttdata.account.msaccount.model.AccountEntityDTO;
import com.nttdata.account.msaccount.repository.AccountRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.Account;
import org.openapitools.model.DepositRequest;
import org.openapitools.model.WithdrawRequest;
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
        AccountEntityDTO accountEntityDTO = new AccountEntityDTO();
        accountEntityDTO.setId(accountId);
        Account accountDto = new Account();
        accountDto.setId(accountId);
        when(accountRepository.findById(accountId)).thenReturn(Mono.just(accountEntityDTO));
        when(accountConverter.toDto(accountEntityDTO)).thenReturn(accountDto);
        StepVerifier.create(accountService.findAccountById(accountId))
                .expectNext(ResponseEntity.ok(accountDto))
                .verifyComplete();
        verify(accountRepository).findById(accountId);
        verify(accountConverter).toDto(accountEntityDTO);
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
    void removeAccount_ReturnsNoContent() {
        String accountId = "123";
        AccountEntityDTO existingAccount = new AccountEntityDTO();
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
        AccountEntityDTO existingAccount = new AccountEntityDTO();
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
        AccountEntityDTO accountEntityDTO1 = new AccountEntityDTO();
        accountEntityDTO1.setId("1");
        accountEntityDTO1.setCustomerId("123");
        accountEntityDTO1.setAccountType(AccountEntityDTO.AccountType.AHORRO);
        AccountEntityDTO accountEntityDTO2 = new AccountEntityDTO();
        accountEntityDTO2.setId("2");
        accountEntityDTO2.setCustomerId("456");
        accountEntityDTO2.setAccountType(AccountEntityDTO.AccountType.CORRIENTE);
        Account accountDto1 = new Account().id("1").customerId("123").accountType(Account.AccountTypeEnum.AHORRO);
        Account accountDto2 = new Account().id("2").customerId("456").accountType(Account.AccountTypeEnum.CORRIENTE);
        when(accountRepository.findAll()).thenReturn(Flux.just(accountEntityDTO1, accountEntityDTO2));
        when(accountConverter.toDto(accountEntityDTO1)).thenReturn(accountDto1);
        when(accountConverter.toDto(accountEntityDTO2)).thenReturn(accountDto2);
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
        AccountEntityDTO existingEntity = new AccountEntityDTO();
        existingEntity.setId(accountId);
        existingEntity.setCustomerId(customerId);
        existingEntity.setCustomerType(AccountEntityDTO.CustomerType.PERSONAL);
        existingEntity.setAccountType(AccountEntityDTO.AccountType.AHORRO);
        AccountEntityDTO updatedEntity = new AccountEntityDTO();
        updatedEntity.setId(accountId);
        updatedEntity.setCustomerId(customerId);
        updatedEntity.setCustomerType(AccountEntityDTO.CustomerType.PERSONAL);
        updatedEntity.setAccountType(AccountEntityDTO.AccountType.AHORRO);
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
        AccountEntityDTO existingEntity = new AccountEntityDTO();
        existingEntity.setId(accountId);
        existingEntity.setCustomerId(customerId);
        existingEntity.setCustomerType(AccountEntityDTO.CustomerType.PERSONAL);
        existingEntity.setAccountType(AccountEntityDTO.AccountType.CORRIENTE);
        AccountEntityDTO updatedEntity = new AccountEntityDTO();
        updatedEntity.setId(accountId);
        updatedEntity.setCustomerId(customerId);
        updatedEntity.setCustomerType(AccountEntityDTO.CustomerType.PERSONAL);
        updatedEntity.setAccountType(AccountEntityDTO.AccountType.AHORRO);
        AccountEntityDTO anotherExistingAccount = new AccountEntityDTO();
        anotherExistingAccount.setId("789");
        anotherExistingAccount.setCustomerId(customerId);
        anotherExistingAccount.setCustomerType(AccountEntityDTO.CustomerType.PERSONAL);
        anotherExistingAccount.setAccountType(AccountEntityDTO.AccountType.AHORRO);
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
        AccountEntityDTO existingEntity = new AccountEntityDTO();
        existingEntity.setId(accountId);
        existingEntity.setCustomerId(customerId);
        existingEntity.setCustomerType(AccountEntityDTO.CustomerType.PERSONAL);
        existingEntity.setAccountType(AccountEntityDTO.AccountType.AHORRO);
        Account updatedAccount = new Account();
        updatedAccount.setAccountType(Account.AccountTypeEnum.PLAZO_FIJO);
        AccountEntityDTO updatedEntity = new AccountEntityDTO();
        updatedEntity.setId(accountId);
        updatedEntity.setCustomerId(customerId);
        updatedEntity.setCustomerType(AccountEntityDTO.CustomerType.PERSONAL);
        updatedEntity.setAccountType(AccountEntityDTO.AccountType.PLAZO_FIJO);
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
        AccountEntityDTO existingEntity = new AccountEntityDTO();
        existingEntity.setId(accountId);
        existingEntity.setCustomerId(customerId);
        existingEntity.setCustomerType(AccountEntityDTO.CustomerType.EMPRESARIAL);
        existingEntity.setAccountType(AccountEntityDTO.AccountType.AHORRO);
        existingEntity.setHolders(Collections.singletonList("holder1")); // Tiene titulares
        Account updatedAccount = new Account();
        updatedAccount.setAccountType(Account.AccountTypeEnum.CORRIENTE);
        AccountEntityDTO updatedEntity = new AccountEntityDTO();
        updatedEntity.setId(accountId);
        updatedEntity.setCustomerId(customerId);
        updatedEntity.setCustomerType(AccountEntityDTO.CustomerType.EMPRESARIAL);
        updatedEntity.setAccountType(AccountEntityDTO.AccountType.CORRIENTE);
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