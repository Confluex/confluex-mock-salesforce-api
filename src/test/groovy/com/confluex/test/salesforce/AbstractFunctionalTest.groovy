package com.confluex.test.salesforce

import com.confluex.mule.test.http.MockHttpsServer
import com.sun.jersey.api.client.Client
import com.sun.jersey.api.client.config.ClientConfig
import com.sun.jersey.api.client.config.DefaultClientConfig
import com.sun.jersey.client.urlconnection.HTTPSProperties
import org.junit.After
import org.junit.Before

class AbstractFunctionalTest {
    protected MockSalesforceApiServer server
    protected Client sslClient

    @Before
    void initServer() {
        server = new MockSalesforceApiServer(8090)
    }

    @After
    void stopServer() {
        server.stop()
    }

    @Before
    void initClient() {
        ClientConfig config = new DefaultClientConfig()
        config.properties.put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(null, MockHttpsServer.clientSslContext))
        sslClient = Client.create(config)
    }
}
