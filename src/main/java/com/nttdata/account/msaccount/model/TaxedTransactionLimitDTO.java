package com.nttdata.account.msaccount.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "comission")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaxedTransactionLimitDTO {
    private String id;
    private AccountType accountType;
    private Double monto;
    private String customerId;

    public enum AccountType {
        AHORRO,
        CORRIENTE,
        PLAZO_FIJO
    }
}