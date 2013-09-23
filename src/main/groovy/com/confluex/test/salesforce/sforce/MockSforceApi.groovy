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
        update().returnSuccess()
    }

    MockRetrieveBuilder retrieve() {
        new MockRetrieveBuilder(mockHttpsServer)
    }

    MockUpdateBuilder update() {
        new MockUpdateBuilder(mockHttpsServer)
    }
}
