package com.confluex.test.salesforce

import com.confluex.mock.http.MockHttpsServer

class MockSforceApi {
    MockHttpsServer mockHttpsServer

    public MockSforceApi(MockHttpsServer mockHttpsServer) {
        this.mockHttpsServer = mockHttpsServer
    }

    MockRetrieveBuilder retrieve() {
        new MockRetrieveBuilder(mockHttpsServer)
    }
}
