package com.neu.myapp.Config;

import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsClientBean {
    @Value("${publish.metrics}")
    private boolean publishMetrics;

    @Value("${metrics.server.hostname}")
    private String metricsServerHost;

    @Value("${metrics.server.port}")
    private int metricsServerPort;

    @Value("csye6225")
    private String prefix;

    @Bean
    public StatsDClient metricsClient() {

        if (publishMetrics) {
            return new NonBlockingStatsDClient(prefix, metricsServerHost, metricsServerPort);
        }

        return new NoOpStatsDClient();
    }
}
