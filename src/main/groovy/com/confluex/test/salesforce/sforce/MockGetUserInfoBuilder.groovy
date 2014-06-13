package com.confluex.test.salesforce.sforce

import com.confluex.mock.http.ClientRequest
import com.confluex.mock.http.MockHttpsServer
import com.confluex.test.salesforce.BaseBuilder
import groovy.util.logging.Slf4j

import static com.confluex.mock.http.matchers.HttpMatchers.*
import static org.hamcrest.Matchers.*

@Slf4j
class MockGetUserInfoBuilder extends BaseBuilder {
    public MockGetUserInfoBuilder(MockHttpsServer mockHttpsServer) {
        super(mockHttpsServer)
    }

    public MockGetUserInfoResponse returnObject() {
        def response = new MockGetUserInfoResponse()
		def xml = slurpAndEditXmlResource('/template/get-user-info-response.xml') { root ->
			root.Body.getUserInfoResponse.result.userFullName = 'Thaddeus Rafacz'
			root.Body.getUserInfoResponse.result.userId = '005i0000000fWxdAAE'
			root.Body.getUserInfoResponse.result.userEmail = 'trafacz@confluex.com'
		}
		log.debug("in MockGetUserInfoResponse.returnObject, after slurpAndEditXml, xml = $xml")
        mockHttpsServer.respondTo(path(startsWith('/services/Soap/u/28.0'))
                .and(body(stringHasXPath('/Envelope/Body/getUserInfo')))
        )
            .withStatus(200)
            .withBody(xml)
        return response
    }
}
