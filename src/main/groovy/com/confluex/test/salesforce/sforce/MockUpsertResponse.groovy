package com.confluex.test.salesforce.sforce

class MockUpsertResponse {
    def errors = []
    MockUpsertResponse withError(String statusCode, String message) {
        errors << [statusCode: statusCode, message: message]
        this
    }
}
