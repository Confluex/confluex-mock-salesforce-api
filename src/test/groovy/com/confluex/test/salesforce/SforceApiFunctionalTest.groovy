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

        ClientResponse response = postSforce(request, ClientResponse)

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
                .post(String)
        assert 'steve@woz.org' == evalXpath('/env:Envelope/env:Body/sf:retrieveResponse/sf:result[1]/so:Email', response)
    }

    @Test
    void updateShouldRespondSuccessWithIdByDefault() {
        ClientResponse httpResponse = postSforce(updateRequest([type: 'Contact', Id: '001234', FirstName: 'NewName']), ClientResponse)

        assert 200 == httpResponse.status

        def response = httpResponse.getEntity(String)
        assert 'true' == evalXpath('/env:Envelope/env:Body/sf:updateResponse/sf:result[1]/sf:success', response)
        assert '001234' == evalXpath('/env:Envelope/env:Body/sf:updateResponse/sf:result[1]/sf:id', response)
    }

    @Test
    void updateShouldCaptureRequestsForAssertion() {
        postSforce(updateRequest([type: 'Contact', Id: '001234', FirstName: 'NewName']))

        assert 0 == server.sforceApi().getRequests('retrieve').size()
        assert 1 == server.sforceApi().getRequests('update').size()
        assert 'Contact' == server.sforceApi().getRequests('update')[0].fields['type']
        assert '001234' == server.sforceApi().getRequests('update')[0].fields['Id']
        assert 'NewName' == server.sforceApi().getRequests('update')[0].fields['FirstName']
    }

    @Test
    void selectQueryShouldReturnEmptyResults() {
        ClientResponse httpResponse = postSforce(queryRequest("SELECT FirstName, LastName, Account.Description FROM Contact WHERE Id = '1'"), ClientResponse)

        assert 200 == httpResponse.status
        def response = httpResponse.getEntity(String)
        assert 'true' == evalXpath('/env:Envelope/env:Body/sf:queryResponse/sf:result[1]/sf:done', response)
        assert 'true' == evalXpath('/env:Envelope/env:Body/sf:queryResponse/sf:result[1]/sf:queryLocator/@xsi:nil', response)
        assert '0' == evalXpath('/env:Envelope/env:Body/sf:queryResponse/sf:result[1]/sf:size', response)
    }

    @Test
    void selectQueryWithResultDataShouldReturnResults() {
        server.sforceApi().query().returnResults()
                .withRow().withField('FirstName', 'Charlie').withField('LastName', 'Chaplin').withField('Account.Description', 'United Artist')
                .withRow().withField('FirstName', 'Batman') // no more fields set

        String response = postSforce(queryRequest("SELECT FirstName, LastName, Account.Description FROM Contact WHERE Id = '1'"))

        assert '2' == evalXpath('/env:Envelope/env:Body/sf:queryResponse/sf:result[1]/sf:size', response)
        assert 'sf:sObject' == evalXpath('/env:Envelope/env:Body/sf:queryResponse/sf:result[1]/sf:records[1]/@xsi:type', response)
        assert 'Contact' == evalXpath('/env:Envelope/env:Body/sf:queryResponse/sf:result[1]/sf:records[1]/sf:type', response)
        assert 'Charlie' == evalXpath('/env:Envelope/env:Body/sf:queryResponse/sf:result[1]/sf:records[1]/sf:FirstName', response)
        assert 'Chaplin' == evalXpath('/env:Envelope/env:Body/sf:queryResponse/sf:result[1]/sf:records[1]/sf:LastName', response)
        assert 'sf:sObject' == evalXpath('/env:Envelope/env:Body/sf:queryResponse/sf:result[1]/sf:records[1]/sf:Account/@xsi:type', response)
        assert 'United Artist' == evalXpath('/env:Envelope/env:Body/sf:queryResponse/sf:result[1]/sf:records[1]/sf:Account/sf:Description', response)

        assert 'Contact' == evalXpath('/env:Envelope/env:Body/sf:queryResponse/sf:result[1]/sf:records[2]/sf:type', response)
        assert 'Batman' == evalXpath('/env:Envelope/env:Body/sf:queryResponse/sf:result[1]/sf:records[2]/sf:FirstName', response)
        assert '' == evalXpath('/env:Envelope/env:Body/sf:queryResponse/sf:result[1]/sf:records[2]/sf:LastName', response)
        assert 'sf:sObject' == evalXpath('/env:Envelope/env:Body/sf:queryResponse/sf:result[1]/sf:records[2]/sf:Account/@xsi:type', response)
        assert '' == evalXpath('/env:Envelope/env:Body/sf:queryResponse/sf:result[1]/sf:records[2]/sf:Account/sf:Description', response)
    }

    private String postSforce(String request) {
        postSforce(request, String)
    }

    private <T> T postSforce(String request, Class<T> typeToReturn) {
        sslClient.resource('https://localhost:8090/services/Soap/u/28.0/00MOCK000000org')
                .entity(request, 'text/xml; charset=UTF-8')
                .post(typeToReturn)
    }

    String updateRequest(Map<String, String> fields) {
        sforceRequest {
            'm:update'(
                    'xmlns:m':'urn:partner.soap.sforce.com',
                    'xmlns:sobj':'urn:sobject.partner.soap.sforce.com',
                    'xmlns:xsi': 'http://www.w3.org/2001/XMLSchema-instance') {
                'm:sObjects' {
                    fields.each { field ->
                        "sobj:${field.key}"( "${field.value}" )
                    }
                }
            }
        }
    }

    String queryRequest(String query) {
        sforceRequest {
            'm:query'(
                    'xmlns:m':'urn:partner.soap.sforce.com',
                    'xmlns:sobj':'urn:sobject.partner.soap.sforce.com') {
                'm:queryString' query
            }
        }
    }

    String sforceRequest(Closure bodyClosure) {
        def root = new XmlSlurper().parse(new ClassPathResource('generic-request.xml').inputStream)
        root.Body.appendNode(bodyClosure)
        new StreamingMarkupBuilder().bind { mkp.yield root }
    }

}
