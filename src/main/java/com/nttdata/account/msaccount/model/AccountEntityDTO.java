package com.nttdata.account.msaccount.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Id;
import java.time.LocalDate;
import java.util.List;

@Document(collection = "accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountEntityDTO {

    @Id
    private String id;
    private String dni;
    private String customerId;
    private CustomerType customerType;
    private AccountType accountType;
    private Double balance;
    private Integer monthlyLimit;
    private LocalDate lastDepositDate;
    private List<String> holders;
    private Double limitTransaction;
    private ClientType clientType;

    public enum AccountType {
        AHORRO,
        CORRIENTE,
        PLAZO_FIJO
    }
    public enum CustomerType {
        PERSONAL,
        EMPRESARIAL
    }
    public enum ClientType {
        VIP,
        PYME,
    }
}