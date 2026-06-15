package com.tracenotify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
public class TraceNotifyApplication {

    public static void main(String[] args) {
        SpringApplication.run(TraceNotifyApplication.class, args);
    }
}
