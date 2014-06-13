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
//        oauthDefaults()
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
        def json = slurpJson('/template/oauth-response.json')
//		[{
//			"id":"https://login.salesforce.com/id/00Di0000000aCvZEAU/005i0000000fWxdAAE",
//			"issued_at":"1401477817387",
//			"scope":"id api openid refresh_token",
//			"instance_url":"https://na15.salesforce.com",
//			"token_type":"Bearer",
//			"refresh_token":"5Aep861z80Xevi74eXTOjr_tYCuqECm6YZsClxsMbUHwXzpcEWM5H.F4RBIWb1tZJ4_4kPOKPzKXF9cRN7SYtWq",
//			"id_token":"eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IjE4OCJ9.eyJleHAiOjE0MDE0Nzc5MzcsInN1YiI6Imh0dHBzOi8vbG9naW4uc2FsZXNmb3JjZS5jb20vaWQvMDBEaTAwMDAwMDBhQ3ZaRUFVLzAwNWkwMDAwMDAwZld4ZEFBRSIsImF1ZCI6IjNNVkc5QTJrTjNCbjE3aHVSdnJnUndFckt4ZEY1VFkudDZUcmNUX2VxVEJvM0xYcjVjcGRwT0dnNkN4QkVRVkRWekpmMHNXZ20xc3JvY0RhSE1IOFciLCJpc3MiOiJodHRwczovL2xvZ2luLnNhbGVzZm9yY2UuY29tIiwiaWF0IjoxNDAxNDc3ODE3LCJjX2hhc2giOiJud1NxbkdNbTBYUkJqRDN3ejNHY1dBIn0.RY5Tmb3OCc91mOZNc4Jp1zeamUH_tYNI-0zkvXJh7tzZvRgAmt9DyuHry3lDc5HEpKsmBfwaGEnQhkH_8NOmZLQXCMp1Om5BnYau6XYMki9HpIwptRTy80xAXTHDln-q_WzFQ9dD6X9g0pZGO4HBsWveZqcf0eTxnj2Vi0xT8-815RBGDQ6VOMGaYKvZx--hKpKGTTZuipl29w7Tl7TOTE3uBnJJ9L-evFxrW579RIXXnZ5sgwLhsqP1RZWbFwQgzTrChpmUgrkYHEw742ZJvPIbeRsDtXx9JThhykojtj3FwP_jvsqYpM455H4BfLY0A7O0NgfsZp0A1AzuSp7swFTYdBFfBtjtivEnKQXzyMhg9CTocpLa00xqo6_UkmBGvMSObXyomwhflfhqY3hNBYUrvDnk_gAWWzcuQy3JCcV0Fy1acTe8TX1y61YFJW7lcG00bRkIpd7gYYDv68OmVvhULoeg9xktzhy9zRU75dpWNHbYT7wM_-8jxY_Saa1sWd43jPhlGWHmWHbAi87mLYNRK_mYgnp5Q64knGynDJo7-mXDOXTHMczNbbWJpuiZK5DrgQawSTtsFIufdwg5Z1cZ3psQzTcBTdNAtgF4GFY-I3-zW_6Pm-f9NEftFmSTKoFKS6jFX_QQxidzD_8xOrNrLT9JTzXXnV06dLYOb_o",
//			"signature":"7X2AenzXtH5LAoYROMOGaJOiSilcKbQEmGZ9LGrjT9s=",
//			"access_token":"00Di0000000aCvZ!AQUAQIdSp9OT0K5.Z3FVA2vT1ur639FtoVzl.lbyJbpIWrJ4PiW7I1Rj.hAqgE_1M4FzhD3WMNnGO0JMyUg3SJ6L3tkylZ.Y"
//		}]
		httpsServer.respondTo(path(startsWith('/services/oauth2/authorize')))
			.withStatus(200)
			.withBody(json)
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
	
	String slurpJson(String path) {
		String json = new ClassPathResource(path).inputStream.getText()
		// def root = new JsonSlurper().parseText(json)
		return json
	}
	
}
