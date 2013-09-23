package com.confluex.test.salesforce.sforce

class SforceUpdateRequest extends SforceRequest {
    Map<String, String> fields = [:]

    SforceUpdateRequest(String xml) {
        super(xml)
    }
}
