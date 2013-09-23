package com.confluex.test.salesforce.streaming

import com.confluex.mock.http.MockHttpsServer
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicInteger

import static com.confluex.mock.http.matchers.HttpMatchers.*

@Slf4j
class MockStreamingApi {
    MockHttpsServer mockHttpsServer
    PushTopicSynchronizer pushTopicSynchronizer = new PushTopicSynchronizer()

    public MockStreamingApi(MockHttpsServer mockHttpsServer) {
        this.mockHttpsServer = mockHttpsServer
        defaults()
    }

    void defaults() {
        mockHttpsServer.respondTo(post('/cometd/26.0/handshake')).withBody { request ->
            def requestBody = new JsonSlurper().parseText(request.body)
            new JsonBuilder([
                    [
                            id: requestBody[0].id,
                            minimumVersion: '1.0',
                            supportedConnectionTypes: ['long-polling'],
                            successful: true,
                            channel: '/meta/handshake',
                            clientId: '111111111111111111111111111',
                            version: '1.0'
                    ]
            ]).toString()
        }

        mockHttpsServer.respondTo(post('/cometd/26.0/connect')).withBody { request ->
            def requestBody = new JsonSlurper().parseText(request.body)
            def responseMessages = [[
                    id: requestBody[0].id,
                    channel: '/meta/connect',
                    successful: true
            ]]
            responseMessages.addAll(pushTopicSynchronizer.waitForPublish()) // waits for messages to arrive
            pushTopicSynchronizer.notifyComplete() // waits for all threads to reach this line
            new JsonBuilder(responseMessages.collect { it.clientId = requestBody[0].clientId; it }).toString()
        }
        // TODO: connect with advice -> come back with a response telling the client a timeout to use.

        mockHttpsServer.respondTo(post('/cometd/26.0/subscribe')).withBody { request ->
            def requestBody = new JsonSlurper().parseText(request.body)
            new JsonBuilder([[
                    id:requestBody[0].id,
                    subscription: requestBody.subscription,
                    successful: true,
                    channel: '/meta/subscribe',
                    clientId: requestBody[0].clientId
            ]]).toString()
        }

        mockHttpsServer.respondTo(post('/cometd/26.0/disconnect')).withBody { request ->
            def requestBody = new JsonSlurper().parseText(request.body)
            pushTopicSynchronizer.publish([])
            new JsonBuilder([[
                    id:requestBody[0].id,
                    successful: true,
                    channel: '/meta/disconnect',
                    clientId: requestBody[0].clientId
            ]]).toString()
        }
    }

    void publish(String topic, Map<String, String>... messages) {
        pushTopicSynchronizer.publish(messages.collect { message ->
            [
                    data: [
                            sobject: message,
                            event: [
                                    type: 'updated',
                                    createdDate: new Date().format("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                            ]
                    ],
                    channel: "/topic/$topic"
            ]
        })
    }

    class PushTopicSynchronizer {
        CountDownLatch latch
        AtomicInteger locks
        List<Map<String, String>> messages
        CyclicBarrier barrier

        PushTopicSynchronizer() {
            reset()
        }

        private void reset() {
            latch = new CountDownLatch(1)
            locks = new AtomicInteger(0)
            messages = []
        }

        Collection<Map<String, String>> waitForPublish() {
            locks.incrementAndGet()
            try {
                latch.await()
                return messages
            } catch (InterruptedException e) {}
        }

        void notifyComplete() {
            barrier.await()
        }

        void publish(List<Map<String, String>> messages) {
            // possible race condition if two threads call publish?  reset() sets messages, waitForPublish() threads read messages
            if (locks.get() > 0) {
                this.messages.addAll(messages)
                barrier = new CyclicBarrier(locks.get(), { reset() } as Runnable)
                latch.countDown()
            } else {
                log.info("No subscribers to notify")
            }
        }
    }
}
