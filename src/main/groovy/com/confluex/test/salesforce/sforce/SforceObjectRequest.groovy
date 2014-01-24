package com.confluex.test.salesforce.sforce

class SforceObjectRequest extends SforceRequest {
    Map<String, String> fields = [:]

    SforceObjectRequest(String xml) {
        super(xml)
    }
}
