package com.nttdata.account.msaccount.mapper;

import com.nttdata.account.msaccount.model.AccountEntityDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.openapitools.model.Account;

@Mapper(componentModel = "spring")
public interface AccountConverter {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "dni", source = "dni")
    @Mapping(target = "customerId", source = "customerId")
    @Mapping(target = "customerType", source = "customerType")
    @Mapping(target = "accountType", source = "accountType")
    @Mapping(target = "balance", source = "balance")
    @Mapping(target = "monthlyLimit", source = "monthlyLimit")
    @Mapping(target = "lastDepositDate", source = "lastDepositDate")
    @Mapping(target = "holders", source = "holders")
    @Mapping(target = "limitTransaction", source = "limitTransaction")
    @Mapping(target = "clientType", source = "clientType")
    AccountEntityDTO toEntity(Account account);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "dni", source = "dni")
    @Mapping(target = "customerId", source = "customerId")
    @Mapping(target = "customerType", source = "customerType")
    @Mapping(target = "accountType", source = "accountType")
    @Mapping(target = "balance", source = "balance")
    @Mapping(target = "monthlyLimit", source = "monthlyLimit")
    @Mapping(target = "lastDepositDate", source = "lastDepositDate")
    @Mapping(target = "holders", source = "holders")
    @Mapping(target = "limitTransaction", source = "limitTransaction")
    @Mapping(target = "clientType", source = "clientType")
    Account toDto(AccountEntityDTO accountEntityDTO);
}