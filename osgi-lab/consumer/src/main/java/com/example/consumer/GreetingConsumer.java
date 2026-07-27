package com.example.consumer;

import com.example.provider.GreetingService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true)
public class GreetingConsumer {

    // SCR will find the service in the registry and inject it here!
    @Reference
    private GreetingService greetingService;

    @Activate
    public void start() {
        System.out.println("=========================================");
        System.out.println("[CONSUMER WAKING UP] Calling the provider...");
        System.out.println(greetingService.sayHello("OpenNMS Developer"));
        System.out.println("=========================================");
    }

    @Deactivate
    public void stop() {
        System.out.println("[CONSUMER] Oh no! The Provider is gone. Going to sleep.");
    }
}
