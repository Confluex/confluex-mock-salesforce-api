package com.confluex.test.salesforce

import com.confluex.mock.http.ClientRequest
import com.confluex.mock.http.MockHttpsServer
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.xml.StreamingMarkupBuilder
import org.springframework.core.io.ClassPathResource

import static com.confluex.mock.http.matchers.HttpMatchers.*
import static org.hamcrest.Matchers.*

class MockSalesforceApiServer {
    static final String DEFAULT_ORG_ID = '00MOCK000000org'
    static final String DEFAULT_USER_ID = '00MOCK00000user'
    static final String DEFAULT_SESSION_ID = 'MOCKSESSIONID'
    static final String METADATA_PATH_PREFIX = '/services/Soap/m/28.0/'
    static final String PATH_PREFIX = '/services/Soap/u/28.0/'

    private MockHttpsServer httpsServer
    private MockAsyncApi asyncApi
    private MockSforceApi sforceApi

    MockSalesforceApiServer(int port) {
        httpsServer = new MockHttpsServer(port)
        asyncApi = new MockAsyncApi(httpsServer)
        sforceApi = new MockSforceApi(httpsServer)
        loginDefaults()
        sforceApiDefaults()
        asyncApiDefaults()
        streamingApiDefaults()
    }

    MockAsyncApi asyncApi() {
        asyncApi
    }

    MockSforceApi sforceApi() {
        sforceApi
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
                .and(header('Content-Type', startsWith('text/xml')))
        ).withBody(xml)
    }

    void sforceApiDefaults() {
        sforceApi().retrieve().returnObject()
    }

    void asyncApiDefaults() {
        asyncApi().batchResult().respondSuccess()

        def batchPathPattern = '/services/async/26\\.0/job/(\\w+)/batch'
        httpsServer.respondTo(path(matchesPattern(batchPathPattern))).withStatus(201).withBody { request ->
            def jobId = (request.path =~ batchPathPattern)[0][1]
            slurpAndEditXml('/template/create-batch-response.xml') { root ->
                root.id = '00MOCK0000batch'
                root.jobId = jobId
                root.createdDate = formatDate(new Date())
                root.systemModstamp = formatDate(new Date())
            }
        }
        httpsServer.respondTo(path('/services/async/26.0/job/')).withStatus(201).withBody { request ->
            def requestXml = new XmlSlurper().parseText(request.body)
            slurpAndEditXml('/template/create-job-response.xml') { root ->
                root.id = '00MOCK000000job'
                root.operation = requestXml.operation.text()
                root.object = requestXml.object.text()
                root.externalIdFieldName = requestXml.externalIdFieldName.text()
                root.createdDate = formatDate(new Date())
                root.systemModstamp = formatDate(new Date())
            }
        }
    }

    void streamingApiDefaults() {
        httpsServer.respondTo(path('/cometd/26.0')).withBody { request ->
            getStreamingResponse(request)
        }
    }

    void stop() {
        httpsServer.stop()
    }

    String getStreamingResponse(ClientRequest request) {
        def message = new JsonSlurper().parseText(request.body)
        message.successful = true
        [
                '/meta/handshake': {
                        message.clientId = '111111111111111111111111111'
                },
                '/meta/connect': {
                }
        ].get(message.channel).call()
        makeJson(message)
    }

    String slurpAndEditXml(String path, Closure editClosure) {
        slurpAndEditXml(new ClassPathResource(path).inputStream, editClosure)
    }

    String slurpAndEditXml(InputStream xml, Closure editClosure) {
        def root = new XmlSlurper().parse(xml)
        editClosure(root)
        new StreamingMarkupBuilder().bind {
            mkp.declareNamespace("": root.namespaceURI())
            mkp.yield root
        }
    }

    String makeJson(map) {
        new JsonBuilder(map).toString()
    }

    String formatDate(Date date) {
        date.format("yyyy-MM-dd'T'HH:mm:ss'Z'")
    }
}
