package com.confluex.test.salesforce

import com.confluex.mule.test.http.MockHttpsServer

class MockAsyncApi {
    MockHttpsServer mockHttpsServer

    MockAsyncApi(MockHttpsServer mockHttpsServer) {
        this.mockHttpsServer = mockHttpsServer
    }

    MockBatchResultBuilder batchResult() {
        def batchResult = new MockBatchResultBuilder()
        // match batch result request
        batchResult
    }
}
