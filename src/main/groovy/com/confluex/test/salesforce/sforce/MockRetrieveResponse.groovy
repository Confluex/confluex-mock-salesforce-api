package com.confluex.test.salesforce.sforce

import groovy.util.logging.Slf4j
import org.w3c.dom.Document
import org.w3c.dom.Element

import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.stream.XMLOutputFactory
import javax.xml.transform.dom.DOMResult


@Slf4j
class MockRetrieveResponse {
    Map<String, String> fieldValues = [:]

    MockRetrieveResponse withField(String name, String value) {
        fieldValues[name] = value
        this
    }

    void buildFieldElements(mkp, List<String> fieldNames) {
        final def mapOfEmptyFields = fieldNames.collectEntries { [it, null] }
        final def mergedFieldValues = mapOfEmptyFields + fieldValues

        def nestedFieldValues = [:]
        mergedFieldValues.keySet().sort().each { fieldName ->
            def targetPosition = fieldName.split(/\./).toList()
            def cursorMap = nestedFieldValues
            while (targetPosition.size() > 1) {
                def levelName = targetPosition.remove(0)
                cursorMap = (cursorMap[levelName] = cursorMap[levelName] ?: [:]) // get or create level map
            }
            cursorMap[targetPosition[0]] = mergedFieldValues[fieldName]
        }

        mkp.yield buildFieldElements(nestedFieldValues)
    }

    Closure buildFieldElements(Map<String, Object> nestedFieldValues) {
        return {
            final buildField = { fieldName ->
                "so:$fieldName"(buildFieldValue(nestedFieldValues[fieldName]))
            }
            nestedFieldValues.keySet().each(buildField)
        }
    }

    def buildFieldValue(fieldValue) {
        if (fieldValue instanceof Map) return buildFieldElements(fieldValue)
        if (fieldValue == null) return ['xsi:nil': 'true']
        return fieldValue
    }

    Closure buildFieldElement(String fieldName) {
        return {
            "so:$fieldName"(fieldValues[fieldName] ?: ['xsi:nil':'true'])
        }
    }
}
