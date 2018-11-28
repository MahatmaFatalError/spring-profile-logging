/*
 * Copyright (C)  2018 Miroslav Wengner
 *                        http://www.wengnermiro.com/
 *
 *  This software is free:
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESSED OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *   OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *   IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 *   NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *   DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *   THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 *   THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *   Copyright (C) Miroslav Wengner, 2018
 */

package com.mirowengner.example.producer;

import com.mirowengner.example.consumer.ConsumerApp;
import com.mirowengner.example.consumer.config.CustomTracerConfig;
import com.mirowengner.example.consumer.model.VehicleModel;
import io.opentracing.contrib.spring.tracer.configuration.TracerRegisterAutoConfiguration;
import io.opentracing.contrib.spring.web.starter.ServerTracingAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SimpleClientApp simple client application that calls {@link ConsumerApp}
 * <p>
 * <p>
 * run local: -Dspring.application.name=producer -Dserver.port=8082
 * run docker compose : use environment
 * variables: APPLICATION_NAME=producer;DEMO_PORT=8082;OPENTRACING_HOST=jaeger;OPENTRACING_PORT=6831
 *
 * @author Miroslav Wengner (@miragemiko)
 */

@EnableAutoConfiguration(exclude = {TracerRegisterAutoConfiguration.class, ServerTracingAutoConfiguration.class})
@EnableScheduling
@RestController
@Import(value = CustomTracerConfig.class)
public class ProducerApp {


    private static final Random RANDOM = new Random();
    private final AtomicInteger vehicleNumber = new AtomicInteger();

    @Value("${tracing.consumer.url:http://localhost:8081}")
    private String consumerServiceUrl;


    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }

    @Autowired
    private RestTemplate restTemplate;


    @Scheduled(fixedRate = 10000)
    public void postNewVehicle() {
        VehicleModel vehicle = new VehicleModel();
        vehicle.setName("vehicle" + vehicleNumber.getAndIncrement());
        ResponseEntity<VehicleModel> response = restTemplate.postForEntity(consumerServiceUrl + "/shop/models/vehicle", vehicle, VehicleModel.class);

    }

    @Scheduled(fixedRate = 3000)
    public void getVehicles() {
        ResponseEntity<List> vehicles = restTemplate.getForEntity(consumerServiceUrl + "/shop/models", List.class);
    }

    @Scheduled(initialDelay = 2000, fixedRate = 20000)
    public void putUpdateVehicle() {
        final int vehicleId = RANDOM.nextInt(vehicleNumber.get());

        HttpHeaders header = new HttpHeaders();
        header.add("Content-Type", "application/json");
        HttpEntity<VehicleModel> httpEntity = new HttpEntity<>(header);
        final ResponseEntity<VehicleModel> response = restTemplate.exchange(consumerServiceUrl + "/shop/models/vehicle?id={id}", HttpMethod.GET,
                httpEntity, new ParameterizedTypeReference<VehicleModel>() {
                }, vehicleId);

        final VehicleModel vehicle = response.getBody();
        vehicle.setName(vehicle.getName() + "u");
        restTemplate.exchange(consumerServiceUrl + "/shop/models/vehicle", HttpMethod.PUT, new HttpEntity<>(vehicle), VehicleModel.class);
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/client", method = RequestMethod.GET, produces = {"application/json"})
    @ResponseBody
    public List<VehicleModel> getShopInfo() {

        final ResponseEntity<List<VehicleModel>> response = restTemplate
                .exchange(consumerServiceUrl + "/shop/models", HttpMethod.GET, null,
                        new ParameterizedTypeReference<List<VehicleModel>>() {
                        });

        return response.getBody();

    }


    public static void main(String[] args) {
        SpringApplication.run(ProducerApp.class, args);
    }


}
