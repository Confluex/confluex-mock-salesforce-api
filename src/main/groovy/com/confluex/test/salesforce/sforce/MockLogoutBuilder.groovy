package com.confluex.test.salesforce.sforce

import com.confluex.mock.http.ClientRequest
import com.confluex.mock.http.MockHttpsServer
import com.confluex.test.salesforce.BaseBuilder

import static com.confluex.mock.http.matchers.HttpMatchers.*
import static org.hamcrest.Matchers.startsWith

class MockLogoutBuilder extends BaseBuilder {
  MockLogoutBuilder(MockHttpsServer mockHttpsServer) {
    super(mockHttpsServer)
  }

  def matchLogoutRequests() {

      /*
      Request
<?xml version="1.0" encoding="UTF-8"?>
<env:Envelope xmlns:env="http://schemas.xmlsoap.org/soap/envelope/" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <env:Header>
        <SessionHeader xmlns="urn:partner.soap.sforce.com">
            <sessionId>MOCKSESSIONID</sessionId>
        </SessionHeader>
    </env:Header>
    <env:Body>
        <m:logout xmlns:m="urn:partner.soap.sforce.com" xmlns:sobj="urn:sobject.partner.soap.sforce.com" />
    </env:Body>
</env:Envelope>
       */

      /*
      Response
<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns="urn:partner.soap.sforce.com">
    <soapenv:Body>
        <logoutResponse />
    </soapenv:Body>
</soapenv:Envelope>
       */

    mockHttpsServer.respondTo(path(startsWith('/services/Soap/u/28.0'))
        .and(body(stringHasXPath('/Envelope/Body/logout')))
    ).withStatus(200).withBody { ClientRequest request ->
      buildSoapEnvelope {
        'env:Body' {
          'sf:logoutResponse' {
          }
        }
      }
    }
  }
}
