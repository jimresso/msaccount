package com.nttdata.account.msaccount.configure;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "msaccount")
@Getter
@Setter
public class AccountProperties {
    private Double vip;
    private Double pemy;
    private int transaction;

}