package com.confluex.test.salesforce.sforce

import com.confluex.mock.http.ClientRequest
import com.confluex.mock.http.MockHttpsServer
import com.confluex.test.salesforce.BaseBuilder

import static com.confluex.mock.http.matchers.HttpMatchers.body
import static com.confluex.mock.http.matchers.HttpMatchers.path
import static com.confluex.mock.http.matchers.HttpMatchers.stringHasXPath
import static org.hamcrest.Matchers.startsWith

class MockQueryBuilder extends BaseBuilder {
    MockQueryBuilder(MockHttpsServer mockHttpsServer) {
        super(mockHttpsServer)
    }

    MockQueryResponse returnResults() {
        def response = new MockQueryResponse()
        mockHttpsServer.respondTo(path(startsWith('/services/Soap/u/28.0'))
                .and(body(stringHasXPath('/Envelope/Body/query')))
        )
                .withStatus(200)
                .withBody() { ClientRequest request ->
                    def requestDoc = new XmlSlurper().parseText(request.body)
                    def query = requestDoc.Body.query.queryString.toString()

                    def rows = applyQuery(query, response.rows)
                    buildSoapEnvelope {
                        'env:Body' {
                            'sf:queryResponse' {
                                'sf:result'('xsi:type': 'sf:QueryResult') {
                                    'sf:done' 'true'
                                    'sf:queryLocator'('xsi:nil': 'true')
                                    rows.each{ row ->
                                        'sf:records'('xsi:type':'so:sObject') {
                                            'so:type'((query =~ /FROM (\w+)/)[0][1])
                                            mkp.yield buildRecord(row)
                                        }
                                    }
                                    'sf:size' "${response.rows.size()}"
                                }
                            }
                        }
                    }
        }

        response
    }

    private Closure buildRecord(Map<String, String> row) {
        return {
            row.keySet().each { fieldName ->
                def fieldValue = row[fieldName]
                if (fieldValue instanceof Map) {
                    "so:$fieldName"('xsi:type': 'so:sObject') {
                        mkp.yield buildRecord(fieldValue)
                    }
                } else {
                    "so:$fieldName"(fieldValue)
                }
            }
        }
    }

    private List<Map<String, Object>> applyQuery(String query, List<Map<String, Object>> rows) {
        rows.collect { copyWithFields(parseFields(query), it) }
    }

    private List<String> parseFields(String query) {
        (query =~ /SELECT (.+) FROM/)[0][1].split(',').collect { it.trim() }
    }

    private Map<String, Object> copyWithFields(List<String> fields, Map<String, Object> source) {
        def copy = [:]
        fields.each { field ->
            def parts = field.split('\\.').toList()
            def value = parts.inject(source) { map, key -> (map instanceof Map) ? (map[key] ?: '') : map }
            putIntoNestedMap(copy, parts, value)
        }
        copy
    }

    private void putIntoNestedMap(Map<String, Object> map, List<String> fields, String value) {
        def field = fields.remove(0)
        if (fields.isEmpty()) {
            map[field] = value
        } else {
            map[field] = map[field] ?: [:]
            putIntoNestedMap(map[field], fields, value)
        }
    }
}
