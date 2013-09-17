package com.confluex.test.salesforce.sforce

import com.confluex.mock.http.MockHttpsServer

class MockSforceApi {
    MockHttpsServer mockHttpsServer

    public MockSforceApi(MockHttpsServer mockHttpsServer) {
        this.mockHttpsServer = mockHttpsServer
        defaults()
    }

    void defaults() {
        retrieve().returnObject()
    }

    MockRetrieveBuilder retrieve() {
        new MockRetrieveBuilder(mockHttpsServer)
    }
}
