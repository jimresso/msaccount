package com.nttdata.account.msaccount.service.impl;


import com.nttdata.account.msaccount.model.TaxedTransactionLimitDTO;

import com.nttdata.account.msaccount.repository.ComissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@TestPropertySource(locations = "classpath:application.properties")
class CommissionServiceImplTest {
    @Mock
    private ComissionRepository comissionRepository;

    @InjectMocks
    private CommissionServiceImpl commissionServiceImpl;

    @Test
    void deleteCommission_shouldReturnNoContent_whenExists() {
        String id = "commission-123";
        TaxedTransactionLimitDTO dto = new TaxedTransactionLimitDTO();
        dto.setMonto(333.44);
        dto.setCustomerId("3344455");
        dto.setAccountType(TaxedTransactionLimitDTO.AccountType.AHORRO);
        dto.setId(id);
        when(comissionRepository.findById(id)).thenReturn(Mono.just(dto));
        when(comissionRepository.delete(dto)).thenReturn(Mono.empty());
        StepVerifier.create(commissionServiceImpl.deleteCommission(id))
                .expectNextMatches(response -> response.getStatusCode().equals(HttpStatus.NO_CONTENT))
                .verifyComplete();
    }

//    @Test
//    void deleteCommission_shouldReturnNoContent_NoExists() {
//        String id = "commission-123";
//        TaxedTransactionLimitDTO dto = new TaxedTransactionLimitDTO();
//        dto.setMonto(333.44);
//        dto.setCustomerId("3344455");
//        dto.setAccountType(TaxedTransactionLimitDTO.AccountType.AHORRO);
//        dto.setId(id);
//        when(comissionRepository.findById(id)).thenReturn(Mono.just(dto));
//        StepVerifier.create(commissionServiceImpl.deleteCommission(id))
//                .expectNextMatches(response -> response.getStatusCode().equals(HttpStatus.NO_CONTENT))
//                .verifyComplete();
//    }
}