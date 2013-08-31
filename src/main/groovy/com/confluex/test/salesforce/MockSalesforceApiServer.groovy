package com.confluex.test.salesforce

import com.confluex.mule.test.http.MockHttpsServer
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.xml.StreamingMarkupBuilder
import org.springframework.core.io.ClassPathResource

import static com.confluex.mule.test.http.matchers.HttpMatchers.*

class MockSalesforceApiServer {
    static final String DEFAULT_ORG_ID = '00MOCK000000org'
    static final String DEFAULT_USER_ID = '00MOCK00000user'
    static final String DEFAULT_SESSION_ID = 'MOCKSESSIONID'
    static final String METADATA_PATH_PREFIX = '/services/Soap/m/28.0/'
    static final String PATH_PREFIX = '/services/Soap/u/28.0/'

    MockHttpsServer httpsServer

    MockSalesforceApiServer(int port) {
        httpsServer = new MockHttpsServer(port)
        loginDefaults()
        streamingApiDefaults()
    }

    void loginDefaults() {
        def xml = slurpAndEditXml('/template/login-response.xml') { root ->
            root.Body.loginResponse.result.metadataServerUrl = "https://localhost:${httpsServer.port}${METADATA_PATH_PREFIX}${DEFAULT_ORG_ID}"
            root.Body.loginResponse.result.serverUrl = "https://localhost:${httpsServer.port}${PATH_PREFIX}${DEFAULT_ORG_ID}"
            root.Body.loginResponse.result.userId = DEFAULT_USER_ID
            root.Body.loginResponse.result.userInfo.userId = DEFAULT_USER_ID
            root.Body.loginResponse.result.userInfo.organizationId = DEFAULT_ORG_ID + 'MAC'
            root.Body.loginResponse.result.sessionId = DEFAULT_SESSION_ID
        }
        httpsServer.respondTo(path('/services/Soap/u/28.0')
                .and(header('SOAPAction', 'login'))
                .and(header('Content-Type', 'text/xml'))
        ).withBody(xml)
    }

    void streamingApiDefaults() {
        httpsServer.respondTo(path('/cometd/26.0')).withBody { request ->
            def message = new JsonSlurper().parseText(request.body)
            message.successful = true
            message.clientId = '111111111111111111111111111'
            makeJson(message)
        }
    }

    void stop() {
        httpsServer.stop()
    }

    String slurpAndEditXml(String path, Closure editClosure) {
        def root = new XmlSlurper().parse(new ClassPathResource(path).inputStream)
        editClosure(root)
        new StreamingMarkupBuilder().bind { mkp.yield root }
    }

    String makeJson(map) {
        new JsonBuilder(map).toString()
    }
}
