package com.mydomain.env;

import com.mydomain.env.EnvService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @Component: 
 * This registers EnvServiceImpl as an OSGi service in the OSGi Service Registry implementing EnvService. 
 * immediate = true means the component is instantiated and 
 * activated as soon as it's satisfied (its dependencies are met), rather 
 * than waiting for the first consumer to request the service.
 */
@Component(immediate = true)
public class EnvServiceImpl implements EnvService {

    private static final Logger LOG = LoggerFactory.getLogger(EnvServiceImpl.class);


    /**
     * NOTE: Dumping env variables to log is a security risk. 
     * This is just for illustration purpose and should not be practiced in production.
     */
    /**
     * The @Activate method is triggered by the OSGi SCR (Service Component Runtime)
     * the exact moment this bundle is successfully deployed and started.
     */
    @Activate
    public void activate() {
        LOG.info("=========================================");
        LOG.info("[ENV PROVIDER] Bundle Started!");
        LOG.info("Printing all system environment variables:");
        
        Map<String, String> envVars = System.getenv();
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            LOG.info("{} = {}", entry.getKey(), entry.getValue());
        }
        
        LOG.info("=========================================");
    }

    @Override
    public String getEnv(String key) {
        return System.getenv(key);
    }

    @Override
    public Map<String, String> getAllEnv() {
        return System.getenv();
    }
}