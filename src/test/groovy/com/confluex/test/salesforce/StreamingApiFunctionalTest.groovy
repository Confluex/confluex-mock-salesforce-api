package com.confluex.test.salesforce

import com.sun.jersey.api.client.ClientResponse
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.junit.Test

import javax.ws.rs.core.MediaType

class StreamingApiFunctionalTest extends AbstractFunctionalTest {
    private def handshakeRequest = [
            id: "1",
            supportedConnectionTypes: ['long-polling'],
            channel: '/meta/handshake',
            version: '1.0'
    ]
    private def connectRequest = [
            id: "2",
            connectionType: 'long-polling',
            advice: [ timeout: 0 ],
            channel: '/meta/connect',
            clientId: 'x'
    ]

    def requestBayeux(request) {
        ClientResponse clientResponse =
            sslClient.resource("https://localhost:8090/cometd/26.0")
                    .entity(new JsonBuilder(request).toString(), MediaType.APPLICATION_JSON_TYPE)
                    .post(ClientResponse)

        new JsonSlurper().parseText(clientResponse.getEntity(String))
    }

    @Test
    void shouldReplySuccessful_toHandshake() {
        assert requestBayeux(handshakeRequest).successful
    }

    @Test
    void shouldReplyWithSameId_toHandshake() {
        assert handshakeRequest.id == requestBayeux(handshakeRequest).id
    }

    @Test
    void shouldReplyWithSameSupportedConnectionTypes_toHandshake() {
        assert [ 'long-polling' ] == requestBayeux(handshakeRequest).supportedConnectionTypes
    }

    @Test
    void shouldReplyWithSameChannel_toHandshake() {
        assert '/meta/handshake' == requestBayeux(handshakeRequest).channel
    }

    @Test
    void shouldReplyWithSameVersion_toHandshake() {
        assert '1.0' == requestBayeux(handshakeRequest).version
    }

    @Test
    void shouldReplyWithClientId_toHandshake() {
        assert null != requestBayeux(handshakeRequest).clientId
    }

    @Test
    void shouldReplySuccessful_toConnect() {
        assert requestBayeux(connectRequest).successful
    }

    @Test
    void shouldReplyWithSameId_toConnect() {
        assert connectRequest.id == requestBayeux(connectRequest).id
    }

    @Test
    void shouldReplyWithSameChannel_toConnect() {
        assert '/meta/connect' == requestBayeux(connectRequest).channel
    }

    @Test
    void shouldReplyWithSameClientId_toConnect() {
        assert connectRequest.clientId == requestBayeux(connectRequest).clientId

    }
}
