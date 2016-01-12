package ddf.catalog.transformer.generic.xml;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.junit.BeforeClass;
import org.junit.Test;

import ddf.catalog.data.Metacard;

public class TestXMLInputTransformer {
    static XMLSaxEventHandlerImpl xmlSaxEventHandlerImpl;
//    XMLInputTransformer xmlInputTransformer;
    static SaxEventHandlerDelegate saxEventHandlerDelegate;
    static InputStream inputStream;

    @BeforeClass
    public static void setUp() throws FileNotFoundException {
        xmlSaxEventHandlerImpl = new XMLSaxEventHandlerImpl();
//        xmlInputTransformer = new XMLInputTransformer();
        saxEventHandlerDelegate = new SaxEventHandlerDelegate(xmlSaxEventHandlerImpl);
        inputStream = new FileInputStream(
                "../catalog-transformer-xml/src/test/resources/invalidExtensibleMetacard.xml");
    }

    @Test
    public void testNormalTransform() {
        Metacard metacard = saxEventHandlerDelegate.read(inputStream);
        assertThat(metacard.getAttribute(Metacard.TITLE).getValues().get(0), is("Title!"));
    }
}
