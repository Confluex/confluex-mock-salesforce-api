package com.confluex.test.salesforce.sforce

class MockUpsertResponse {
    final MockUpsertBuilder builder
    def currentResult = defaultResult()
    def results = [currentResult]

    MockUpsertResponse(MockUpsertBuilder builder) {
        this.builder = builder
    }

    MockUpsertResponse withError(String statusCode, String message) {
        currentResult.errors << [statusCode: statusCode, message: message]
        this
    }

    MockUpsertBuilder forObject(int index) {
        results[index] = currentResult
        currentResult = defaultResult()
        results.add currentResult
        builder
    }

    def defaultResult() {
        [errors: [], success: 'true']
    }

    void setResultSuccess(String success) {
        currentResult.success = success
    }
}
