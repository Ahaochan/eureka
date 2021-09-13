package com.netflix.discovery.shared.transport.jersey;

import java.util.Collection;

import com.netflix.appinfo.EurekaClientIdentity;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.resolver.EurekaEndpoint;
import com.netflix.discovery.shared.transport.EurekaHttpClient;
import com.netflix.discovery.shared.transport.TransportClientFactory;
import com.netflix.discovery.shared.transport.decorator.MetricsCollectingEurekaHttpClient;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.client.apache4.ApacheHttpClient4;

public class Jersey1TransportClientFactories implements TransportClientFactories<ClientFilter> {
    @Deprecated
    public TransportClientFactory newTransportClientFactory(final Collection<ClientFilter> additionalFilters,
                                                                   final EurekaJerseyClient providedJerseyClient) {
        ApacheHttpClient4 apacheHttpClient = providedJerseyClient.getClient();
        if (additionalFilters != null) {
            for (ClientFilter filter : additionalFilters) {
                if (filter != null) {
                    apacheHttpClient.addFilter(filter);
                }
            }
        }

        final TransportClientFactory jerseyFactory = new JerseyEurekaHttpClientFactory(providedJerseyClient, false);
        final TransportClientFactory metricsFactory = MetricsCollectingEurekaHttpClient.createFactory(jerseyFactory);

        return new TransportClientFactory() {
            @Override
            public EurekaHttpClient newClient(EurekaEndpoint serviceUrl) {
                return metricsFactory.newClient(serviceUrl);
            }

            @Override
            public void shutdown() {
                metricsFactory.shutdown();
                jerseyFactory.shutdown();
            }
        };
    }

    public TransportClientFactory newTransportClientFactory(final EurekaClientConfig clientConfig,
                                                                   final Collection<ClientFilter> additionalFilters,
                                                                   final InstanceInfo myInstanceInfo) {
        // 里面创建了JerseyApplicationClient, 这才是最终的EurekaHttpClient
        final TransportClientFactory jerseyFactory = JerseyEurekaHttpClientFactory.create(
                clientConfig,
                additionalFilters,
                myInstanceInfo,
                new EurekaClientIdentity(myInstanceInfo.getIPAddr())
        );

        // 里面一通操作，还是调用上面的jerseyFactory
        final TransportClientFactory metricsFactory = MetricsCollectingEurekaHttpClient.createFactory(jerseyFactory);

        return new TransportClientFactory() {
            @Override
            public EurekaHttpClient newClient(EurekaEndpoint serviceUrl) {
                return metricsFactory.newClient(serviceUrl); // 又在绕! 走上面的metricsFactory
            }

            @Override
            public void shutdown() {
                metricsFactory.shutdown();
                jerseyFactory.shutdown();
            }
        };
    }
}
