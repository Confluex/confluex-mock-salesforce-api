package com.confluex.test.salesforce

import com.confluex.mock.http.ClientRequest
import com.confluex.mock.http.MockHttpsServer
import groovy.util.logging.Slf4j

import static com.confluex.mock.http.matchers.HttpMatchers.*
import static org.hamcrest.Matchers.*

@Slf4j
class MockRetrieveBuilder extends BaseBuilder {

    public MockRetrieveBuilder(MockHttpsServer mockHttpsServer) {
        super(mockHttpsServer)
    }

    public MockRetrieveResponse returnObject() {
        mockHttpsServer.respondTo(path(startsWith('/services/Soap/u/28.0'))
                .and(body(stringHasXPath('/Envelope/Body/retrieve')))
        )
                .withStatus(200)
                .withBody() { ClientRequest request ->
                    def requestDoc = new XmlSlurper().parseText(request.body)
                    def ids = requestDoc.Body.retrieve.ids.text().split(',').toList()
                    def fields = requestDoc.Body.retrieve.fieldList.text().split(',')
                    buildXml {
                        'env:Envelope'(NAMESPACES.collectEntries {key, value -> ["xmlns:$key", value]}) {
                            'env:Body' {
                                'sf:retrieveResponse' {
                                    ids.each { id ->
                                        'sf:result' {
                                            'so:type'(requestDoc.Body.retrieve.sObjectType.text())
                                            'so:Id'(id)
                                            fields.each { field ->
                                                "so:$field"(['xsi:nil':'true'])
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
        new MockRetrieveResponse()
    }
}
