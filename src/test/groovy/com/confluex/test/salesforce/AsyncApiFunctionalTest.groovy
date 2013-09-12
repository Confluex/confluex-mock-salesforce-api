package com.confluex.test.salesforce

import com.sun.jersey.api.client.ClientResponse
import groovy.time.TimeCategory
import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import org.junit.Before
import org.junit.Test

import javax.xml.parsers.DocumentBuilderFactory

@Slf4j
class AsyncApiFunctionalTest extends AbstractFunctionalTest {

    @Before
    void disableXmlNamespaces() {
        def factory = DocumentBuilderFactory.newInstance()
        factory.namespaceAware = false
        builder = factory.newDocumentBuilder()
    }

    @Test
    public void createJobShouldReplyWithJobInfo() {
        Date started = new Date()
        use(TimeCategory) {
            started = started - 1.second
        }

        def writer = new StringWriter()
        def builder = new MarkupBuilder(writer)
        builder.jobInfo(xmlns: 'http://www.force.com/2009/06/asyncapi/dataload') {
            operation('upsert')
            object('Contact')
            externalIdFieldName('Unique_ID__c')
        }

        ClientResponse response = sslClient.resource('https://localhost:8090/services/async/26.0/job/')
                .entity(writer.toString(), 'text/xml; charset=UTF-8')
                .post(ClientResponse.class)
        assert 201 == response.status
        String responseBody = response.getEntity(String)
        log.debug responseBody

        assert 'upsert' == evalXpath('/jobInfo/operation', responseBody)
        assert 'Contact' == evalXpath('/jobInfo/object', responseBody)
        assert 'Unique_ID__c' == evalXpath('/jobInfo/externalIdFieldName', responseBody)
        assert evalXpath('/jobInfo/id', responseBody).length() > 0
        assert started <= Date.parse("yyyy-MM-dd'T'HH:mm:ss'Z'", evalXpath('/jobInfo/createdDate', responseBody))
        assert started <= Date.parse("yyyy-MM-dd'T'HH:mm:ss'Z'", evalXpath('/jobInfo/systemModstamp', responseBody))
    }

    @Test
    public void createBatchShouldReplyWithBatchInfo() {
        Date started = new Date()
        use(TimeCategory) {
            started = started - 1.second
        }

        def writer = new StringWriter()
        def builder = new MarkupBuilder(writer)
        builder.sObjects(xmlns: 'http://www.force.com/2009/06/asyncapi/dataload')

        ClientResponse response = sslClient.resource('https://localhost:8090/services/async/26.0/job/JOB001/batch')
                .entity(writer.toString(), 'text/xml; charset=UTF-8')
                .post(ClientResponse.class)
        assert 201 == response.status
        String responseBody = response.getEntity(String)
        log.debug responseBody

        assert evalXpath('/batchInfo/id', responseBody).length() > 0
        assert 'JOB001' == evalXpath('/batchInfo/jobId', responseBody)
        assert 'Queued' == evalXpath('/batchInfo/state', responseBody)
        assert started <= Date.parse("yyyy-MM-dd'T'HH:mm:ss'Z'", evalXpath('/batchInfo/createdDate', responseBody))
        assert started <= Date.parse("yyyy-MM-dd'T'HH:mm:ss'Z'", evalXpath('/batchInfo/systemModstamp', responseBody))
    }

    @Test
    public void checkBatchStatusShouldReplySuccessByDefault() {
        ClientResponse response = sslClient.resource('https://localhost:8090/services/async/26.0/job/JOB001/batch/ARBITRARYBATCH/result').get(ClientResponse.class)

        String responseBody = response.getEntity(String)
        log.debug responseBody

        assert 200 == response.status
        assert 'ARBITRARYBATCH' == evalXpath('/results/result/id', responseBody)
        assert 'true' == evalXpath('/results/result/success', responseBody)
    }

    @Test
    public void checkBatchStatusShouldReplyInvalidBatchAfterConfiguredAsSuch() {
        server.asyncApi().batchResult().forId("IMAGINARYBATCH").respondInvalid();
        ClientResponse response = sslClient.resource('https://localhost:8090/services/async/26.0/job/JOB001/batch/IMAGINARYBATCH/result').get(ClientResponse.class)

        String responseBody = response.getEntity(String)
        log.debug responseBody

        assert 400 == response.status
        assert 'InvalidBatch' == evalXpath('/error/exceptionCode', responseBody)
        assert 'Unable to find batch for id: IMAGINARYBATCH' == evalXpath('/error/exceptionMessage', responseBody)
    }

    @Test
    public void checkBatchStatusShouldReplyBatchNotCompletedAfterConfiguredAsSuch() {
        server.asyncApi().batchResult().forId("INCOMPLETEBATCH").respondIncomplete();

        ClientResponse response = sslClient.resource('https://localhost:8090/services/async/26.0/job/JOB001/batch/INCOMPLETEBATCH/result').get(ClientResponse.class)

        String responseBody = response.getEntity(String)
        log.debug responseBody

        assert 400 == response.status
        assert 'InvalidBatch' == evalXpath('/error/exceptionCode', responseBody)
        assert 'Batch not completed' == evalXpath('/error/exceptionMessage', responseBody)
    }

    @Test
    public void checkBatchStatusHonorsIdMatch() {
        def batchResource = sslClient.resource('https://localhost:8090/services/async/26.0/job/JOB001/batch')
        server.asyncApi().batchResult().forId("INCOMPLETEBATCH").respondIncomplete();
        assert 400 == batchResource.path('INCOMPLETEBATCH/result').get(ClientResponse.class).status
        assert 200 == batchResource.path('SOMEOTHERBATCH/result').get(ClientResponse.class).status
    }
}
