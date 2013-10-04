package com.confluex.test.salesforce.sforce

class MockQueryResponse {
    def rows = []
    MockQueryResponse withRow() {
        rows << [:]
        this
    }

    MockQueryResponse withField(String name, String value) {
        setField(name, value, rows[-1])
        this
    }

    private void setField(String name, String value, Map<String, String> object) {
        if (! name.contains('.')) {
            object[name] = value
            return
        }
        def partMatcher = name =~ /(\w+)\.(.*)/
        def parentName = partMatcher[0][1]
        def parent = object[parentName] = [:]
        setField(partMatcher[0][2], value, parent)
    }
}
