package com.confluex.test.salesforce.sforce

import com.confluex.mock.http.MockHttpsServer
import com.confluex.mock.http.matchers.HttpMatchers

import javax.xml.namespace.QName
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathFactory

class MockSforceApi {
    MockHttpsServer mockHttpsServer

    public MockSforceApi(MockHttpsServer mockHttpsServer) {
        this.mockHttpsServer = mockHttpsServer
        defaults()
    }

    void defaults() {
        retrieve().returnObject()
        update().returnSuccess()
    }

    List<SforceRequest> getRequests(String call) {
        mockHttpsServer.requests.findAll {
            it.path =~ /\/services\/Soap\/u\/28.0/ && xpath("/Envelope/Body/$call", it.body)
        }.collect {
            switch(call) {
                case 'update':
                    update().capture(it)
                    break
                default:
                    new SforceRequest(it.body)
            }
        }
    }

    MockRetrieveBuilder retrieve() {
        new MockRetrieveBuilder(mockHttpsServer)
    }

    MockUpdateBuilder update() {
        new MockUpdateBuilder(mockHttpsServer)
    }

    private xpath(String xpath, String xml) {
        HttpMatchers.stringHasXPath(xpath).matches(xml)
    }
}
