package ddf.security.samlp.impl

import ddf.security.samlp.SamlProtocol
import org.xml.sax.SAXException
import org.xml.sax.SAXParseException
import spock.lang.Specification

class HtmlResponseTemplateSpec extends Specification {
    static final String URL = "https://mytargeturl.com/idp/stuffs"
    static final String ENCODED_SAML = "000b64encoded0saml0object+but0not0really000"
    static final String RELAY_STATE = "0400-4c12-40a07b-123abcd"

    def "get post page"() {
        setup:
        String result
        def xml

        when:
        result = HtmlResponseTemplate.getPostPage(
                URL,
                SamlProtocol.Type.REQUEST,
                ENCODED_SAML,
                RELAY_STATE)
        xml = getXmlParser().parseText(result)

        then:
        result.contains(URL)
        result.contains(ENCODED_SAML)
        result.contains(SamlProtocol.Type.REQUEST.getKey())
        result.contains(RELAY_STATE)
        notThrown(SAXException)

        when:
        result = HtmlResponseTemplate.getPostPage(
                URL,
                SamlProtocol.Type.RESPONSE,
                ENCODED_SAML,
                RELAY_STATE)
        xml = getXmlParser().parseText(result)

        then:
        result.contains(URL)
        result.contains(ENCODED_SAML)
        result.contains(SamlProtocol.Type.RESPONSE.getKey())
        result.contains(RELAY_STATE)
        notThrown(SAXException)
    }

    def "get post page with null"() {
        when:
        String result = HtmlResponseTemplate.getPostPage(null, null, null, null)

        then:
        notThrown(NullPointerException)
        !result.contains("null")
    }

    def "validate invalid xml fails"() {
        when:
        String result = HtmlResponseTemplate.getPostPage(
                "bla",
                SamlProtocol.Type.REQUEST,
                "bla",
                "bla").replace("</html>", ">>>topkek>>>")


        withNullStandardError {
            def xml = getXmlParser().parseText(result)
        }


        then:
        thrown(SAXParseException)
    }

    def "get redirect page"() {
        when:
        String result = HtmlResponseTemplate.getRedirectPage(URL)
        def xml = getXmlParser().parseText(result)

        then:
        result.contains(URL)
        notThrown(SAXParseException)
    }

    void withNullStandardError(Closure closure) {
        def terr = System.err
        try {
            System.err = new PrintStream(new ByteArrayOutputStream())
            closure.call()
        } finally {
            System.err = terr
        }
    }

    def getXmlParser() {
        def parser = new XmlParser()
        parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
        parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        return parser
    }
}
