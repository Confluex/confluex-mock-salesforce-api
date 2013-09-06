package com.confluex.test.salesforce

import com.confluex.mule.test.http.MockHttpsServer
import com.sun.jersey.api.client.Client
import com.sun.jersey.api.client.ClientResponse
import com.sun.jersey.api.client.config.ClientConfig
import com.sun.jersey.api.client.config.DefaultClientConfig
import com.sun.jersey.client.urlconnection.HTTPSProperties
import groovy.xml.StreamingMarkupBuilder
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.core.io.ClassPathResource

import javax.ws.rs.core.MediaType
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathFactory

class LoginFunctionalTest extends AbstractFunctionalTest {

    @Test
    void shouldAcceptAnyLoginCredentialsByDefault() {
        def root = new XmlSlurper().parse(new ClassPathResource('login-request.xml').inputStream)
        root.Body.login.username = 'arbitrary@confluex.com'
        root.Body.login.password = 'yrartibraAPIKEY'
        String request = new StreamingMarkupBuilder().bind { mkp.yield root }

        ClientResponse response = sslClient.resource('https://localhost:8090/services/Soap/u/28.0')
            .entity(request, 'text/xml; charset=UTF-8')
            .post(ClientResponse.class)

        def responseBody = response.getEntity(String)

        assert response.status == 200
        assert MockSalesforceApiServer.DEFAULT_USER_ID == evalXpath('/Envelope/Body/loginResponse/result/userId', responseBody)
        assert MockSalesforceApiServer.DEFAULT_USER_ID == evalXpath('/Envelope/Body/loginResponse/result/userInfo/userId', responseBody)
        assert MockSalesforceApiServer.DEFAULT_ORG_ID + 'MAC' == evalXpath('/Envelope/Body/loginResponse/result/userInfo/organizationId', responseBody)
        assert MockSalesforceApiServer.DEFAULT_SESSION_ID == evalXpath('/Envelope/Body/loginResponse/result/sessionId', responseBody)

        URL metadataUrl = new URL(evalXpath('/Envelope/Body/loginResponse/result/metadataServerUrl', responseBody))
        assert 'localhost' == metadataUrl.host
        assert 8090 == metadataUrl.port
        assert metadataUrl.path ==~ /${MockSalesforceApiServer.METADATA_PATH_PREFIX}.*/
        assert metadataUrl.path ==~ /.*${MockSalesforceApiServer.DEFAULT_ORG_ID}/

        URL serverUrl = new URL(evalXpath('/Envelope/Body/loginResponse/result/serverUrl', responseBody))
        assert 'localhost' == serverUrl.host
        assert 8090 == serverUrl.port
        assert serverUrl.path ==~ /${MockSalesforceApiServer.PATH_PREFIX}.*/
        assert serverUrl.path ==~ /.*${MockSalesforceApiServer.DEFAULT_ORG_ID}/
    }
}
