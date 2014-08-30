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
    MockUpsertResponse myResponse

    MockUpsertBuilder(MockHttpsServer mockHttpsServer) {
        super(mockHttpsServer)
        myResponse = new MockUpsertResponse(this)
        matchUpsertRequests()
    }

    void matchUpsertRequests() {
        mockHttpsServer.respondTo(path(startsWith('/services/Soap/u/28.0'))
                .and(body(stringHasXPath('/Envelope/Body/upsert')))
        )
                .withStatus(200)
                .withBody { ClientRequest request ->
                    buildSoapEnvelope {
                        def requestDoc = new XmlSlurper().parseText(request.body)
                        def ids = requestDoc.Body.upsert.sObjects.collect {
                            it.Id.text()
                        }

                        'env:Body' {
                            'sf:upsertResponse' {
                                ids.eachWithIndex { id, index ->
                                    'sf:result' {
                                        'sf:created'(myResponse.getResult(index).success)
                                        myResponse.getResult(index).errors.each { error ->
                                            'sf:errors' {
                                                'sf:message'(error.message)
                                                'sf:statusCode'(error.statusCode)
                                            }
                                        }
                                        if (myResponse.getResult(index).success)
                                            'sf:id'(id)
                                        else
                                            'sf:id'('xsi:nil': true)
                                        'sf:success'(myResponse.getResult(index).success)
                                    }
                                }
                            }
                        }
                    }
        }
    }

    SforceObjectRequest capture(ClientRequest httpRequest) {
        def request = new SforceObjectRequest( httpRequest.body )
        evalXpath('/Envelope/Body/upsert/sObjects/*', httpRequest.body, XPathConstants.NODESET).each {
            request.fields[it.name.split(':')[1] ?: it] = it.textContent
        }
        request
    }

    MockUpsertResponse returnSuccess() {
        myResponse.resultSuccess = 'true'
        return myResponse
    }

    MockUpsertResponse returnFailure() {
        myResponse.resultSuccess = 'false'
        return myResponse
    }
}
