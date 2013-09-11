confluex-mock-salesforce-api
============================

To help you write isolated tests for your salesforce API client code

### Alpha
This library is still very much in alpha, and none of the APIs are considered stable.

### Quick Start

This will start a server on localhost that tries to respond reasonably by default.

```groovy

MockSalesforceApiServer mockServer = new MockSalesforceApiServer(443)

```
