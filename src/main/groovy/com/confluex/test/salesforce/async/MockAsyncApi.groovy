package com.confluex.test.salesforce.async

import com.confluex.mock.http.MockHttpsServer


class MockAsyncApi {
    MockHttpsServer mockHttpsServer

    MockAsyncApi(MockHttpsServer mockHttpsServer) {
        this.mockHttpsServer = mockHttpsServer
    }

    MockBatchResultBuilder batchResult() {
        new MockBatchResultBuilder(mockHttpsServer)
    }
}
