package com.confluex.test.salesforce

class SforceResponse {
    List<SforceResponseResult> results = []
    SforceResponse(String data) {
        def envelope = new XmlSlurper().parseText(data).declareNamespace(env: 'http://schemas.xmlsoap.org/soap/envelope/', sf: 'urn:partner.soap.sforce.com')

        envelope.'env:Body'.'sf:upsertResponse'.'sf:result'.each {
            boolean success = Boolean.parseBoolean(it.'sf:success'.text())

            if (success) {
                results << new SforceResponseResult([success: true])
            }
            else {
                SforceResponseResult result = new SforceResponseResult([success: false])

                it.'sf:errors'.each {
                    result.errors << new SforceResponseResultError([statusCode: it.'sf:statusCode'.text(), message: it.'sf:message'.text()])
                }

                results << result
            }
        }
    }
}
