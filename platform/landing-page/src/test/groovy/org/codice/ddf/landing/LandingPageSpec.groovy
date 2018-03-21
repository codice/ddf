package org.codice.ddf.landing;

import spock.lang.Specification

class LandingPageSpec extends Specification {

    def "test valid links"() {
        setup:
        def landingPage = new LandingPage()
        landingPage.setLinks(Arrays.asList(input))

        expect:
        landingPage.getParsedLinks().get(text).equals(url)

        where:
        input                            | text          | url
        "example, example.com"           | "example"     | "example.com"
        "example,example.com"            | "example"     | "example.com"
        "  example  , \t example.com   " | "example"     | "example.com"
        "word1 word2, example.com  "     | "word1 word2" | "example.com"
    }

    def "test invalid links"() {
        setup:
        def landingPage = new LandingPage()
        landingPage.setLinks(Arrays.asList(
                "",
                ",,,",
                ",,what",
                "example, example.com,",
                "   ,example.com",
                "   ,   \t"))

        expect:
        landingPage.getParsedLinks().keySet().isEmpty()
    }
}