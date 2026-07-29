package com.mydomain.env;

import java.util.Map;

public interface EnvService {
    /**
     * Retrieves a specific environment variable by key.
     */
    String getEnv(String key);

    /**
     * Retrieves all environment variables currently visible to the JVM.
     */
    Map<String, String> getAllEnv();
}