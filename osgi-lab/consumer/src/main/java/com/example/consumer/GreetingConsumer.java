package com.example.consumer;

import com.example.provider.GreetingService;

import java.util.function.Consumer;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
public class GreetingConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(Consumer.class);
    
    // SCR will find the service in the registry and inject it here!
    @Reference
    private GreetingService greetingService;

    @Activate
    public void start() {
        LOG.info("=========================================");
        LOG.info("[CONSUMER WAKING UP] Calling the provider...");
        
        String message = greetingService.sayHello("OpenNMS Developer");
        
        LOG.info("[RESPONSE]: {}", message);
        LOG.info("=========================================");
    }

    @Deactivate
    public void stop() {
        System.out.println("[CONSUMER] Oh no! The Provider is gone. Going to sleep.");
    }
}
