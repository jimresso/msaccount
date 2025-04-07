package com.nttdata.account.msaccount.service;


import org.openapitools.model.ReportOperationsRequest;
import org.openapitools.model.ReportOperationsResponse;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

public interface ReportAccountService {
    Mono<ResponseEntity<ReportOperationsResponse>> reportAccount(String a);

}
