package com.nttdata.account.msaccount.service;


import org.openapitools.model.ReportOperationsResponse;
import org.openapitools.model.ReportProductoRequest;
import org.openapitools.model.ReportProductoResponse;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReportAccountService {
    Mono<ResponseEntity<ReportOperationsResponse>> reportAccount(String a);
    Mono<ResponseEntity<Flux<ReportProductoResponse>>> reportProduct(ReportProductoRequest c);
}

