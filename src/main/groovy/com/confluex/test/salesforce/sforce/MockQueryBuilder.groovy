package com.confluex.test.salesforce.sforce

import com.confluex.mock.http.ClientRequest
import com.confluex.mock.http.MockHttpsServer
import com.confluex.test.salesforce.BaseBuilder

import static com.confluex.mock.http.matchers.HttpMatchers.body
import static com.confluex.mock.http.matchers.HttpMatchers.path
import static com.confluex.mock.http.matchers.HttpMatchers.stringHasXPath
import static org.hamcrest.Matchers.startsWith

class MockQueryBuilder extends BaseBuilder {
    MockQueryBuilder(MockHttpsServer mockHttpsServer) {
        super(mockHttpsServer)
    }

    MockQueryResponse returnResults() {
        def response = new MockQueryResponse()
        mockHttpsServer.respondTo(path(startsWith('/services/Soap/u/28.0'))
                .and(body(stringHasXPath('/Envelope/Body/query')))
        )
                .withStatus(200)
                .withBody() { ClientRequest request ->
            def requestDoc = new XmlSlurper().parseText(request.body)
            buildSoapEnvelope {
                'env:Body' {
                    'sf:queryResponse' {
                        'sf:result'('xsi:type': 'QueryResult') {
                            'sf:done' 'true'
                            'sf:queryLocator'('xsi:nil': 'true')
                            'sf:size' '0'
                        }
                    }
                }
            }
        }

        response
    }
}
