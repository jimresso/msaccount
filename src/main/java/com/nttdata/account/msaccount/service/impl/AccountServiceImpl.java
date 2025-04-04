package com.nttdata.account.msaccount.service.impl;


import com.nttdata.account.msaccount.exception.BusinessException;
import com.nttdata.account.msaccount.exception.EntityNotFoundException;
import com.nttdata.account.msaccount.exception.InsufficientFundsException;
import com.nttdata.account.msaccount.exception.InternalServerErrorException;
import com.nttdata.account.msaccount.mapper.AccountConverter;
import com.nttdata.account.msaccount.model.AccountEntity;
import com.nttdata.account.msaccount.repository.AccountRepository;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
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
    private static final Logger logger = LoggerFactory.getLogger(AccountServiceImpl.class);
    @Value("${msaccount.amount.mini.vip}")
    private Double amountMiniVip;
    @Value("${msaccount.amount.mini.pemy}")
    private Double amountMinipemy;

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
                    return Mono.error(new InternalServerErrorException("Unexpected error occurred while retrieving account"));
                });
    }
@Override
public Mono<ResponseEntity<Account>> newAccount(Account account) {
    WebClient webClient = webClientBuilder.build();
    AccountEntity accountEntity = accountConverter.toEntity(account);
    if (accountEntity.getClientType() == AccountEntity.ClientType.VIP || accountEntity.getClientType() == AccountEntity.ClientType.PYME) {
        if (accountEntity.getClientType() == AccountEntity.ClientType.VIP && accountEntity.getBalance() < amountMiniVip) {
            logger.warn("The initial amount {} is less than the minimum allowed amount of {} for VIP accounts", accountEntity.getBalance(), amountMiniVip);
            return Mono.error(new BusinessException("The initial amount must be greater than or equal to " + amountMiniVip + " for VIP accounts."));
        } else if (accountEntity.getClientType() == AccountEntity.ClientType.PYME && accountEntity.getBalance() < amountMinipemy) {
            logger.warn("The initial amount {} is less than the minimum allowed amount of {} for PYME accounts", accountEntity.getBalance(), amountMinipemy);
            return Mono.error(new BusinessException("The initial amount must be greater than or equal to " + amountMinipemy + " for PYME accounts."));
        }
        return accountRepository.findByDni(account.getDni())
                .collectList()
                .flatMap(existingAccounts -> {
                    List<String> customerIds = existingAccounts.stream()
                            .map(AccountEntity::getCustomerId)
                            .collect(Collectors.toList());
                    Map<String, Object> request = new HashMap<>();
                    request.put("customerId", customerIds);
                    return webClient.post()
                            .uri("http://localhost:8087/creditcards/exists")
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(request)
                            .retrieve()
                            .bodyToMono(Map.class)
                            .flatMap(response -> {
                                Boolean hasCreditCard = (Boolean) response.get("creditCard");
                                if (Boolean.TRUE.equals(hasCreditCard)) {
                                    return createAccount(accountEntity);
                                } else {
                                    logger.warn("Customer with DNI {} does not have a credit card", account.getDni());
                                    return Mono.error(new BusinessException("Customer does not have a credit card, cannot create VIP or PYME account"));
                                }
                            });
                });
    }
    return createAccount(accountEntity);
}
    private Mono<ResponseEntity<Account>> createAccount(AccountEntity accountEntity) {
        return validateAccountCreation(accountEntity)
                .flatMap(valid -> {
                    if (Boolean.FALSE.equals(valid)) {
                        logger.warn("The account does not meet the requirements");
                        return Mono.error(new BusinessException("Account creation does not meet business rules"));
                    }
                    return accountRepository.save(accountEntity)
                            .map(accountConverter::toDto)
                            .map(savedAccount -> ResponseEntity.status(HttpStatus.CREATED).body(savedAccount))
                            .onErrorResume(e -> {
                                logger.error("Error saving account for Customer ID: {}. Error: {}", accountEntity.getCustomerId(), e.getMessage());
                                return Mono.error(new InternalServerErrorException("An error occurred while saving the account"));
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
        AccountEntity updatedAccountEntity = accountConverter.toEntity(updatedAccount);
        return accountRepository.findById(accountId)
                .flatMap(existingAccount -> {
                    boolean typeChanged = !existingAccount.getAccountType().equals(updatedAccountEntity.getAccountType());
                    boolean customerTypeChanged = !existingAccount.getCustomerType().equals(updatedAccountEntity.getCustomerType());
                    if (typeChanged || customerTypeChanged) {
                        return validateAccountUpdate(existingAccount, updatedAccountEntity)
                                .flatMap(isValid -> {
                                    if (Boolean.FALSE.equals(isValid)) {
                                        return Mono.error(new BusinessException("Account update does not meet business rules"));
                                    }
                                    return saveUpdatedAccount(existingAccount, updatedAccountEntity);
                                });
                    }
                    return saveUpdatedAccount(existingAccount, updatedAccountEntity);
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
                .flatMap(existingAccount -> {
                    logger.warn("Account found: {}", existingAccount.getId());
                    existingAccount.setBalance(existingAccount.getBalance() + amount.getMonto());
                    return accountRepository.save(existingAccount);
                })
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
                    if (existingAccount.getBalance() < amount.getMonto()) {
                        return Mono.error(new InsufficientFundsException("Insufficient funds in account " + id));
                    }
                    existingAccount.setBalance(existingAccount.getBalance() - amount.getMonto());
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

    private Mono<Boolean> validateAccountCreation(AccountEntity accountEntity) {
        return accountRepository.findByCustomerId(accountEntity.getCustomerId())
                .collectList()
                .map(existingAccounts -> {
                    AccountEntity.AccountType accountType = accountEntity.getAccountType();
                    if (accountEntity.getCustomerType() == AccountEntity.CustomerType.EMPRESARIAL) {
                        boolean hasHolders = accountEntity.getHolders() != null && !accountEntity.getHolders().isEmpty();
                        return hasHolders && accountType != AccountEntity.AccountType.AHORRO && accountType != AccountEntity.AccountType.PLAZO_FIJO;
                    }
                    if (accountType == AccountEntity.AccountType.AHORRO || accountType == AccountEntity.AccountType.CORRIENTE) {
                        boolean existsSameType = existingAccounts.stream()
                                .anyMatch(acc -> acc.getAccountType() == accountType);
                        return !existsSameType;
                    }
                    if (accountType == AccountEntity.AccountType.PLAZO_FIJO) {
                        return accountEntity.getCustomerType() == AccountEntity.CustomerType.PERSONAL;
                    }
                    return false;
                });
    }

    private Mono<Boolean> validateAccountUpdate(AccountEntity existingAccount, AccountEntity updatedAccount) {
        return accountRepository.findByCustomerId(existingAccount.getCustomerId())
                .collectList()
                .map(existingAccounts -> {
                    AccountEntity.AccountType newType = AccountEntity.AccountType.valueOf(updatedAccount.getAccountType().name());
                    if (updatedAccount.getCustomerType() == AccountEntity.CustomerType.EMPRESARIAL) {
                        boolean hasHolders = updatedAccount.getHolders() != null && !updatedAccount.getHolders().isEmpty();
                        return hasHolders && newType == AccountEntity.AccountType.CORRIENTE;
                    }
                    if (newType == AccountEntity.AccountType.AHORRO || newType == AccountEntity.AccountType.CORRIENTE) {
                        boolean existsSameType = existingAccounts.stream()
                                .anyMatch(acc -> acc.getAccountType() == newType && !acc.getId().equals(existingAccount.getId()));
                        return !existsSameType;
                    }
                    if (newType == AccountEntity.AccountType.PLAZO_FIJO) {
                        return updatedAccount.getCustomerType() == AccountEntity.CustomerType.PERSONAL;
                    }
                    return false;
                });
    }

    private Mono<AccountEntity> saveUpdatedAccount(AccountEntity existingAccount, AccountEntity updatedAccount) {
        existingAccount.setAccountType(AccountEntity.AccountType.valueOf(updatedAccount.getAccountType().name()));
        existingAccount.setBalance(updatedAccount.getBalance());
        existingAccount.setMonthlyLimit(updatedAccount.getMonthlyLimit());
        existingAccount.setLastDepositDate(updatedAccount.getLastDepositDate());
        return accountRepository.save(existingAccount);
    }
}
