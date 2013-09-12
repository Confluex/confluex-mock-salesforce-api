package com.confluex.test.salesforce

class MockRetrieveResponse {
    Map<String, String> fieldValues = [:]

    MockRetrieveResponse withField(String name, String value) {
        fieldValues[name] = value
        this
    }

    void buildFieldElements(mkp, List<String> fieldNames) {
        fieldNames.each { field -> mkp.yield buildFieldElement(field) }
    }
    Closure buildFieldElement(String fieldName) {
        return {
            "so:$fieldName"(fieldValues[fieldName] ?: ['xsi:nil':'true'])
        }
    }
}
