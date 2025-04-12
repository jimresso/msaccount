package com.nttdata.account.msaccount.service.impl;


import com.nttdata.account.msaccount.configure.AccountProperties;
import com.nttdata.account.msaccount.notificaciones.FallbackNotifier;
import com.nttdata.account.msaccount.exception.BusinessException;
import com.nttdata.account.msaccount.exception.EntityNotFoundException;
import com.nttdata.account.msaccount.exception.InternalServerErrorException;
import com.nttdata.account.msaccount.exception.RemoteServiceUnavailableException;
import com.nttdata.account.msaccount.mapper.AccountConverter;
import com.nttdata.account.msaccount.model.AccountEntityDTO;
import com.nttdata.account.msaccount.model.TaxedTransactionLimitDTO;
import com.nttdata.account.msaccount.model.TransactionDTO;
import com.nttdata.account.msaccount.repository.AccountRepository;
import com.nttdata.account.msaccount.repository.ComissionRepository;
import com.nttdata.account.msaccount.repository.TransactionRepository;
import com.nttdata.account.msaccount.service.AccountService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.openapitools.model.Account;
import org.openapitools.model.DepositRequest;
import org.openapitools.model.WithdrawRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final WebClient.Builder webClientBuilder;
    private final AccountRepository accountRepository;
    private final AccountConverter accountConverter;
    private final TransactionRepository transactionRepository;
    private final ComissionRepository comissionRepository;
    private final AccountProperties accountProperties;
    private final FallbackNotifier fallbackNotifier;
    private static final Logger logger = LoggerFactory.getLogger(AccountServiceImpl.class);

    @Override
    public Mono<ResponseEntity<Account>> findAccountById(String id) {
        return accountRepository.findById(id)
                .map(accountConverter::toDto)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.defer(() -> {
                    logger.warn("Account not  found with ID : {}", id);
                    return Mono.error(new EntityNotFoundException("Failed to retrieve the account with ID: " + id));
                }))
                .onErrorResume(e -> {
                    if (e instanceof EntityNotFoundException) {
                        return Mono.error(e);
                    }
                    logger.error("Error retrieving account with ID {}: {}", id, e.getMessage());
                    return Mono.error(new
                            InternalServerErrorException("Unexpected error occurred while retrieving account"));
                });
    }
    @CircuitBreaker(name = "circuitBreakerAccount", fallbackMethod = "fallbackCheckCard")
    public Mono<ResponseEntity<Account>> newAccount(Account account) {
        AccountEntityDTO dto = accountConverter.toEntity(account);
        if (dto.getClientType() == AccountEntityDTO.ClientType.VIP &&
                dto.getBalance() < accountProperties.getVip()) {
            return Mono.error(new BusinessException("Minimum balance for VIP not met"));
        }
        if (dto.getClientType() == AccountEntityDTO.ClientType.PYME &&
                dto.getBalance() < accountProperties.getPemy()) {
            return Mono.error(new BusinessException("Minimum balance for PYME not met"));
        }
        if (dto.getClientType() == AccountEntityDTO.ClientType.VIP ||
                dto.getClientType() == AccountEntityDTO.ClientType.PYME) {
            return accountRepository.findByDni(account.getDni())
                    .collectList()
                    .flatMap(accounts -> {
                        List<String> customerIds = accounts.stream()
                                .map(AccountEntityDTO::getCustomerId)
                                .toList();
                        return checkCustomerHasCard(customerIds)
                                .flatMap(hasCard -> {
                                    if (Boolean.TRUE.equals(hasCard)) {
                                        return createAccount(dto);
                                    } else {
                                        return Mono.error(new BusinessException("Customer has no credit card"));
                                    }
                                });
                    })
                    .onErrorResume(RemoteServiceUnavailableException.class, ex -> {
                        logger.error("Error técnico al consultar tarjetas: {}", ex.getMessage());
                        return Mono.error(ex);
                    });
        }
        return createAccount(dto);
    }
    public Mono<Boolean> checkCustomerHasCard(List<String> customerIds) {
        Map<String, Object> request = new HashMap<>();
        request.put("customerId", customerIds);
        return webClientBuilder.build()
                .post()
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        Mono.error(new RemoteServiceUnavailableException("Card service not available")))
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        Mono.error(new BusinessException("Client error checking card")))
                .bodyToMono(Map.class)
                .map(response -> (Boolean) response.get("creditCard"))
                .onErrorMap(throwable -> {
                    if (throwable instanceof WebClientRequestException) {
                        return new RemoteServiceUnavailableException("Card service unreachable", throwable);
                    }
                return throwable;
        });
    }


    public Mono<ResponseEntity<Account>> fallbackCheckCard(Throwable throwable) {
        if (!(throwable instanceof BusinessException)) {
            fallbackNotifier.sendFallbackEmail("AccountServiceImpl", throwable);
        }
        logger.warn("Fallback enabled for newAccount - type: {}", throwable.getClass().getName());
        return Mono.error(throwable);
    }

    private Mono<ResponseEntity<Account>> createAccount(AccountEntityDTO dto) {
        return validateAccountCreation(dto)
                .flatMap(valid -> {
                    if (!valid) {
                        return Mono.error(new BusinessException("Account creation does not meet business rules"));
                    }
                    return accountRepository.save(dto)
                            .map(accountConverter::toDto)
                            .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(saved))
                            .onErrorResume(e -> {
                                logger.error("DB error: {}", e.getMessage());
                                return Mono.error(new InternalServerErrorException("Error saving account"));
                            });
                });
    }
    private Mono<Boolean> validateAccountCreation(AccountEntityDTO dto) {
        return accountRepository.findByCustomerId(dto.getCustomerId())
                .collectList()
                .map(existingAccounts -> {
                    var type = dto.getAccountType();
                    var customerType = dto.getCustomerType();

                    if (customerType == AccountEntityDTO.CustomerType.EMPRESARIAL) {
                        boolean hasHolders = dto.getHolders() != null && !dto.getHolders().isEmpty();
                        return hasHolders && type != AccountEntityDTO.AccountType.AHORRO
                                && type != AccountEntityDTO.AccountType.PLAZO_FIJO;
                    }
                    if (type == AccountEntityDTO.AccountType.AHORRO || type == AccountEntityDTO.AccountType.CORRIENTE) {
                        return existingAccounts.stream().noneMatch(acc -> acc.getAccountType() == type);
                    }
                    if (type == AccountEntityDTO.AccountType.PLAZO_FIJO) {
                        return customerType == AccountEntityDTO.CustomerType.PERSONAL;
                    }
                    return false;
                });
    }

    @Override
    public Mono<ResponseEntity<Void>> removeAccount(String id) {
        return accountRepository.findById(id)
                .flatMap(existingAccount -> accountRepository.delete(existingAccount)
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
    public Mono<ResponseEntity<Account>> upgradeAccount(String accountId, Account updatedAccount) {
        AccountEntityDTO updatedAccountEntityDTO = accountConverter.toEntity(updatedAccount);
        return accountRepository.findById(accountId)
                .flatMap(existingAccount -> {
                    boolean typeChanged = !existingAccount.getAccountType()
                            .equals(updatedAccountEntityDTO.getAccountType());
                    boolean customerTypeChanged = !existingAccount.getCustomerType()
                            .equals(updatedAccountEntityDTO.getCustomerType());
                    if (typeChanged || customerTypeChanged) {
                        return validateAccountUpdate(existingAccount, updatedAccountEntityDTO)
                                .flatMap(isValid -> {
                                    if (Boolean.FALSE.equals(isValid)) {
                                        return Mono.error(
                                                new BusinessException("Account update does not meet business rules"));
                                    }
                                    return saveUpdatedAccount(existingAccount, updatedAccountEntityDTO);
                                });
                    }
                    return saveUpdatedAccount(existingAccount, updatedAccountEntityDTO);
                })
                .map(accountConverter::toDto)
                .map(updatedDto -> ResponseEntity.ok().body(updatedDto))
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Account not found")))
                .onErrorResume(e -> {
                    if (e instanceof BusinessException || e instanceof EntityNotFoundException) {
                        return Mono.error(e);
                    }
                    logger.error("Unexpected error updating account {}: {}", accountId, e.getMessage());
                    return Mono.error(new InternalServerErrorException("Unexpected error updating account"));
                });
    }

    @Override
    public Mono<ResponseEntity<Flux<Account>>> listAccounts() {
        return accountRepository.findAll()
                .map(accountConverter::toDto)
                .collectList()
                .flatMap(accounts -> Mono.just(ResponseEntity.ok(Flux.fromIterable(accounts))))
                .onErrorResume(e -> {
                    logger.error("Error retrieving accounts: {}", e.getMessage());
                    return Mono.error(new BusinessException("An error occurred while retrieving accounts"));
                });
    }

    @Override
    public Mono<ResponseEntity<Account>> depositAmount(String id, DepositRequest amount) {
        return accountRepository.findById(id)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Account not found with id: " + id)))
                .flatMap(acountDestination  -> accountRepository
                        .findFirstByCustomerId(amount.getCustomerId())
                        .switchIfEmpty(Mono.error(new EntityNotFoundException("Account not found with customer id: " +
                                amount.getCustomerId())))
                        .flatMap(accountOrigin  -> {
                            logger.warn("Account found: {}", acountDestination .getId());
                            return comissionRepository.findByAccountType(acountDestination .getAccountType().name())
                                    .defaultIfEmpty(new TaxedTransactionLimitDTO())
                                    .flatMap(commission -> {
                                        double commissionAmount = commission.getMonto() != null
                                                ? commission.getMonto().doubleValue() : 0.0;
                                        double netAmount;
                                        double taxes;
                                        double acountLimit = accountOrigin .getLimitTransaction();
                                        if ( accountOrigin.getBalance() < amount.getMonto() ) {
                                            return Mono.error(new BusinessException("insufficient balance"));
                                        }

                                        if (accountOrigin .getLimitTransaction() >
                                                accountProperties.getTransaction()) {
                                            netAmount = amount.getMonto() - commissionAmount;

                                            if (netAmount < 0) {
                                                return Mono.error(
                                                        new BusinessException("Net deposit amount cannot be negative"));
                                            }
                                            taxes = commission.getMonto();
                                            accountOrigin.setBalance(accountOrigin.getBalance() - netAmount);
                                            acountDestination.setBalance(acountDestination .getBalance() + netAmount);
                                        } else {
                                            taxes = 0;
                                            accountOrigin.setBalance(accountOrigin.getBalance() - amount.getMonto());
                                            acountDestination .setBalance(acountDestination .getBalance() +
                                                    amount.getMonto());
                                        }
                                        accountOrigin .setLimitTransaction(++acountLimit);
                                        return accountRepository.save(acountDestination )
                                                .flatMap(savedAccount -> {
                                                    TransactionDTO transactionDTO = new TransactionDTO();
                                                    transactionDTO.setAmount(amount.getMonto());
                                                    transactionDTO.setTransactionType(
                                                            TransactionDTO.TransactionType.DEPOSITO);
                                                    transactionDTO.setTransactionDate(LocalDate.now());
                                                    transactionDTO.setCustomerIdOrigin(accountOrigin .getCustomerId());
                                                    transactionDTO.setCommissionAmount(taxes);
                                                    transactionDTO.setDni(accountOrigin.getDni());
                                                    transactionDTO.setCustomerIdDestination(
                                                            acountDestination.getCustomerId());
                                                    return transactionRepository.save(transactionDTO)
                                                            .flatMap(savedTransaction ->
                                                                    accountRepository.save(accountOrigin)
                                                                    .thenReturn(savedAccount));
                                                });
                                    });
                        }))
                .map(accountConverter::toDto)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> logger.info("Successful deposit into account {}", id))
                .onErrorResume(Exception.class, e -> {
                    if (e instanceof BusinessException || e instanceof EntityNotFoundException) {
                        return Mono.error(e);
                    }
                    logger.error("Unexpected error during deposit: {}", e.getMessage());
                    return Mono.error(new InternalServerErrorException("Error processing deposit"));
                });
    }

    @Override
    public Mono<ResponseEntity<Account>> withdrawAmount(String customerId, WithdrawRequest amount) {
        return accountRepository.findFirstByCustomerId(customerId)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Account not found with id: " + customerId)))
                .flatMap(existingAccount -> {
                    logger.warn("Account found: {}", existingAccount.getId());
                    return comissionRepository.findByAccountType(existingAccount.getAccountType().name())
                            .defaultIfEmpty(new TaxedTransactionLimitDTO())
                            .flatMap(commission -> {
                                double commissionAmount = commission.getMonto() != null
                                        ? commission.getMonto().doubleValue() : 0.0;
                                double netAmount;
                                double taxes;
                                double acountLimit = existingAccount.getLimitTransaction();
                                if ( existingAccount.getBalance() < amount.getMonto() ) {
                                    return Mono.error(new BusinessException("insufficient balance"));
                                }
                                if (existingAccount.getLimitTransaction() > accountProperties.getTransaction()) {
                                    netAmount = amount.getMonto() - commissionAmount;
                                    if (netAmount < 0) {
                                        return Mono.error(
                                                new BusinessException("Net deposit amount cannot be negative"));
                                    }
                                    taxes = commission.getMonto();
                                    existingAccount.setBalance(existingAccount.getBalance() - netAmount);
                                } else {
                                    taxes = 0;
                                    existingAccount.setBalance(existingAccount.getBalance() - amount.getMonto());
                                }
                                existingAccount.setLimitTransaction(++acountLimit);
                                return accountRepository.save( existingAccount )
                                        .flatMap(savedAccount -> {
                                            TransactionDTO transactionDTO = new TransactionDTO();
                                            transactionDTO.setAmount(amount.getMonto());
                                            transactionDTO.setTransactionType(
                                                    TransactionDTO.TransactionType.RETIRO);
                                            transactionDTO.setTransactionDate(LocalDate.now());
                                            transactionDTO.setCustomerIdOrigin(existingAccount.getCustomerId());
                                            transactionDTO.setCommissionAmount(taxes);
                                            transactionDTO.setDni(existingAccount.getDni());
                                            transactionDTO.setCustomerIdDestination(null);
                                            return transactionRepository.save(transactionDTO)
                                                    .thenReturn(existingAccount);
                                        });

                            });
                })
                .map(accountConverter::toDto)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> logger.info("Successful withdrawal from account {}", customerId))
                .onErrorResume(Exception.class, e -> {
                    logger.error("Unexpected error during withdrawal: {}", e.getMessage());
                    return Mono.error(new InternalServerErrorException("Error processing withdrawal"));
                });
    }

    private Mono<Boolean> validateAccountUpdate(AccountEntityDTO existingAccount, AccountEntityDTO updatedAccount) {
        return accountRepository.findByCustomerId(existingAccount.getCustomerId())
                .collectList()
                .map(existingAccounts -> {
                    AccountEntityDTO.AccountType newType = AccountEntityDTO.AccountType
                            .valueOf(updatedAccount.getAccountType().name());
                    if (updatedAccount.getCustomerType() == AccountEntityDTO.CustomerType.EMPRESARIAL) {
                        boolean hasHolders = updatedAccount.getHolders() != null
                                && !updatedAccount.getHolders().isEmpty();
                        return hasHolders && newType == AccountEntityDTO.AccountType.CORRIENTE;
                    }
                    if (newType == AccountEntityDTO.AccountType.AHORRO ||
                            newType == AccountEntityDTO.AccountType.CORRIENTE) {
                        boolean existsSameType = existingAccounts.stream()
                                .anyMatch(acc -> acc.getAccountType() == newType &&
                                        !acc.getId().equals(existingAccount.getId()));
                        return !existsSameType;
                    }
                    if (newType == AccountEntityDTO.AccountType.PLAZO_FIJO) {
                        return updatedAccount.getCustomerType() == AccountEntityDTO.CustomerType.PERSONAL;
                    }
                    return false;
                });
    }

    private Mono<AccountEntityDTO> saveUpdatedAccount(AccountEntityDTO existingAccount,
                                                      AccountEntityDTO updatedAccount) {
        existingAccount.setAccountType(AccountEntityDTO.AccountType.valueOf(updatedAccount.getAccountType().name()));
        existingAccount.setBalance(updatedAccount.getBalance());
        existingAccount.setMonthlyLimit(updatedAccount.getMonthlyLimit());
        existingAccount.setLastDepositDate(updatedAccount.getLastDepositDate());
        return accountRepository.save(existingAccount);
    }
}
