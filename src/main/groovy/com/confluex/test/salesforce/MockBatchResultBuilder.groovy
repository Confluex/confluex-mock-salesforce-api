package com.confluex.test.salesforce

import com.confluex.mock.http.MockHttpsServer
import com.confluex.mock.http.matchers.HttpRequestMatcher
import groovy.xml.StreamingMarkupBuilder
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.springframework.core.io.ClassPathResource

import static com.confluex.mock.http.matchers.HttpMatchers.*
import static org.hamcrest.Matchers.*

class MockBatchResultBuilder {

    MockHttpsServer mockHttpsServer
    def batchResultPathPattern = '/services/async/26\\.0/job/\\w+/batch/(\\w+)/result'
    Matcher<String> idMatcher = any(String)

    MockBatchResultBuilder(MockHttpsServer mockHttpsServer) {
        this.mockHttpsServer = mockHttpsServer
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
            slurpAndEditXml('/template/batch-result-response.xml') { root ->
                root.result.id = batchId
                root.result.success = 'true'
            }
        }
        new MockBatchResultResponse()
    }

    MockBatchResultResponse respondInvalid() {
        mockHttpsServer.respondTo(path(pathMatcher)).withStatus(400).withBody { request ->
            def batchId = (request.path =~ batchResultPathPattern )[0][1]
            slurpAndEditXml('/template/error-response.xml') { root ->
                root.exceptionCode = 'InvalidBatch'
                root.exceptionMessage = "Unable to find batch for id: $batchId"
            }
        }
        new MockBatchResultResponse()
    }

    MockBatchResultResponse respondIncomplete() {
        mockHttpsServer.respondTo(path(pathMatcher)).withStatus(400).withBody { request ->
            slurpAndEditXml('/template/error-response.xml') { root ->
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

    String slurpAndEditXml(String path, Closure editClosure) {
        slurpAndEditXml(new ClassPathResource(path).inputStream, editClosure)
    }

    String slurpAndEditXml(InputStream xml, Closure editClosure) {
        def root = new XmlSlurper().parse(xml)
        editClosure(root)
        new StreamingMarkupBuilder().bind {
            mkp.declareNamespace("": root.namespaceURI())
            mkp.yield root
        }
    }


}
