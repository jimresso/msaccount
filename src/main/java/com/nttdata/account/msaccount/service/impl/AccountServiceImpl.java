package com.nttdata.account.msaccount.service.impl;


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
import com.nttdata.account.msaccount.service.AccountService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.openapitools.model.Account;
import org.openapitools.model.DepositRequest;
import org.openapitools.model.WithdrawRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    @Autowired
    private WebClient.Builder webClientBuilder;
    private final AccountRepository accountRepository;
    private final AccountConverter accountConverter;
    private final TransactionRepository transactionRepository;
    private final ComissionRepository comissionRepository;
    private static final Logger logger = LoggerFactory.getLogger(AccountServiceImpl.class);
    @Value("${msaccount.amount.mini.vip}")
    private Double amountMiniVip;
    @Value("${msaccount.amount.mini.pemy}")
    private Double amountMinipemy;
    @Value("${limit.transaction}")
    private int limit;
    @PostConstruct
    public void init() {
        if (webClientBuilder != null) {
            System.out.println("WebClient.Builder inyectado correctamente.");
        } else {
            System.out.println("WebClient.Builder no está inyectado.");
        }
    }


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
@Override
public Mono<ResponseEntity<Account>> newAccount(Account account) {
    WebClient webClient = webClientBuilder.build();
    AccountEntityDTO accountEntityDTO = accountConverter.toEntity(account);
    if (accountEntityDTO.getClientType() == AccountEntityDTO.ClientType.VIP ||
            accountEntityDTO.getClientType() == AccountEntityDTO.ClientType.PYME) {
        if (accountEntityDTO.getClientType() == AccountEntityDTO.ClientType.VIP &&
                accountEntityDTO.getBalance() < amountMiniVip) {
            logger.warn("The initial amount {} is less than the minimum allowed " +
                    "amount of {} for VIP accounts", accountEntityDTO.getBalance(), amountMiniVip);
            return Mono.error(new BusinessException("The initial amount must be greater " +
                    "than or equal to " + amountMiniVip + " for VIP accounts."));
        } else if (accountEntityDTO.getClientType() == AccountEntityDTO.ClientType.PYME &&
                accountEntityDTO.getBalance() < amountMinipemy) {
            logger.warn("The initial amount {} is less than the minimum allowed" +
                    " amount of {} for PYME accounts", accountEntityDTO.getBalance(), amountMinipemy);
            return Mono.error(new BusinessException("The initial amount must be greater" +
                    " than or equal to " + amountMinipemy + " for PYME accounts."));
        }
        return accountRepository.findByDni(account.getDni())
                .collectList()
                .flatMap(existingAccounts -> {
                    List<String> customerIds = existingAccounts.stream()
                            .map(AccountEntityDTO::getCustomerId)
                            .collect(Collectors.toList());
                    Map<String, Object> request = new HashMap<>();
                    request.put("customerId", customerIds);
                    return webClient.post()
                            .bodyValue(request)
                            .retrieve()
                            .bodyToMono(Map.class)
                            .flatMap(response -> {
                                Boolean hasCreditCard = (Boolean) response.get("creditCard");
                                if (Boolean.TRUE.equals(hasCreditCard)) {
                                    return createAccount(accountEntityDTO);
                                } else {
                                    logger.warn("Customer with DNI {} does not have a credit card", account.getDni());
                                    return Mono.error(new BusinessException("Customer does not have a credit card," +
                                            " cannot create VIP or PYME account"));
                                }
                            });
                });
    }
    return createAccount(accountEntityDTO);
}
    private Mono<ResponseEntity<Account>> createAccount(AccountEntityDTO accountEntityDTO) {
        return validateAccountCreation(accountEntityDTO)
                .flatMap(valid -> {
                    if (Boolean.FALSE.equals(valid)) {
                        logger.warn("The account does not meet the requirements");
                        return Mono.error(new BusinessException("Account creation does not meet business rules"));
                    }
                    return accountRepository.save(accountEntityDTO)
                            .map(accountConverter::toDto)
                            .map(savedAccount -> ResponseEntity.status(HttpStatus.CREATED).body(savedAccount))
                            .onErrorResume(e -> {
                                logger.error("Error saving account for Customer ID: " +
                                        "{}. Error: {}", accountEntityDTO.getCustomerId(), e.getMessage());
                                return Mono.error(
                                        new InternalServerErrorException("An error occurred while saving the account"));
                            });
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
                                        double acountLimit = accountOrigin .getLimitTransaction();
                                        if ( accountOrigin.getBalance() < amount.getMonto() ) {
                                            return Mono.error(new BusinessException("insufficient balance"));
                                        } else {
                                            accountOrigin.setBalance(accountOrigin.getBalance() - amount.getMonto());
                                            accountOrigin .setLimitTransaction(++acountLimit);
                                        }
                                        if (accountOrigin .getLimitTransaction() > limit) {
                                            netAmount = amount.getMonto() - commissionAmount;
                                            if (netAmount < 0) {
                                                return Mono.error(
                                                        new BusinessException("Net deposit amount cannot be negative"));
                                            }
                                            accountOrigin.setBalance(accountOrigin.getBalance() - netAmount);
                                            acountDestination .setBalance(acountDestination .getBalance() + netAmount);
                                        } else {
                                            netAmount = 0;
                                            accountOrigin.setBalance(accountOrigin.getBalance() - amount.getMonto());
                                            acountDestination .setBalance(acountDestination .getBalance() +
                                                    amount.getMonto());
                                        }
                                        return accountRepository.save(acountDestination )
                                                .flatMap(savedAccount -> {
                                                    TransactionDTO transactionDTO = new TransactionDTO();
                                                    transactionDTO.setAmount(amount.getMonto());
                                                    transactionDTO.setTransactionType(
                                                            TransactionDTO.TransactionType.DEPOSITO);
                                                    transactionDTO.setTransactionDate(LocalDate.now());
                                                    transactionDTO.setCustomerIdOrigin(accountOrigin .getCustomerId());
                                                    transactionDTO.setCommissionAmount(netAmount);
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
                    logger.error("Unexpected error during deposit: {}", e.getMessage());
                    return Mono.error(new InternalServerErrorException("Error processing deposit"));
                });
    }

    @Override
    public Mono<ResponseEntity<Account>> withdrawAmount(String id, WithdrawRequest amount) {
        return accountRepository.findById(id)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Account not found with id: " + id)))
                .flatMap(existingAccount -> {
                    logger.warn("Account found: {}", existingAccount.getId());


                    return accountRepository.save(existingAccount);
                })
                .map(accountConverter::toDto)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> logger.info("Successful withdrawal from account {}", id))
                .onErrorResume(Exception.class, e -> {
                    logger.error("Unexpected error during withdrawal: {}", e.getMessage());
                    return Mono.error(new InternalServerErrorException("Error processing withdrawal"));
                });
    }

    private Mono<Boolean> validateAccountCreation(AccountEntityDTO accountEntityDTO) {
        return accountRepository.findByCustomerId(accountEntityDTO.getCustomerId())
                .collectList()
                .map(existingAccounts -> {
                    AccountEntityDTO.AccountType accountType = accountEntityDTO.getAccountType();
                    if (accountEntityDTO.getCustomerType() == AccountEntityDTO.CustomerType.EMPRESARIAL) {
                        boolean hasHolders = accountEntityDTO.getHolders() != null &&
                                !accountEntityDTO.getHolders().isEmpty();
                        return hasHolders && accountType != AccountEntityDTO.AccountType.AHORRO && accountType
                                != AccountEntityDTO.AccountType.PLAZO_FIJO;
                    }
                    if (accountType == AccountEntityDTO.AccountType.AHORRO ||
                            accountType == AccountEntityDTO.AccountType.CORRIENTE) {
                        boolean existsSameType = existingAccounts.stream()
                                .anyMatch(acc -> acc.getAccountType() == accountType);
                        return !existsSameType;
                    }
                    if (accountType == AccountEntityDTO.AccountType.PLAZO_FIJO) {
                        return accountEntityDTO.getCustomerType() == AccountEntityDTO.CustomerType.PERSONAL;
                    }
                    return false;
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
