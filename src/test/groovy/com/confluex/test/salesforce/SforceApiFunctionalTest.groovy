package com.confluex.test.salesforce

import com.sun.jersey.api.client.ClientResponse
import groovy.util.logging.Slf4j
import groovy.xml.StreamingMarkupBuilder
import org.junit.Test
import org.springframework.core.io.ClassPathResource

import javax.xml.xpath.XPathConstants

@Slf4j
class SforceApiFunctionalTest extends AbstractFunctionalTest {

    @Test
    void retrieveShouldReturnRequestedIdsWithNullFieldsByDefault() {
        def root = new XmlSlurper().parse(new ClassPathResource('retrieve-request.xml').inputStream)
        root.Header.SessionHeader.sessionId = '00arbitrary!sessionid'
        root.Body.retrieve.fieldList = 'Id,FirstName,LastName,Email'
        root.Body.retrieve.sObjectType = 'Contact'
        root.Body.retrieve.ids = '000000000000001,000000000000002'
        String request = new StreamingMarkupBuilder().bind { mkp.yield root }

        ClientResponse response = sslClient.resource('https://localhost:8090/services/Soap/u/28.0/00MOCK000000org')
                .entity(request, 'text/xml; charset=UTF-8')
                .post(ClientResponse.class)

        assert response.status == 200

        def responseBody = response.getEntity(String)
        log.debug("retrieve response: $responseBody")
        assert 2 == evalXpath('/env:Envelope/env:Body/sf:retrieveResponse/sf:result', responseBody, XPathConstants.NODESET).length
        assert '000000000000001' == evalXpath('/env:Envelope/env:Body/sf:retrieveResponse/sf:result[1]/so:Id', responseBody)
        assert '000000000000002' == evalXpath('/env:Envelope/env:Body/sf:retrieveResponse/sf:result[2]/so:Id', responseBody)
        ['1', '2'].each { index ->
            assert 'Contact' == evalXpath("/env:Envelope/env:Body/sf:retrieveResponse/sf:result[$index]/so:type", responseBody)
            assert 'true' == evalXpath("/env:Envelope/env:Body/sf:retrieveResponse/sf:result[$index]/so:FirstName/@xsi:nil", responseBody)
            assert 'true' == evalXpath("/env:Envelope/env:Body/sf:retrieveResponse/sf:result[$index]/so:LastName/@xsi:nil", responseBody)
            assert 'true' == evalXpath("/env:Envelope/env:Body/sf:retrieveResponse/sf:result[$index]/so:Email/@xsi:nil", responseBody)
        }
    }

    @Test
    void retrieveShouldReturnSpecificFieldValues() {
        server.sforceApi().retrieve().returnObject().withField("Email", "steve@woz.org")

        def root = new XmlSlurper().parse(new ClassPathResource('retrieve-request.xml').inputStream)
        root.Header.SessionHeader.sessionId = '00arbitrary!sessionid'
        root.Body.retrieve.fieldList = 'Id,Email'
        root.Body.retrieve.sObjectType = 'Contact'
        root.Body.retrieve.ids = '000000000000001'
        String request = new StreamingMarkupBuilder().bind { mkp.yield root }

        String response = sslClient.resource('https://localhost:8090/services/Soap/u/28.0/00MOCK000000org')
                .entity(request, 'text/xml; charset=UTF-8')
                .post(String.class)
        assert 'steve@woz.org' == evalXpath('/env:Envelope/env:Body/sf:retrieveResponse/sf:result[1]/so:Email', response)
    }
}
