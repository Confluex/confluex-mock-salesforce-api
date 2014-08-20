package com.confluex.test.salesforce

import com.sun.jersey.api.client.ClientResponse
import groovy.json.JsonSlurper
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

        String response = sslClient.resource('https://localhost:8081/services/Soap/u/28.0/00MOCK000000org')
                .entity(request, 'text/xml; charset=UTF-8')
                .post(String)
        assert 'steve@woz.org' == evalXpath('/env:Envelope/env:Body/sf:retrieveResponse/sf:result[1]/so:Email', response)
    }

    @Test
    void retrieveShouldReturnNestedSObjectField() {
        server.sforceApi().retrieve().returnObject().withField("RecordType.DeveloperName", "SuperHero")

        def root = new XmlSlurper().parse(new ClassPathResource('retrieve-request.xml').inputStream)
        root.Header.SessionHeader.sessionId = '00arbitrary!sessionid'
        root.Body.retrieve.fieldList = 'Id,RecordType.DeveloperName'
        root.Body.retrieve.sObjectType = 'Contact'
        root.Body.retrieve.ids = '000000000000001'
        String request = new StreamingMarkupBuilder().bind { mkp.yield root }

        String response = sslClient.resource('https://localhost:8081/services/Soap/u/28.0/00MOCK000000org')
                .entity(request, 'text/xml; charset=UTF-8')
                .post(String)
        assert 'SuperHero' == evalXpath('/env:Envelope/env:Body/sf:retrieveResponse/sf:result[1]/so:RecordType/so:DeveloperName', response)
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
        assert 'so:sObject' == evalXpath('/env:Envelope/env:Body/sf:queryResponse/sf:result[1]/sf:records[1]/@xsi:type', response)
        assert 'Contact' == evalXpath('/env:Envelope/env:Body/sf:queryResponse/sf:result[1]/sf:records[1]/so:type', response)
        assert 'Charlie' == evalXpath('/env:Envelope/env:Body/sf:queryResponse/sf:result[1]/sf:records[1]/so:FirstName', response)
        assert 'Chaplin' == evalXpath('/env:Envelope/env:Body/sf:queryResponse/sf:result[1]/sf:records[1]/so:LastName', response)
        assert 'so:sObject' == evalXpath('/env:Envelope/env:Body/sf:queryResponse/sf:result[1]/sf:records[1]/so:Account/@xsi:type', response)
        assert 'United Artist' == evalXpath('/env:Envelope/env:Body/sf:queryResponse/sf:result[1]/sf:records[1]/so:Account/so:Description', response)

        assert 'Contact' == evalXpath('/env:Envelope/env:Body/sf:queryResponse/sf:result[1]/sf:records[2]/so:type', response)
        assert 'Batman' == evalXpath('/env:Envelope/env:Body/sf:queryResponse/sf:result[1]/sf:records[2]/so:FirstName', response)
        assert '' == evalXpath('/env:Envelope/env:Body/sf:queryResponse/sf:result[1]/sf:records[2]/so:LastName', response)
        assert 'so:sObject' == evalXpath('/env:Envelope/env:Body/sf:queryResponse/sf:result[1]/sf:records[2]/so:Account/@xsi:type', response)
        assert '' == evalXpath('/env:Envelope/env:Body/sf:queryResponse/sf:result[1]/sf:records[2]/so:Account/so:Description', response)
    }

    @Test
    void upsertShouldRespondSuccessWithIdByDefault() {
        ClientResponse httpResponse = postSforce(upsertRequest([type: 'Contact', Id: '001234', FirstName: 'NewName']), ClientResponse)

        assert 200 == httpResponse.status

        def response = httpResponse.getEntity(String)
        assert 'true' == evalXpath('/env:Envelope/env:Body/sf:upsertResponse/sf:result[1]/sf:success', response)
        assert '001234' == evalXpath('/env:Envelope/env:Body/sf:upsertResponse/sf:result[1]/sf:id', response)
    }

    @Test
    void upsertShouldCaptureRequestsForAssertion() {
        postSforce(upsertRequest([type: 'Contact', Id: '001234', FirstName: 'NewName']))

        assert 0 == server.sforceApi().getRequests('retrieve').size()
        assert 0 == server.sforceApi().getRequests('update').size()
        assert 1 == server.sforceApi().getRequests('upsert').size()
        assert 'Contact' == server.sforceApi().getRequests('upsert')[0].fields['type']
        assert '001234' == server.sforceApi().getRequests('upsert')[0].fields['Id']
        assert 'NewName' == server.sforceApi().getRequests('upsert')[0].fields['FirstName']
    }

    @Test
    void upsertWithExpectedError_shouldReturnErrorsAndNotBeSuccessful() {
        server.sforceApi().upsert().returnFailure().withError("TESTING_STATUS_CODE", "testing error message")

        String response = postSforce(upsertRequest([type: 'Contact', Id: '001234', FirstName: 'NewName']))

        assert 'false' == evalXpath('/env:Envelope/env:Body/sf:upsertResponse/sf:result[1]/sf:success', response)
        assert 'TESTING_STATUS_CODE' == evalXpath('/env:Envelope/env:Body/sf:upsertResponse/sf:result[1]/sf:errors/sf:statusCode', response)
        assert 'testing error message' == evalXpath('/env:Envelope/env:Body/sf:upsertResponse/sf:result[1]/sf:errors/sf:message', response)
    }

    @Test
    void upsertWithSomeExpectedErrors_shouldReturnTheCorrectResultsInOrder() {
        server.sforceApi().upsert()
            .returnSuccess().forObject(0)
            .returnFailure().withError("STATUS_CODE", "error message").forObject(1)
            .returnFailure().withError("STATUS CODE 1", "error message 1").withError("STATUS CODE 2", "error message 2").forObject(2)
            // no mention of the 4th record, because I want to see that the default is success

        String response = postSforce(upsertRequest([
                    [type: 'Contact', Id: '001234', FirstName: 'NewName1'],
                    [type: 'Contact', Id: '002345', FirstName: 'NewName2'],
                    [type: 'Contact', Id: '003456', FirstName: 'NewName3'],
                    [type: 'Contact', Id: '004567', FirstName: 'NewName4']
                ]))

        assert 'true' == evalXpath('/env:Envelope/env:Body/sf:upsertResponse/sf:result[1]/sf:success', response)
        assert 'false' == evalXpath('/env:Envelope/env:Body/sf:upsertResponse/sf:result[2]/sf:success', response)
        assert 'false' == evalXpath('/env:Envelope/env:Body/sf:upsertResponse/sf:result[3]/sf:success', response)
        assert 'true' == evalXpath('/env:Envelope/env:Body/sf:upsertResponse/sf:result[4]/sf:success', response)

        assert '0' == evalXpath('count(/env:Envelope/env:Body/sf:upsertResponse/sf:result[1]/sf:errors)', response)
        assert '1' == evalXpath('count(/env:Envelope/env:Body/sf:upsertResponse/sf:result[2]/sf:errors)', response)
        assert '2' == evalXpath('count(/env:Envelope/env:Body/sf:upsertResponse/sf:result[3]/sf:errors)', response)
        assert '0' == evalXpath('count(/env:Envelope/env:Body/sf:upsertResponse/sf:result[4]/sf:errors)', response)
    }

    @Test
    void upsertMultipleObjects_shouldReturnSameNumberOfResults_whenMoreExpectationsExist() {
        server.sforceApi().upsert()
                .returnSuccess().forObject(0)
                .returnFailure().withError("STATUS_CODE", "error message").forObject(1)
                .returnFailure().withError("STATUS CODE 1", "error message 1").withError("STATUS CODE 2", "error message 2").forObject(2)

        String response = postSforce(upsertRequest([
                [type: 'Contact', Id: '001234', FirstName: 'NewName1'],
                [type: 'Contact', Id: '002345', FirstName: 'NewName2']
        ]))

        assert '2' == evalXpath('count(/env:Envelope/env:Body/sf:upsertResponse/sf:result)', response)

        assert 'true' == evalXpath('/env:Envelope/env:Body/sf:upsertResponse/sf:result[1]/sf:success', response)
        assert 'false' == evalXpath('/env:Envelope/env:Body/sf:upsertResponse/sf:result[2]/sf:success', response)

        assert '0' == evalXpath('count(/env:Envelope/env:Body/sf:upsertResponse/sf:result[1]/sf:errors)', response)
        assert '1' == evalXpath('count(/env:Envelope/env:Body/sf:upsertResponse/sf:result[2]/sf:errors)', response)
    }

    @Test
    void upsertMultipleObjects_shouldReturnSuccessForIntermediateObjectsByDefault() {
        server.sforceApi().upsert()
                .returnFailure().withError("STATUS_CODE", "error message").forObject(4)

        String response = postSforce(upsertRequest([
                [type: 'Contact', Id: '001234', FirstName: 'NewName1'],
                [type: 'Contact', Id: '002345', FirstName: 'NewName2'],
                [type: 'Contact', Id: '003456', FirstName: 'NewName3'],
                [type: 'Contact', Id: '004567', FirstName: 'NewName4'],
                [type: 'Contact', Id: '005678', FirstName: 'NewName5'],
                [type: 'Contact', Id: '006789', FirstName: 'NewName6']
        ]))

        assert '6' == evalXpath('count(/env:Envelope/env:Body/sf:upsertResponse/sf:result)', response)

        assert 'true' == evalXpath('/env:Envelope/env:Body/sf:upsertResponse/sf:result[1]/sf:success', response)
        assert 'true' == evalXpath('/env:Envelope/env:Body/sf:upsertResponse/sf:result[2]/sf:success', response)
        assert 'true' == evalXpath('/env:Envelope/env:Body/sf:upsertResponse/sf:result[3]/sf:success', response)
        assert 'true' == evalXpath('/env:Envelope/env:Body/sf:upsertResponse/sf:result[4]/sf:success', response)
        assert 'false' == evalXpath('/env:Envelope/env:Body/sf:upsertResponse/sf:result[5]/sf:success', response)
        assert 'true' == evalXpath('/env:Envelope/env:Body/sf:upsertResponse/sf:result[6]/sf:success', response)

    }

	@Test
	void getUserInfoShouldRespondSuccess() {
		ClientResponse httpResponse = postSforce(getUserInfoRequest(), ClientResponse)
		
		def response = httpResponse.getEntity(String)
		log.debug("in getUserInfoShouldRespondSuccess, httpResponse = $response")
		
		assert 200 == httpResponse.status

		assert 'Thaddeus Rafacz' == evalXpath('/env:Envelope/env:Body/sf:getUserInfoResponse/sf:result/sf:userFullName', response)
	}
	
	@Test
	void authorizeShouldRespondSuccess() {
		ClientResponse httpResponse = authorizeSforce(ClientResponse)
		
		def response = httpResponse.getEntity(String)
		log.debug("in authorizeShouldRespondSuccess, httpResponse = $response")
		
		assert 200 == httpResponse.status
		
		ArrayList props = new JsonSlurper().parseText(response)
		String propsDump = props.dump()
		log.debug("in authorizeShouldRespondSuccess, props = $propsDump")
		
		assert 'Bearer' == props[0].token_type
	}
	
    private String postSforce(String request) {
        postSforce(request, String)
    }

    private <T> T postSforce(String request, Class<T> typeToReturn) {
        sslClient.resource('https://localhost:8081/services/Soap/u/28.0/00MOCK000000org')
                .entity(request, 'text/xml; charset=UTF-8')
                .post(typeToReturn)
    }
	
	private <T> T authorizeSforce(Class<T> typeToReturn) {
		// sslClient.resource().getUriBuilder().queryParam(request, null)
		sslClient.resource('https://localhost:8081/services/oauth2/authorize?response_type=code&client_id=3MVG9A2kN3Bn17huRvrgRwErKxdF5TY.t6TrcT_eqTBo3LXr5cpdpOGg6CxBEQVDVzJf0sWgm1srocDaHMH8W&immediate=false&state=%3C%3CMULE_EVENT_ID%3D0-ed84f231-e81f-11e3-bf8b-d4bed9318703%3E%3E&display=page&redirect_uri=http%3A%2F%2Flocalhost%3A8081%2Foauthcallback')
                .get(typeToReturn)
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

    String upsertRequest(Map<String, String> fields) {
        upsertRequest( [fields] )
    }

    String upsertRequest(List<Map<String, String>> records) {
        sforceRequest {
            'm:upsert'(
                    'xmlns:m':'urn:partner.soap.sforce.com',
                    'xmlns:sobj':'urn:sobject.partner.soap.sforce.com',
                    'xmlns:xsi': 'http://www.w3.org/2001/XMLSchema-instance') {
                records.each { fields ->
                    'm:sObjects' {
                        fields.each { field ->
                            "sobj:${field.key}"( "${field.value}" )
                        }
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
	
	
	String getUserInfoRequest() {
		sforceRequest {
			'm:getUserInfo'(
					'xmlns:m':'urn:partner.soap.sforce.com',
					'xmlns:sobj':'urn:sobject.partner.soap.sforce.com') {
                'm:queryString' 'query text'
            }
		}
	}

}
