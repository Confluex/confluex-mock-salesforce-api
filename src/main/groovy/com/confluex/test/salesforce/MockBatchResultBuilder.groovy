package com.confluex.test.salesforce

import com.confluex.mock.http.MockHttpsServer
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher

import static com.confluex.mock.http.matchers.HttpMatchers.*
import static org.hamcrest.Matchers.*

class MockBatchResultBuilder extends BaseBuilder {

    def batchResultPathPattern = '/services/async/26\\.0/job/\\w+/batch/(\\w+)/result'
    Matcher<String> idMatcher = any(String)

    MockBatchResultBuilder(MockHttpsServer mockHttpsServer) {
        super(mockHttpsServer)
    }

    MockBatchResultBuilder forId(String id) {
        forId(equalTo(id))
    }

    MockBatchResultBuilder forId(Matcher<String> idMatcher) {
        this.idMatcher = idMatcher
        this
    }

    MockBatchResultResponse respondSuccess() {
        mockHttpsServer.respondTo(path(pathMatcher)).withStatus(200).withBody { request ->
            def batchId = (request.path =~ batchResultPathPattern )[0][1]
            slurpAndEditXmlResource('/template/batch-result-response.xml') { root ->
                root.result.id = batchId
                root.result.success = 'true'
            }
        }
        new MockBatchResultResponse()
    }

    MockBatchResultResponse respondInvalid() {
        mockHttpsServer.respondTo(path(pathMatcher)).withStatus(400).withBody { request ->
            def batchId = (request.path =~ batchResultPathPattern )[0][1]
            slurpAndEditXmlResource('/template/error-response.xml') { root ->
                root.exceptionCode = 'InvalidBatch'
                root.exceptionMessage = "Unable to find batch for id: $batchId"
            }
        }
        new MockBatchResultResponse()
    }

    MockBatchResultResponse respondIncomplete() {
        mockHttpsServer.respondTo(path(pathMatcher)).withStatus(400).withBody { request ->
            slurpAndEditXmlResource('/template/error-response.xml') { root ->
                root.exceptionCode = 'InvalidBatch'
                root.exceptionMessage = "Batch not completed"
            }
        }
        new MockBatchResultResponse()
    }

    Matcher<String> getPathMatcher() {
        new BaseMatcher<String>() {
            @Override
            boolean matches(Object s) {
                def regexMatcher = (s =~ batchResultPathPattern)
                regexMatcher.matches() && idMatcher.matches(regexMatcher[0][1])
            }

            @Override
            void describeTo(Description description) {
                description.appendText("a string matching the regular expression ")
                        .appendValue(batchResultPathPattern)
                        .appendText(" and with the first group matching ")
                        .appendDescriptionOf(idMatcher)
            }
        }
    }
}
