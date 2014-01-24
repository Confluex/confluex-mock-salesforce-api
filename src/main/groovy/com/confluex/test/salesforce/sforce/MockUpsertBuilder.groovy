package com.confluex.test.salesforce.sforce

import com.confluex.mock.http.ClientRequest
import com.confluex.mock.http.MockHttpsServer
import com.confluex.test.salesforce.BaseBuilder

import javax.xml.xpath.XPathConstants

import static com.confluex.mock.http.matchers.HttpMatchers.body
import static com.confluex.mock.http.matchers.HttpMatchers.path
import static com.confluex.mock.http.matchers.HttpMatchers.stringHasXPath
import static org.hamcrest.Matchers.startsWith

class MockUpsertBuilder extends BaseBuilder {
    MockUpsertBuilder(MockHttpsServer mockHttpsServer) {
        super(mockHttpsServer)
    }

    SforceObjectRequest capture(ClientRequest httpRequest) {
        def request = new SforceObjectRequest( httpRequest.body )
        evalXpath('/Envelope/Body/upsert/sObjects/*', httpRequest.body, XPathConstants.NODESET).each {
            request.fields[it.name.split(':')[1] ?: it] = it.textContent
        }
        request
    }

    MockUpsertResponse returnSuccess() {
        def response = new MockUpsertResponse()
        mockHttpsServer.respondTo(path(startsWith('/services/Soap/u/28.0'))
                .and(body(stringHasXPath('/Envelope/Body/upsert')))
        )
            .withStatus(200)
            .withBody { ClientRequest request ->
                buildSoapEnvelope {
                    def requestDoc = new XmlSlurper().parseText(request.body)
                    def id = requestDoc.Body.upsert.sObjects.Id.text()
                    'env:Body' {
                        'sf:upsertResponse' {
                            'sf:result' {
                                'sf:id'(id)
                                'sf:success'('true')
                            }
                        }
                    }
                }
            }
        return response
    }

    MockUpsertResponse returnFailure() {
        def response = new MockUpsertResponse()

        mockHttpsServer.respondTo(path(startsWith('/services/Soap/u/28.0'))
                .and(body(stringHasXPath('/Envelope/Body/upsert')))
        )
                .withStatus(200)
                .withBody { ClientRequest request ->
                    buildSoapEnvelope {
                        def requestDoc = new XmlSlurper().parseText(request.body)
                        def id = requestDoc.Body.upsert.sObjects.Id.text()
                        'env:Body' {
                            'sf:upsertResponse' {
                                'sf:result' {
                                    response.errors.each { error ->
                                        'sf:errors' {
                                            'sf:message'(error.message)
                                            'sf:statusCode'(error.statusCode)
                                        }
                                    }
                                    'sf:id'(id)
                                    'sf:success'('false')
                                }
                            }
                        }
                    }
        }

        return response
    }
}
