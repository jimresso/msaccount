package com.nttdata.account.msaccount.service.impl;

import com.nttdata.account.msaccount.configure.AccountProperties;
import com.nttdata.account.msaccount.exception.BusinessException;
import com.nttdata.account.msaccount.exception.EntityNotFoundException;
import com.nttdata.account.msaccount.exception.InternalServerErrorException;
import com.nttdata.account.msaccount.mapper.AccountConverter;
import com.nttdata.account.msaccount.model.AccountEntityDTO;
import com.nttdata.account.msaccount.model.TaxedTransactionLimitDTO;
import com.nttdata.account.msaccount.model.TransactionDTO;
import com.nttdata.account.msaccount.repository.AccountRepository;
import com.nttdata.account.msaccount.repository.ComissionRepository;
import com.nttdata.account.msaccount.repository.TransactionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.Account;

import org.openapitools.model.DepositRequest;
import org.openapitools.model.WithdrawRequest;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestPropertySource(locations = "classpath:application.properties")
class AccountServiceImplTest {

    @InjectMocks
    private AccountServiceImpl accountService;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountConverter accountConverter;

    @Mock
    private Logger logger;

    @Mock
    private WebClient.Builder webClientBuilder;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private ComissionRepository comissionRepository;

    @Captor
    private ArgumentCaptor<Account> accountCaptor;

    @Mock
    private AccountProperties accountProperties;

    @BeforeEach
    void setup() {
        lenient().when(accountProperties.getVip()).thenReturn(1000.0);
        lenient().when(accountProperties.getPemy()).thenReturn(1000.0);
        lenient().when(accountProperties.getTransaction()).thenReturn(10);
    }


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
        existingEntity.setHolders(Collections.singletonList("holder1"));
        Account updatedAccount = new Account();
        updatedAccount.setAccountType(Account.AccountTypeEnum.CORRIENTE);
        AccountEntityDTO updatedEntity = new AccountEntityDTO();
        updatedEntity.setId(accountId);
        updatedEntity.setCustomerId(customerId);
        updatedEntity.setCustomerType(AccountEntityDTO.CustomerType.EMPRESARIAL);
        updatedEntity.setAccountType(AccountEntityDTO.AccountType.CORRIENTE);
        updatedEntity.setHolders(Collections.singletonList("holder1"));
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
    @Test
    void testNewAccount_VipAccount_WithBalanceLessThanMinimum() {
        Account account = new Account();
        account.setDni("123456789");
        account.setClientType(Account.ClientTypeEnum.VIP);
        account.setBalance(500.0);
        account.setCustomerId("CUST-123");
        when(accountProperties.getVip()).thenReturn(1000.0);
        AccountEntityDTO accountEntityDTO = new AccountEntityDTO();
        accountEntityDTO.setDni("123456789");
        accountEntityDTO.setClientType(AccountEntityDTO.ClientType.VIP);
        accountEntityDTO.setBalance(500.0);
        accountEntityDTO.setCustomerId("CUST-123");
        when(accountConverter.toEntity(account)).thenReturn(accountEntityDTO);
        Mono<ResponseEntity<Account>> result = accountService.newAccount(account);
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        throwable.getMessage().contains("The initial amount must be greater than or equal to 1000.0 for VIP accounts"))
                .verify();
    }
    @Test
    void testNewAccount_PymeAccount_WithBalanceLessThanMinimum() {
        Account account = new Account();
        account.setDni("123456789");
        account.setClientType(Account.ClientTypeEnum.PYME);
        account.setBalance(500.0);
        account.setCustomerId("CUST-123");
        AccountEntityDTO accountEntityDTO = new AccountEntityDTO();
        accountEntityDTO.setDni("123456789");
        accountEntityDTO.setClientType(AccountEntityDTO.ClientType.PYME);
        accountEntityDTO.setBalance(500.0);
        accountEntityDTO.setCustomerId("CUST-123");
        when(accountConverter.toEntity(account)).thenReturn(accountEntityDTO);
        Mono<ResponseEntity<Account>> result = accountService.newAccount(account);
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        throwable.getMessage().contains("The initial amount must be greater than or equal to 1000.0 for PYME accounts"))
                .verify();
    }

    @Test
    void testNewAccount_WithExistingCustomerAndNoCreditCard() {
        Account account = new Account();
        account.setDni("123456789");
        account.setClientType(Account.ClientTypeEnum.VIP);
        account.setBalance(1500.0);
        account.setCustomerId("CUST-123");

        AccountEntityDTO accountEntityDTO = new AccountEntityDTO();
        accountEntityDTO.setDni("123456789");
        accountEntityDTO.setClientType(AccountEntityDTO.ClientType.VIP);
        accountEntityDTO.setBalance(1500.0);
        accountEntityDTO.setCustomerId("CUST-123");

        when(accountConverter.toEntity(account)).thenReturn(accountEntityDTO);
        when(accountRepository.findByDni(account.getDni())).thenReturn(Flux.just(accountEntityDTO));
         Map<String, Object> response = new HashMap<>();
        response.put("creditCard", false);

        WebClient webClient = mock(WebClient.class);
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(response));

        Mono<ResponseEntity<Account>> result = accountService.newAccount(account);
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        throwable.getMessage().contains("Customer does not have a credit card, cannot create VIP or PYME account"))
                .verify();
        verify(accountRepository).findByDni(account.getDni());
        verify(webClientBuilder).build();
        verify(webClient).post();
        verify(requestBodyUriSpec).bodyValue(any());
        verify(requestHeadersSpec).retrieve();
        verify(responseSpec).bodyToMono(Map.class);
    }

    @Test
    void testNewAccount_WithExistingDuplicateCustomerAndCreditCard() {
        Account account = new Account();
        account.setDni("123456789");
        account.setClientType(Account.ClientTypeEnum.VIP);
        account.setBalance(1500.0);
        account.setCustomerId("CUST-123");
        account.setAccountType(Account.AccountTypeEnum.AHORRO);

        AccountEntityDTO accountEntityDTO = new AccountEntityDTO();
        accountEntityDTO.setDni("123456789");
        accountEntityDTO.setClientType(AccountEntityDTO.ClientType.VIP);
        accountEntityDTO.setBalance(1500.0);
        accountEntityDTO.setCustomerId("CUST-123");
        accountEntityDTO.setAccountType(AccountEntityDTO.AccountType.AHORRO);

        when(accountConverter.toEntity(account)).thenReturn(accountEntityDTO);
        when(accountRepository.findByDni(account.getDni())).thenReturn(Flux.just(accountEntityDTO));
        when(accountRepository.findByCustomerId(account.getCustomerId())).thenReturn(Flux.just(accountEntityDTO));

        Map<String, Object> response = new HashMap<>();
        response.put("creditCard", true);

        WebClient webClient = mock(WebClient.class);
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(response));

        Mono<ResponseEntity<Account>> result = accountService.newAccount(account);
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        throwable.getMessage().contains("Account creation does not meet business rules"))
                .verify();
        verify(accountRepository).findByDni(account.getDni());
        verify(webClientBuilder).build();
        verify(webClient).post();
        verify(requestBodyUriSpec).bodyValue(any());
        verify(requestHeadersSpec).retrieve();
        verify(responseSpec).bodyToMono(Map.class);
    }

    @Test
    void testNewAccount_WithExistingCustomerAndCreditCard() {
        Account account = new Account();
        account.setDni("123111789");
        account.setClientType(Account.ClientTypeEnum.VIP);
        account.setBalance(1500.0);
        account.setCustomerId("CUST-123");
        account.setAccountType(Account.AccountTypeEnum.AHORRO);

        AccountEntityDTO accountEntityDTO = new AccountEntityDTO();
        accountEntityDTO.setDni("123111789");
        accountEntityDTO.setClientType(AccountEntityDTO.ClientType.VIP);
        accountEntityDTO.setBalance(1500.0);
        accountEntityDTO.setCustomerId("CUST-123");
        accountEntityDTO.setAccountType(AccountEntityDTO.AccountType.AHORRO);

        AccountEntityDTO accountEntity = new AccountEntityDTO();
        accountEntity.setDni("123111789");
        accountEntity.setClientType(AccountEntityDTO.ClientType.VIP);
        accountEntity.setBalance(1500.0);
        accountEntity.setCustomerId("CUST-123");
        accountEntity.setAccountType(AccountEntityDTO.AccountType.CORRIENTE);


        when(accountConverter.toEntity(account)).thenReturn(accountEntityDTO);
        when(accountConverter.toDto(accountEntityDTO)).thenReturn(account);
        when(accountRepository.findByDni(account.getDni())).thenReturn(Flux.just(accountEntityDTO));
        when(accountRepository.findByCustomerId(account.getCustomerId())).thenReturn(Flux.just(accountEntity));
        when(accountRepository.save(accountEntityDTO)).thenReturn(Mono.just(accountEntityDTO));

        Map<String, Object> response = new HashMap<>();
        response.put("creditCard", true);

        WebClient webClient = mock(WebClient.class);
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(response));

        Mono<ResponseEntity<Account>> result = accountService.newAccount(account);
        StepVerifier.create(result)
                .expectNextMatches(responseEntity -> responseEntity.getStatusCode().equals(HttpStatus.CREATED) &&
                        responseEntity.getBody() != null &&
                        responseEntity.getBody().getCustomerId().equals("CUST-123"))
                .verifyComplete();

        verify(accountRepository).findByDni(account.getDni());
        verify(accountRepository).findByCustomerId(account.getCustomerId());
        verify(webClientBuilder).build();
        verify(webClient).post();
        verify(requestBodyUriSpec).bodyValue(any());
        verify(requestHeadersSpec).retrieve();
        verify(responseSpec).bodyToMono(Map.class);
    }
    @Test
    void testNewAccount_WithExistingCustomerPlazoFijoAndCreditCard() {
        Account account = new Account();
        account.setDni("123111789");
        account.setClientType(Account.ClientTypeEnum.VIP);
        account.setCustomerType(Account.CustomerTypeEnum.PERSONAL);
        account.setBalance(1500.0);
        account.setCustomerId("CUST-111");
        account.setAccountType(Account.AccountTypeEnum.PLAZO_FIJO);

        AccountEntityDTO accountEntityDTO = new AccountEntityDTO();
        accountEntityDTO.setDni("123111111");
        accountEntityDTO.setClientType(AccountEntityDTO.ClientType.VIP);
        accountEntityDTO.setBalance(1500.0);
        accountEntityDTO.setCustomerId("CUST-111");
        accountEntityDTO.setAccountType(AccountEntityDTO.AccountType.PLAZO_FIJO);
        accountEntityDTO.setCustomerType(AccountEntityDTO.CustomerType.PERSONAL);

        AccountEntityDTO accountEntity = new AccountEntityDTO();
        accountEntity.setDni("123111111");
        accountEntity.setClientType(AccountEntityDTO.ClientType.VIP);
        accountEntity.setBalance(1500.0);
        accountEntity.setCustomerId("CUST-111");
        accountEntity.setAccountType(AccountEntityDTO.AccountType.CORRIENTE);


        when(accountConverter.toEntity(account)).thenReturn(accountEntityDTO);
        when(accountConverter.toDto(accountEntityDTO)).thenReturn(account);
        when(accountRepository.findByDni(account.getDni())).thenReturn(Flux.just(accountEntityDTO));
        when(accountRepository.findByCustomerId(account.getCustomerId())).thenReturn(Flux.just(accountEntity));
        when(accountRepository.save(accountEntityDTO)).thenReturn(Mono.just(accountEntityDTO));

        Map<String, Object> response = new HashMap<>();
        response.put("creditCard", true);

        WebClient webClient = mock(WebClient.class);
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(response));

        Mono<ResponseEntity<Account>> result = accountService.newAccount(account);
        StepVerifier.create(result)
                .expectNextMatches(responseEntity -> responseEntity.getStatusCode().equals(HttpStatus.CREATED) &&
                        responseEntity.getBody() != null &&
                        responseEntity.getBody().getCustomerId().equals("CUST-111"))
                .verifyComplete();

        verify(accountRepository).findByDni(account.getDni());
        verify(accountRepository).findByCustomerId(account.getCustomerId());
        verify(webClientBuilder).build();
        verify(webClient).post();
        verify(requestBodyUriSpec).bodyValue(any());
        verify(requestHeadersSpec).retrieve();
        verify(responseSpec).bodyToMono(Map.class);
    }
    @Test
    void testNewAccount_WithExistingCustomerEmpresarialAndCreditCard() {
        Account account = new Account();
        account.setDni("123111222");
        account.setClientType(Account.ClientTypeEnum.PYME);
        account.setCustomerType(Account.CustomerTypeEnum.EMPRESARIAL);
        account.setBalance(1500.0);
        account.setCustomerId("CUST-222");
        account.setAccountType(Account.AccountTypeEnum.CORRIENTE);
        List<String> lista = new ArrayList<>(Arrays.asList("Elemento1", "Elemento2", "Elemento3"));
        account.setHolders(lista);

        AccountEntityDTO accountEntityDTO = new AccountEntityDTO();
        accountEntityDTO.setDni("123111222");
        accountEntityDTO.setClientType(AccountEntityDTO.ClientType.PYME);
        accountEntityDTO.setBalance(1500.0);
        accountEntityDTO.setCustomerId("CUST-222");
        accountEntityDTO.setAccountType(AccountEntityDTO.AccountType.CORRIENTE);
        accountEntityDTO.setCustomerType(AccountEntityDTO.CustomerType.EMPRESARIAL);
        accountEntityDTO.setHolders(lista);

        AccountEntityDTO accountEntity = new AccountEntityDTO();
        accountEntity.setDni("123111222");
        accountEntity.setClientType(AccountEntityDTO.ClientType.PYME);
        accountEntity.setBalance(1500.0);
        accountEntity.setCustomerId("CUST-222");
        accountEntity.setAccountType(AccountEntityDTO.AccountType.CORRIENTE);


        when(accountConverter.toEntity(account)).thenReturn(accountEntityDTO);
        when(accountConverter.toDto(accountEntityDTO)).thenReturn(account);
        when(accountRepository.findByDni(account.getDni())).thenReturn(Flux.just(accountEntityDTO));
        when(accountRepository.findByCustomerId(account.getCustomerId())).thenReturn(Flux.just(accountEntity));
        when(accountRepository.save(accountEntityDTO)).thenReturn(Mono.just(accountEntityDTO));

        Map<String, Object> response = new HashMap<>();
        response.put("creditCard", true);

        WebClient webClient = mock(WebClient.class);
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(response));

        Mono<ResponseEntity<Account>> result = accountService.newAccount(account);
        StepVerifier.create(result)
                .expectNextMatches(responseEntity -> responseEntity.getStatusCode().equals(HttpStatus.CREATED) &&
                        responseEntity.getBody() != null &&
                        responseEntity.getBody().getCustomerId().equals("CUST-222"))
                .verifyComplete();

        verify(accountRepository).findByDni(account.getDni());
        verify(accountRepository).findByCustomerId(account.getCustomerId());
        verify(accountRepository).save(accountEntityDTO);
        verify(webClientBuilder).build();
        verify(webClient).post();
        verify(requestBodyUriSpec).bodyValue(any());
        verify(requestHeadersSpec).retrieve();
        verify(responseSpec).bodyToMono(Map.class);
    }

    @Test
    void  testSaveAccount_ErrorHandling(){
        Account account = new Account();
        account.setDni("123111222");
        account.setClientType(Account.ClientTypeEnum.PYME);
        account.setCustomerType(Account.CustomerTypeEnum.EMPRESARIAL);
        account.setBalance(1500.0);
        account.setCustomerId("CUST-222");
        account.setAccountType(Account.AccountTypeEnum.CORRIENTE);
        List<String> lista = new ArrayList<>(Arrays.asList("Elemento1", "Elemento2", "Elemento3"));
        account.setHolders(lista);

        AccountEntityDTO accountEntityDTO = new AccountEntityDTO();
        accountEntityDTO.setDni("123111222");
        accountEntityDTO.setClientType(AccountEntityDTO.ClientType.PYME);
        accountEntityDTO.setBalance(1500.0);
        accountEntityDTO.setCustomerId("CUST-222");
        accountEntityDTO.setAccountType(AccountEntityDTO.AccountType.CORRIENTE);
        accountEntityDTO.setCustomerType(AccountEntityDTO.CustomerType.EMPRESARIAL);
        accountEntityDTO.setHolders(lista);

        AccountEntityDTO accountEntity = new AccountEntityDTO();
        accountEntity.setDni("123111222");
        accountEntity.setClientType(AccountEntityDTO.ClientType.PYME);
        accountEntity.setBalance(1500.0);
        accountEntity.setCustomerId("CUST-222");
        accountEntity.setAccountType(AccountEntityDTO.AccountType.CORRIENTE);


        when(accountConverter.toEntity(account)).thenReturn(accountEntityDTO);
        when(accountRepository.findByDni(account.getDni())).thenReturn(Flux.just(accountEntityDTO));
        when(accountRepository.findByCustomerId(account.getCustomerId())).thenReturn(Flux.just(accountEntity));
        when(accountRepository.save(accountEntityDTO)).thenReturn(Mono.error(new RuntimeException("Error saving account")));

        Map<String, Object> response = new HashMap<>();
        response.put("creditCard", true);

        WebClient webClient = mock(WebClient.class);
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(response));

        Mono<ResponseEntity<Account>> result = accountService.newAccount(account);
        StepVerifier.create(result)
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(InternalServerErrorException.class);
                    assertThat(throwable.getMessage()).isEqualTo("An error occurred while saving the account");
                })
                .verify();

        verify(accountRepository).findByDni(account.getDni());
        verify(accountRepository).findByCustomerId(account.getCustomerId());
        verify(webClientBuilder).build();
        verify(webClient).post();
        verify(requestBodyUriSpec).bodyValue(any());
        verify(requestHeadersSpec).retrieve();
        verify(responseSpec).bodyToMono(Map.class);
    }

    @Test
    void testDepositAmount_Successful() {
        String accountIdDestination = "acc-456";
        String customerIdOrigin = "cust-123";

        DepositRequest request = new DepositRequest();
        request.setMonto(200.0);
        request.setCustomerId(customerIdOrigin);

        AccountEntityDTO destinationAccount = new AccountEntityDTO();
        destinationAccount.setId(accountIdDestination);
        destinationAccount.setCustomerId("cust-456");
        destinationAccount.setAccountType(AccountEntityDTO.AccountType.AHORRO);
        destinationAccount.setBalance(500.0);

        AccountEntityDTO originAccount = new AccountEntityDTO();
        originAccount.setId("acc-123");
        originAccount.setCustomerId(customerIdOrigin);
        originAccount.setBalance(1000.0);
        originAccount.setLimitTransaction(11.0);
        originAccount.setDni("77788899");

        TaxedTransactionLimitDTO commission = new TaxedTransactionLimitDTO();
        commission.setMonto(BigDecimal.valueOf(10.0).doubleValue());

        TransactionDTO savedTransaction = new TransactionDTO();

        when(accountRepository.findById(accountIdDestination)).thenReturn(Mono.just(destinationAccount));
        when(accountRepository.findFirstByCustomerId(customerIdOrigin)).thenReturn(Mono.just(originAccount));
        when(comissionRepository.findByAccountType("AHORRO")).thenReturn(Mono.just(commission));
        when(accountRepository.save(any(AccountEntityDTO.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(transactionRepository.save(any(TransactionDTO.class))).thenReturn(Mono.just(savedTransaction));
        when(accountConverter.toDto(any())).thenReturn(new Account());

        StepVerifier.create(accountService.depositAmount(accountIdDestination, request))
                .expectNextMatches(response -> response.getStatusCode().is2xxSuccessful())
                .verifyComplete();

        verify(accountRepository).findById(accountIdDestination);
        verify(accountRepository).findFirstByCustomerId(customerIdOrigin);
        verify(comissionRepository).findByAccountType("AHORRO");
        verify(accountRepository, times(2)).save(any(AccountEntityDTO.class)); // origen y destino
        verify(transactionRepository).save(any(TransactionDTO.class));
    }

    @Test
    void testDepositAmount_AccountDestinationNotFound() {
        String accountIdDestination = "acc-999";
        DepositRequest request = new DepositRequest();
        request.setCustomerId("cust-123");
        when(accountRepository.findById(accountIdDestination)).thenReturn(Mono.empty());
        StepVerifier.create(accountService.depositAmount(accountIdDestination, request))
                .expectErrorMatches(throwable ->
                        throwable instanceof EntityNotFoundException &&
                                throwable.getMessage().equals("Account not found with id: " + accountIdDestination)
                )
                .verify();
        verify(accountRepository).findById(accountIdDestination);
        verify(accountRepository, never()).findFirstByCustomerId(any());
    }

    @Test
    void testDepositAmount_AccountCustomerIdNotFound() {
        String accountIdDestination = "acc-999";
        String customerIdOrigin = "cust-123";
        DepositRequest request = new DepositRequest();
        request.setCustomerId(customerIdOrigin);
        AccountEntityDTO destinationAccount = new AccountEntityDTO();
        destinationAccount.setId(accountIdDestination);
        destinationAccount.setCustomerId("cust-456");
        destinationAccount.setAccountType(AccountEntityDTO.AccountType.AHORRO);
        destinationAccount.setBalance(500.0);
        when(accountRepository.findById(accountIdDestination)).thenReturn(Mono.just(destinationAccount));
        when(accountRepository.findFirstByCustomerId(customerIdOrigin)).thenReturn(Mono.empty());
        StepVerifier.create(accountService.depositAmount(accountIdDestination, request))
                .expectErrorMatches(throwable ->
                        throwable instanceof EntityNotFoundException &&
                                throwable.getMessage().equals("Account not found with customer id: " + customerIdOrigin)
                )
                .verify();
        verify(accountRepository).findById(accountIdDestination);
        verify(accountRepository).findFirstByCustomerId(customerIdOrigin);
    }

    @Test
    void testDepositAmountBalanceCero_Successful() {
        String accountIdDestination = "acc-456";
        String customerIdOrigin = "cust-123";

        DepositRequest request = new DepositRequest();
        request.setMonto(200.0);
        request.setCustomerId(customerIdOrigin);

        AccountEntityDTO destinationAccount = new AccountEntityDTO();
        destinationAccount.setId(accountIdDestination);
        destinationAccount.setCustomerId("cust-456");
        destinationAccount.setAccountType(AccountEntityDTO.AccountType.AHORRO);
        destinationAccount.setBalance(500.0);

        AccountEntityDTO originAccount = new AccountEntityDTO();
        originAccount.setId("acc-123");
        originAccount.setCustomerId(customerIdOrigin);
        originAccount.setBalance(0.0);
        originAccount.setLimitTransaction(11.0);
        originAccount.setDni("77788899");

        TaxedTransactionLimitDTO commission = new TaxedTransactionLimitDTO();
        commission.setMonto(BigDecimal.valueOf(10.0).doubleValue());

        TransactionDTO savedTransaction = new TransactionDTO();

        when(accountRepository.findById(accountIdDestination)).thenReturn(Mono.just(destinationAccount));
        when(accountRepository.findFirstByCustomerId(customerIdOrigin)).thenReturn(Mono.just(originAccount));
        when(comissionRepository.findByAccountType("AHORRO")).thenReturn(Mono.just(commission));

        StepVerifier.create(accountService.depositAmount(accountIdDestination, request))
                .expectErrorMatches(throwable ->
                        throwable instanceof BusinessException &&
                                throwable.getMessage().equals("insufficient balance")
                )
                .verify();

        verify(accountRepository).findById(accountIdDestination);
        verify(accountRepository).findFirstByCustomerId(customerIdOrigin);
        verify(comissionRepository).findByAccountType("AHORRO");
    }

    @Test
    void testDepositAmountSinComission_Successful() {
        String accountIdDestination = "acc-333";
        String customerIdOrigin = "cust-222";

        DepositRequest request = new DepositRequest();
        request.setMonto(200.0);
        request.setCustomerId(customerIdOrigin);

        AccountEntityDTO destinationAccount = new AccountEntityDTO();
        destinationAccount.setId(accountIdDestination);
        destinationAccount.setCustomerId("cust-333");
        destinationAccount.setAccountType(AccountEntityDTO.AccountType.AHORRO);
        destinationAccount.setBalance(500.0);

        AccountEntityDTO originAccount = new AccountEntityDTO();
        originAccount.setId("acc-222");
        originAccount.setCustomerId(customerIdOrigin);
        originAccount.setBalance(1000.0);
        originAccount.setLimitTransaction(9.0);
        originAccount.setDni("77118899");

        TaxedTransactionLimitDTO commission = new TaxedTransactionLimitDTO();
        commission.setMonto(BigDecimal.valueOf(10.0).doubleValue());

        TransactionDTO savedTransaction = new TransactionDTO();

        when(accountRepository.findById(accountIdDestination)).thenReturn(Mono.just(destinationAccount));
        when(accountRepository.findFirstByCustomerId(customerIdOrigin)).thenReturn(Mono.just(originAccount));
        when(comissionRepository.findByAccountType("AHORRO")).thenReturn(Mono.just(commission));
        when(accountRepository.save(any(AccountEntityDTO.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(transactionRepository.save(any(TransactionDTO.class))).thenReturn(Mono.just(savedTransaction));
        when(accountConverter.toDto(any())).thenReturn(new Account());

        StepVerifier.create(accountService.depositAmount(accountIdDestination, request))
                .expectNextMatches(response -> response.getStatusCode().is2xxSuccessful())
                .verifyComplete();

        verify(accountRepository).findById(accountIdDestination);
        verify(accountRepository).findFirstByCustomerId(customerIdOrigin);
        verify(comissionRepository).findByAccountType("AHORRO");
        verify(accountRepository, times(2)).save(any(AccountEntityDTO.class));
        verify(transactionRepository).save(any(TransactionDTO.class));
    }
    @Test
    void testWithdrawAmount_Successful() {
        String accountId = "acc-123";
        WithdrawRequest request = new WithdrawRequest();
        request.setMonto(100.0);
        AccountEntityDTO existingAccount = new AccountEntityDTO();
        existingAccount.setId(accountId);
        existingAccount.setCustomerId("cust-001");
        existingAccount.setDni("12345678");
        existingAccount.setAccountType(AccountEntityDTO.AccountType.CORRIENTE);
        existingAccount.setBalance(500.0);
        existingAccount.setLimitTransaction(12.0);
        TaxedTransactionLimitDTO commission = new TaxedTransactionLimitDTO();
        commission.setMonto(BigDecimal.valueOf(10.0).doubleValue());
        AccountEntityDTO savedAccount = new AccountEntityDTO();
        savedAccount.setId(accountId);
        savedAccount.setCustomerId("cust-001");
        savedAccount.setDni("12345678");
        savedAccount.setAccountType(AccountEntityDTO.AccountType.CORRIENTE);
        savedAccount.setBalance(390.0);
        savedAccount.setLimitTransaction(13.0);
        Account expectedAccount = new Account();
        expectedAccount.setId(accountId);
        TransactionDTO transactionDTO = new TransactionDTO();
        transactionDTO.setTransactionType(TransactionDTO.TransactionType.RETIRO);
        transactionDTO.setAmount(100.0);
        transactionDTO.setCommissionAmount(10.0);
        transactionDTO.setCustomerIdOrigin("acc-123");
        transactionDTO.setCustomerIdDestination("cust-001");
        transactionDTO.setDni("12345678");
        when(accountRepository.findFirstByCustomerId(accountId)).thenReturn(Mono.just(existingAccount));
        when(comissionRepository.findByAccountType("CORRIENTE")).thenReturn(Mono.just(commission));
        when(accountRepository.save(any(AccountEntityDTO.class))).thenReturn(Mono.just(savedAccount));
        when(transactionRepository.save(any(TransactionDTO.class))).thenReturn(Mono.just(transactionDTO));
        when(accountConverter.toDto(any(AccountEntityDTO.class))).thenReturn(expectedAccount);
        StepVerifier.create(accountService.withdrawAmount(accountId, request))
                .expectNextMatches(response ->
                        response.getStatusCode() == HttpStatus.OK &&
                                Objects.requireNonNull(response.getBody()).getId().equals(accountId)
                )
                .verifyComplete();
        verify(accountRepository).findFirstByCustomerId(accountId);
        verify(comissionRepository).findByAccountType("CORRIENTE");
        verify(accountRepository, times(1)).save(any(AccountEntityDTO.class));
        verify(transactionRepository).save(any(TransactionDTO.class));
        verify(accountConverter).toDto(any(AccountEntityDTO.class));
    }
}