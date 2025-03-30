package com.nttdata.account.msaccount.mapper;

import com.nttdata.account.msaccount.model.AccountEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.openapitools.model.Account;

@Mapper(componentModel = "spring")
public interface AccountConverter {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "customerId", source = "customerId")
    @Mapping(target = "customerType", source = "customerType")
    @Mapping(target = "accountType", source = "accountType")
    @Mapping(target = "balance", source = "balance")
    @Mapping(target = "monthlyLimit", source = "monthlyLimit")
    @Mapping(target = "lastDepositDate", source = "lastDepositDate")
    @Mapping(target = "holders", source = "holders")
    AccountEntity toEntity(Account account);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "customerId", source = "customerId")
    @Mapping(target = "customerType", source = "customerType")
    @Mapping(target = "accountType", source = "accountType")
    @Mapping(target = "balance", source = "balance")
    @Mapping(target = "monthlyLimit", source = "monthlyLimit")
    @Mapping(target = "lastDepositDate", source = "lastDepositDate")
    @Mapping(target = "holders", source = "holders")
    Account toDto(AccountEntity accountEntity);
}