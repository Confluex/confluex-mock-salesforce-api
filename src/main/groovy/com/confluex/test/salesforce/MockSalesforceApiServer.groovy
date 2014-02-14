package com.confluex.test.salesforce

import com.confluex.mock.http.ClientRequest
import com.confluex.mock.http.MockHttpsServer
import com.confluex.test.salesforce.async.MockAsyncApi
import com.confluex.test.salesforce.sforce.MockSforceApi
import com.confluex.test.salesforce.streaming.MockStreamingApi
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
    private MockStreamingApi streamingApi

    MockSalesforceApiServer(int port) {
        httpsServer = new MockHttpsServer(port)
        asyncApi = new MockAsyncApi(httpsServer)
        sforceApi = new MockSforceApi(httpsServer)
        streamingApi = new MockStreamingApi(httpsServer)
        loginDefaults()
    }

    MockAsyncApi asyncApi() {
        asyncApi
    }

    MockSforceApi sforceApi() {
        sforceApi
    }

    MockStreamingApi streamingApi() {
        streamingApi
    }

    int getPort() {
        httpsServer.getPort()
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

    void oauthDefaults() {
        String json = '''
{
"id":"https://login.salesforce.com/id/00Di0000000cjCZEAY/005i0000001XnFkAAK",
"issued_at":"1391183965298",
"instance_url":"https://na15.salesforce.com",
"signature":"+a.shorter.signature=",
"access_token":"some.kind.of.long.crazy.string.that.reflects.an.oauth.accesstoken.word.to.your.momma"
}
'''

        httpsServer.respondTo(path('/services/oauth2/token')).withBody(json)
    }

    void stop() {
        httpsServer.stop()
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
}
