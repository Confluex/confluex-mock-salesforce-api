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
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
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
        evalXpath(xpath, xml, XPathConstants.STRING)
    }

    def evalXpath(String xpath, String xml, QName resultType) {
        def namespaces = [
                env: 'http://schemas.xmlsoap.org/soap/envelope/',
                xsd: 'http://www.w3.org/2001/XMLSchema',
                xsi: 'http://www.w3.org/2001/XMLSchema-instance',
                sf: 'urn:partner.soap.sforce.com',
                so: 'urn:sobject.partner.soap.sforce.com',
                a: 'urn:a',
                b: 'urn:b',
                c: 'urn:c'
        ]

        def evaluator = XPathFactory.newInstance().newXPath()
        evaluator.setNamespaceContext([
                getNamespaceURI: { prefix ->
                    namespaces[prefix]
                }
        ] as NamespaceContext)
        def builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        def rootElement = builder.parse(new ByteArrayInputStream(xml.bytes)).documentElement

        evaluator.evaluate(xpath, rootElement, resultType)
    }
}
