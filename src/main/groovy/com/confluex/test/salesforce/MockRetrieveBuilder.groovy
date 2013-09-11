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
                    slurpAndEditXmlResource('/template/retrieve-response.xml') { root ->
                        def ids = requestDoc.Body.retrieve.ids.text().split(',').toList()
                        log.debug("$ids")
                        ids.size().times {
                            def id = ids[it]
                            root.Body.retrieveResponse.appendNode {
                                result {
                                    Id(id)
                                    type(requestDoc.Body.retrieve.sObjectType)
                                    requestDoc.Body.retrieve.fieldList.text().split(',').each {
                                        "$it"([nil: 'true'])
                                    }
                                }
                            }
                        }
                    }
                }
        new MockRetrieveResponse()
    }
}
