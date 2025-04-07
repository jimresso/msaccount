package com.nttdata.account.msaccount.model;


import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Data
@Document(collection = "transactions")
public class TransactionDTO {
    private String customerIdOrigin;
    private String customerIdDestination ;
    private Double amount;
    private LocalDate transactionDate;
    private TransactionType transactionType;
    private double commissionAmount;

    public enum TransactionType {
        DEPOSITO, RETIRO
    }
}
