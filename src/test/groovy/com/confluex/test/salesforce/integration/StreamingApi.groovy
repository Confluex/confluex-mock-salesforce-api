package com.confluex.test.salesforce.integration

import com.confluex.test.salesforce.util.SfdcStreamingClient
import com.sun.jersey.api.client.Client
import com.sun.jersey.api.client.ClientResponse
import groovy.util.logging.Slf4j
import groovy.xml.StreamingMarkupBuilder
import org.cometd.bayeux.Message
import org.cometd.bayeux.client.ClientSessionChannel
import org.cometd.client.BayeuxClient
import org.cometd.client.transport.LongPollingTransport
import org.eclipse.jetty.client.ContentExchange
import org.eclipse.jetty.client.HttpClient
import org.junit.Before
import org.junit.Test
import org.springframework.core.io.ClassPathResource

import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

@Slf4j
class StreamingApi {
    Properties config

    @Before
    void initConfig() {
        config = new Properties()
        config.load(new ClassPathResource('config-local.properties').inputStream)
    }

    @Test
    void doIt() {
        def client = new SfdcStreamingClient(config['sfdc.username'], config['sfdc.password'], config['sfdc.securityToken'], config['sfdc.url'])
        client.login()
        assert null != client.sessionId && client.sessionId.length() > 0
        client.subscribeTopic('RyanContactUpdates') { message ->
            log.info "Whoopee a message! $message"
        }
        Thread.sleep(300000)
    }

}
