package com.confluex.test.salesforce

import com.confluex.mock.http.MockHttpsServer
import groovy.util.slurpersupport.GPathResult
import groovy.xml.StreamingMarkupBuilder
import org.springframework.core.io.ClassPathResource

class BaseBuilder {
    MockHttpsServer mockHttpsServer

    BaseBuilder(MockHttpsServer mockHttpsServer) {
        this.mockHttpsServer = mockHttpsServer
    }

    String slurpAndEditXmlResource(String path, Closure editClosure) {
        slurpAndEditXml(new ClassPathResource(path).inputStream, editClosure)
    }

    String slurpAndEditXml(String xml, Closure editClosure) {
        slurpAndEditXml(new ByteArrayInputStream(xml.bytes), editClosure)
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
