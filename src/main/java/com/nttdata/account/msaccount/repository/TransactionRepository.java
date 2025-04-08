package com.nttdata.account.msaccount.repository;

import com.nttdata.account.msaccount.model.TransactionDTO;
import org.openapitools.model.ReportOperationsResponse;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

import java.util.Locale;


public interface TransactionRepository extends ReactiveMongoRepository<TransactionDTO, String> {
    Flux<TransactionDTO> findByDni(String dni);

    Flux<TransactionDTO>findByCustomerIdOrigin(String customerId);
}
