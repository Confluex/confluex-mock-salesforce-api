package com.confluex.test.salesforce

import com.sun.jersey.api.client.ClientResponse
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.junit.Before
import org.junit.Test

import javax.ws.rs.core.MediaType

class StreamingApiFunctionalTest extends AbstractFunctionalTest {
    private def handshakeRequest

    @Before
    void initRequests() {
        handshakeRequest = [
                id: "3",
                supportedConnectionTypes: ['long-polling'],
                channel: '/meta/handshake',
                version: '1.0'
        ]
    }

    def handshake() {
        def request = new JsonBuilder(handshakeRequest).toString()

        ClientResponse clientResponse =
            sslClient.resource("https://localhost:8090/cometd/26.0")
                    .entity(request, MediaType.APPLICATION_JSON_TYPE)
                    .post(ClientResponse)

        def response = new JsonSlurper().parseText(clientResponse.getEntity(String))
        response
    }

    @Test
    void shouldReplySuccessful_toHandshake() {
        assert handshake().successful
    }

    @Test
    void shouldReplyWithSameId_toHandshake() {
        assert handshakeRequest.id == handshake().id
    }

    @Test
    void shouldReplyWithSameSupportedConnectionTypes_toHandshake() {
        assert [ 'long-polling' ] == handshake().supportedConnectionTypes
    }

    @Test
    void shouldReplyWithSameChannel_toHandshake() {
        assert '/meta/handshake' == handshake().channel
    }

    @Test
    void shouldReplyWithSameVersion_toHandshake() {
        assert '1.0' == handshake().version
    }

    @Test
    void shouldReplyWithClientId_toHandshake() {
        assert null != handshake().clientId
    }
}
