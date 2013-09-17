package com.confluex.test.salesforce.sforce

import com.confluex.mock.http.ClientRequest
import com.confluex.mock.http.MockHttpsServer
import com.confluex.test.salesforce.BaseBuilder
import groovy.util.logging.Slf4j

import static com.confluex.mock.http.matchers.HttpMatchers.*
import static org.hamcrest.Matchers.*

@Slf4j
class MockRetrieveBuilder extends BaseBuilder {

    public MockRetrieveBuilder(MockHttpsServer mockHttpsServer) {
        super(mockHttpsServer)
    }

    public MockRetrieveResponse returnObject() {
        def response = new MockRetrieveResponse()
        mockHttpsServer.respondTo(path(startsWith('/services/Soap/u/28.0'))
                .and(body(stringHasXPath('/Envelope/Body/retrieve')))
        )
            .withStatus(200)
            .withBody() { ClientRequest request ->
                def requestDoc = new XmlSlurper().parseText(request.body)
                def ids = requestDoc.Body.retrieve.ids.text().split(',').toList()
                def fields = requestDoc.Body.retrieve.fieldList.text().split(',').toList().minus('Id')
                buildSoapEnvelope {
                    'env:Body' {
                        'sf:retrieveResponse' {
                            ids.each { id ->
                                'sf:result' {
                                    'so:type'(requestDoc.Body.retrieve.sObjectType.text())
                                    'so:Id'(id)
                                    fields.each { field ->
                                        mkp.yield response.buildFieldElement(field)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        return response
    }
}
