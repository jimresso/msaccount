package com.nttdata.account.msaccount.controller;

import com.nttdata.account.msaccount.service.ReportAccountService;
import lombok.RequiredArgsConstructor;
import org.openapitools.api.ReportApi;
import org.openapitools.model.ReportOperationsRequest;
import org.openapitools.model.ReportOperationsResponse;
import org.openapitools.model.ReportProductoRequest;
import org.openapitools.model.ReportProductoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@RestController
@RequiredArgsConstructor
public class ReportAccountImpl implements ReportApi {
    private static final Logger logger = LoggerFactory.getLogger(ReportAccountImpl.class);
    private final ReportAccountService reportAccountService;


    @Override
    public Mono<ResponseEntity<ReportOperationsResponse>> reportOperations(
            Mono<ReportOperationsRequest> reportOperationsRequest, ServerWebExchange exchange) {
        logger.info("Starting reportOperations");
        return reportOperationsRequest.flatMap(a ->
                reportAccountService.reportAccount(a.getDni()));
    }

    @Override
    public Mono<ResponseEntity<Flux<ReportProductoResponse>>> reportProduct(
            Mono<ReportProductoRequest> reportProductoRequest, ServerWebExchange exchange) {
        logger.info("Starting reportProduct");
        return  reportProductoRequest.flatMap(reportAccountService::reportProduct);
    }

}
