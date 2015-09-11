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

You can start the server from command line using [Exec Maven Plugin](http://www.mojohaus.org/exec-maven-plugin/):

    >  mvn compile exec:java -Dexec.mainClass=com.confluex.test.salesforce.MockSalesforceApiServer -Dexec.args="443"
     
#### Caveats
If you get an error like this

    java.lang.reflect.InvocationTargetException
        at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
        ...    
    Caused by: java.net.BindException: Permission denied

it means that you might need specific permissions in order to bind the process to certain ports. In this case try using
a different port or running the process with a user with sufficient permissions.