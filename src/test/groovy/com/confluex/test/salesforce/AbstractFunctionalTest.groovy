package com.confluex.test.salesforce

import com.confluex.mock.http.MockHttpsServer
import com.sun.jersey.api.client.Client
import com.sun.jersey.api.client.config.ClientConfig
import com.sun.jersey.api.client.config.DefaultClientConfig
import com.sun.jersey.client.urlconnection.HTTPSProperties
import org.junit.After
import org.junit.Before

import javax.xml.namespace.NamespaceContext
import javax.xml.namespace.QName
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

class AbstractFunctionalTest {
    protected MockSalesforceApiServer server
    protected Client sslClient
    protected XPath evaluator
    protected DocumentBuilder builder

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

    @Before
    void initXml() {
        evaluator = XPathFactory.newInstance().newXPath()
        evaluator.setNamespaceContext([
                getNamespaceURI: { prefix ->
                    BaseBuilder.NAMESPACES[prefix]
                }
        ] as NamespaceContext)

        def factory = DocumentBuilderFactory.newInstance()
        factory.namespaceAware = true
        builder = factory.newDocumentBuilder()
        assert builder.namespaceAware
    }

    def evalXpath(String xpath, String xml) {
        evalXpath(xpath, xml, XPathConstants.STRING)
    }

    def evalXpath(String xpath, String xml, QName resultType) {
        def rootElement = builder.parse(new ByteArrayInputStream(xml.bytes)).documentElement
        evaluator.evaluate(xpath, rootElement, resultType)
    }
}
