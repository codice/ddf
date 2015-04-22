/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package sdk.ddf.soap.hello;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the sdk.ddf.soap.hello package.
 * <p>An ObjectFactory allows you to programatically
 * construct new instances of the Java representation
 * for XML content. The Java representation of XML
 * content can consist of schema derived interfaces
 * and classes representing the binding of schema
 * type definitions, element declarations and model
 * groups.  Factory methods for each of these are
 * provided in this class.
 */
@XmlRegistry
public class ObjectFactory {

    private static final QName HELLO_WORLD_RESPONSE_QNAME = new QName("http://ddf.sdk/soap/hello", "helloWorldResponse");

    private static final QName HELLO_WORLD_QNAME = new QName("http://ddf.sdk/soap/hello", "helloWorld");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: sdk.ddf.soap.hello
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link sdk.ddf.soap.hello.HelloWorldResponse }
     */
    public HelloWorldResponse createHelloWorldResponse() {
        return new HelloWorldResponse();
    }

    /**
     * Create an instance of {@link sdk.ddf.soap.hello.HelloWorld }
     */
    public HelloWorld createHelloWorld() {
        return new HelloWorld();
    }

    /**
     * Create an instance of {@link javax.xml.bind.JAXBElement }{@code <}{@link sdk.ddf.soap.hello.HelloWorldResponse }{@code >}}
     */
    @XmlElementDecl(namespace = "http://ddf.sdk/soap/hello", name = "helloWorldResponse")
    public JAXBElement<HelloWorldResponse> createHelloWorldResponse(HelloWorldResponse value) {
        return new JAXBElement<HelloWorldResponse>(HELLO_WORLD_RESPONSE_QNAME, HelloWorldResponse.class, null, value);
    }

    /**
     * Create an instance of {@link javax.xml.bind.JAXBElement }{@code <}{@link sdk.ddf.soap.hello.HelloWorld }{@code >}}
     */
    @XmlElementDecl(namespace = "http://ddf.sdk/soap/hello", name = "helloWorld")
    public JAXBElement<HelloWorld> createHelloWorld(HelloWorld value) {
        return new JAXBElement<HelloWorld>(HELLO_WORLD_QNAME, HelloWorld.class, null, value);
    }

}
