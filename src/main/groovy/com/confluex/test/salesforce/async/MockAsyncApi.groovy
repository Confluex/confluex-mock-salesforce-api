package com.confluex.test.salesforce.async

import com.confluex.mock.http.MockHttpsServer
import groovy.xml.StreamingMarkupBuilder
import org.springframework.core.io.ClassPathResource

import static com.confluex.mock.http.matchers.HttpMatchers.matchesPattern
import static com.confluex.mock.http.matchers.HttpMatchers.path
import static com.confluex.mock.http.matchers.HttpMatchers.path


class MockAsyncApi {
    MockHttpsServer mockHttpsServer

    MockAsyncApi(MockHttpsServer mockHttpsServer) {
        this.mockHttpsServer = mockHttpsServer
        defaults()
    }

    void defaults() {
        batchResult().respondSuccess()

        def batchPathPattern = '/services/async/26\\.0/job/(\\w+)/batch'
        mockHttpsServer.respondTo(path(matchesPattern(batchPathPattern))).withStatus(201).withBody { request ->
            def jobId = (request.path =~ batchPathPattern)[0][1]
            slurpAndEditXml('/template/create-batch-response.xml') { root ->
                root.id = '00MOCK0000batch'
                root.jobId = jobId
                root.createdDate = formatDate(new Date())
                root.systemModstamp = formatDate(new Date())
            }
        }
        mockHttpsServer.respondTo(path('/services/async/26.0/job/')).withStatus(201).withBody { request ->
            def requestXml = new XmlSlurper().parseText(request.body)
            slurpAndEditXml('/template/create-job-response.xml') { root ->
                root.id = '00MOCK000000job'
                root.operation = requestXml.operation.text()
                root.object = requestXml.object.text()
                root.externalIdFieldName = requestXml.externalIdFieldName.text()
                root.createdDate = formatDate(new Date())
                root.systemModstamp = formatDate(new Date())
            }
        }
    }

    MockBatchResultBuilder batchResult() {
        new MockBatchResultBuilder(mockHttpsServer)
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

    String formatDate(Date date) {
        date.format("yyyy-MM-dd'T'HH:mm:ss'Z'")
    }

}
