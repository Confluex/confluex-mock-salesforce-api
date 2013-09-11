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
        assert 2 == evalXpath('/Envelope/Body/retrieveResponse/result', responseBody, XPathConstants.NODESET).length
        assert '000000000000001' == evalXpath('/Envelope/Body/retrieveResponse/result[1]/Id', responseBody)
        assert '000000000000002' == evalXpath('/Envelope/Body/retrieveResponse/result[2]/Id', responseBody)
        ['1', '2'].each { index ->
            assert 'Contact' == evalXpath("/Envelope/Body/retrieveResponse/result[$index]/type", responseBody)
            assert 'true' == evalXpath("/Envelope/Body/retrieveResponse/result[$index]/FirstName/@nil", responseBody)
            assert 'true' == evalXpath("/Envelope/Body/retrieveResponse/result[$index]/LastName/@nil", responseBody)
            assert 'true' == evalXpath("/Envelope/Body/retrieveResponse/result[$index]/Email/@nil", responseBody)
        }
    }
}
