package com.confluex.test.salesforce

import org.junit.Test

class SforceResponseTest {
    @Test
    void responsesAreParsedCorrectlyWithErrorsAndSuccessIntermixed() {
        String data = '''
<env:Envelope xmlns:env='http://schemas.xmlsoap.org/soap/envelope/' xmlns:xsd='http://www.w3.org/2001/XMLSchema' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:sf='urn:partner.soap.sforce.com' xmlns:so='urn:sobject.partner.soap.sforce.com' xmlns:sd='http://www.force.com/2009/06/asyncapi/dataload'><env:Body><sf:upsertResponse>

<sf:result><sf:id>001234</sf:id><sf:success>true</sf:success></sf:result>
<sf:result><sf:id>002345</sf:id><sf:errors><sf:message>error message</sf:message><sf:statusCode>STATUS_CODE</sf:statusCode></sf:errors><sf:success>false</sf:success></sf:result>
<sf:result>
  <sf:id>003456</sf:id>
  <sf:errors>
    <sf:message>error message 1</sf:message>
    <sf:statusCode>STATUS CODE 1</sf:statusCode>
  </sf:errors>
  <sf:errors>
    <sf:message>error message 2</sf:message>
    <sf:statusCode>STATUS CODE 2</sf:statusCode>
  </sf:errors>
  <sf:success>false</sf:success>
</sf:result>
<sf:result><sf:id>004567</sf:id><sf:success>true</sf:success></sf:result>

</sf:upsertResponse></env:Body></env:Envelope>
'''

        SforceResponse response = new SforceResponse(data)

        assert 4 == response.results.size()
        assert response.results[0].success
        assert !response.results[1].success
        assert !response.results[2].success
        assert response.results[3].success

        assert 'STATUS_CODE' == response.results[1].errors[0].statusCode
        assert 'error message' == response.results[1].errors[0].message

        assert 'STATUS CODE 1' == response.results[2].errors[0].statusCode
        assert 'error message 1' == response.results[2].errors[0].message
    }
}
