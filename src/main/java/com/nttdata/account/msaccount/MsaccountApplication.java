package com.nttdata.account.msaccount;

import com.nttdata.account.msaccount.configure.AccountProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableConfigurationProperties(AccountProperties.class)
public class MsaccountApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsaccountApplication.class, args);
	}

}
