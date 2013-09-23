package com.confluex.test.salesforce.sforce

import com.confluex.mock.http.ClientRequest
import com.confluex.mock.http.MockHttpsServer
import com.confluex.test.salesforce.BaseBuilder

import javax.xml.xpath.XPathConstants

import static com.confluex.mock.http.matchers.HttpMatchers.body
import static com.confluex.mock.http.matchers.HttpMatchers.path
import static com.confluex.mock.http.matchers.HttpMatchers.stringHasXPath
import static org.hamcrest.Matchers.startsWith

class MockUpdateBuilder extends BaseBuilder {
    MockUpdateBuilder(MockHttpsServer mockHttpsServer) {
        super(mockHttpsServer)
    }

    SforceUpdateRequest capture(ClientRequest httpRequest) {
        def request = new SforceUpdateRequest( httpRequest.body )
        evalXpath('/Envelope/Body/update/sObjects/*', httpRequest.body, XPathConstants.NODESET).each {
            request.fields[it.name.split(':')[1] ?: it] = it.textContent
        }
        request
    }

    MockUpdateResponse returnSuccess() {
        def response = new MockUpdateResponse()

        mockHttpsServer.respondTo(path(startsWith('/services/Soap/u/28.0'))
                .and(body(stringHasXPath('/Envelope/Body/update')))
        )
                .withStatus(200)
                .withBody() { ClientRequest request ->
                    def requestDoc = new XmlSlurper().parseText(request.body)
                    def id = requestDoc.Body.update.sObjects.Id.text()
                    buildSoapEnvelope {
                        'env:Body' {
                            'sf:updateResponse' {
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
}
