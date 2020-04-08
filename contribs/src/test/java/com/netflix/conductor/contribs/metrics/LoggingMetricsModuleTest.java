package com.netflix.conductor.contribs.metrics;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.codahale.metrics.Slf4jReporter;
import com.netflix.conductor.core.config.Configuration;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.slf4j.Logger;

public class LoggingMetricsModuleTest {

    @Test
    public void testCollector() {
        Logger logger = mock(Logger.class);
        doReturn(true).when(logger).isInfoEnabled(null);

        Configuration cfg = mock(Configuration.class);
        doReturn(1L).when(cfg).getLongProperty(anyString(), anyLong());

        LoggingMetricsModule.Slf4jReporterProvider logMetrics =
                new LoggingMetricsModule.Slf4jReporterProvider(cfg, MetricsRegistryModule.METRIC_REGISTRY, logger);

        MetricsRegistryModule.METRIC_REGISTRY.counter("test").inc();
        Slf4jReporter slf4jReporter = logMetrics.get();

        verify(logger, timeout(TimeUnit.SECONDS.toMillis(10))).isInfoEnabled(null);
    }
}