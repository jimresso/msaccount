package com.nttdata.account.msaccount.repository;

import com.nttdata.account.msaccount.model.TransactionDTO;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface TransactionRepository extends ReactiveMongoRepository<TransactionDTO, String> {
}
