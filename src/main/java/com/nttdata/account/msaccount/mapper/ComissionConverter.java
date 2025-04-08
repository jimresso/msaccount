package com.nttdata.account.msaccount.mapper;

import com.nttdata.account.msaccount.model.TaxedTransactionLimitDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.openapitools.model.TaxedTransactionLimit;

@Mapper(componentModel = "spring")
public interface ComissionConverter {
    @Mapping(target = "id", source = "id")
    @Mapping(target = "accountType", source = "accountType")
    @Mapping(target = "monto", source = "monto")
    TaxedTransactionLimitDTO toEntity(TaxedTransactionLimit taxedTransactionLimit);
    @Mapping(target = "id", source = "id")
    @Mapping(target = "accountType", source = "accountType")
    @Mapping(target = "monto", source = "monto")
    TaxedTransactionLimit toDto(TaxedTransactionLimitDTO taxedTransactionLimitDTO);

}
