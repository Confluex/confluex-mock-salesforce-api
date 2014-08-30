package com.confluex.test.salesforce.sforce

import com.confluex.mock.http.MockHttpsServer
import com.confluex.mock.http.matchers.HttpMatchers

class MockSforceApi {
    MockHttpsServer mockHttpsServer

    public MockSforceApi(MockHttpsServer mockHttpsServer) {
        this.mockHttpsServer = mockHttpsServer
        defaults()
    }

    void defaults() {
        logout()
        retrieve().returnObject()
        update().returnSuccess()
        upsert().returnSuccess()
        query().returnResults()
		getUserInfo().returnObject()
		authorize().returnObject()
    }

    List<SforceRequest> getRequests(String call) {
        mockHttpsServer.requests.findAll {
            it.path =~ /\/services\/Soap\/u\/28.0/ && xpath("/Envelope/Body/$call", it.body)
        }.collect {
            switch(call) {
                case 'update':
                    update().capture(it)
                    break
                case 'upsert':
                    upsert().capture(it)
                    break
                default:
                    new SforceRequest(it.body)
            }
        }
    }

    MockRetrieveBuilder retrieve() {
        new MockRetrieveBuilder(mockHttpsServer)
    }

    MockUpdateBuilder update() {
        new MockUpdateBuilder(mockHttpsServer)
    }

    MockQueryBuilder query() {
        new MockQueryBuilder(mockHttpsServer)
    }

    MockUpsertBuilder upsert() {
        new MockUpsertBuilder(mockHttpsServer)
    }
    
    MockGetUserInfoBuilder getUserInfo() {
        new MockGetUserInfoBuilder(mockHttpsServer)
    }
	
	MockAuthorizeBuilder authorize() {
		new MockAuthorizeBuilder(mockHttpsServer)
	}

    private xpath(String xpath, String xml) {
        HttpMatchers.stringHasXPath(xpath).matches(xml)
    }

    MockLogoutBuilder logout() {
      new MockLogoutBuilder(mockHttpsServer)
    }
}
