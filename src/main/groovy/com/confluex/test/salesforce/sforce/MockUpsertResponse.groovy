package com.confluex.test.salesforce.sforce

class MockUpsertResponse {
    final MockUpsertBuilder builder
    private currentResult = defaultResult()
    private results = [currentResult]

    MockUpsertResponse(MockUpsertBuilder builder) {
        this.builder = builder
    }

    MockUpsertResponse withError(String statusCode, String message) {
        currentResult.errors << [statusCode: statusCode, message: message]
        this
    }

    MockUpsertBuilder forObject(int index) {
        if (results.contains(currentResult)) results.remove(currentResult)
        results[index] = currentResult
        currentResult = defaultResult()
        builder
    }

    def defaultResult() {
        [errors: [], success: 'true']
    }

    void setResultSuccess(String success) {
        currentResult.success = success
    }

    Map<String, Object> getResult(int index) {
        results[index] ?: defaultResult()
    }
}
