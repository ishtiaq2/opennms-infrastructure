package com.mydomain.env;

import com.mydomain.env.EnvService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

// @Component registers this class as a service in the OSGi Service Registry
@Component(immediate = true)
public class EnvServiceImpl implements EnvService {

    private static final Logger LOG = LoggerFactory.getLogger(EnvServiceImpl.class);

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