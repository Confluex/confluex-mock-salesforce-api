package com.confluex.test.salesforce

import com.confluex.mock.http.MockHttpsServer
import com.sun.jersey.api.client.Client
import com.sun.jersey.api.client.config.ClientConfig
import com.sun.jersey.api.client.config.DefaultClientConfig
import com.sun.jersey.client.urlconnection.HTTPSProperties
import org.junit.After
import org.junit.Before

import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathFactory

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

    def evalXpath(String xpath, String xml) {
        def evaluator = XPathFactory.newInstance().newXPath()
        def builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        def rootElement = builder.parse(new ByteArrayInputStream(xml.bytes)).documentElement

        evaluator.evaluate(xpath, rootElement)
    }
}
