package com.andesstay.ms_andesstay_rabbitadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MsAndesstayRabbitadminApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsAndesstayRabbitadminApplication.class, args);
	}

}
