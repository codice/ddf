package org.codice.ddf.spatial.kml.util;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonMap;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

public class KmlMarshallerTest {

  private KmlMarshaller kmlMarshaller;

  @Before
  public void setup() {
    setupXpath();
    kmlMarshaller = new KmlMarshaller();
  }

  void setupXpath() {
    NamespaceContext ctx =
        new SimpleNamespaceContext(singletonMap("m", "http://www.opengis.net/kml/2.2"));
    XMLUnit.setXpathNamespaceContext(ctx);
  }

  @Test
  public void marshall() throws SAXException, IOException, XpathException {

    Placemark placemark = new Placemark();
    placemark.setName("a");

    Kml kml = new Kml();
    kml.setFeature(placemark);
    final String kmlString = kmlMarshaller.marshal(kml);

    assertXpathExists("/m:kml", kmlString);
    assertXpathEvaluatesTo("a", "//m:Placemark/m:name", kmlString);
  }

  @Test(expected = IllegalArgumentException.class)
  public void marshallNull() {
    kmlMarshaller.marshal(null);
  }

  @Test
  public void unmarshall() {
    final InputStream resourceAsStream = this.getClass().getResourceAsStream("/kmlPoint.kml");
    final Kml kml = kmlMarshaller.unmarshal(resourceAsStream).get();
    final Feature feature = kml.getFeature();

    assertThat(feature.getName(), is("Simple placemark"));
  }

  @Test(expected = NoSuchElementException.class)
  public void unmarshallNullStream() {
    final Kml kml = kmlMarshaller.unmarshal(null).get();
    final Feature feature = kml.getFeature();

    assertThat(feature.getName(), is("Simple placemark"));
  }

  @Test(expected = NoSuchElementException.class)
  public void unmarshallEmptyStream() {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream("".getBytes(UTF_8));
    final Kml kml = kmlMarshaller.unmarshal(inputStream).get();
    final Feature feature = kml.getFeature();

    assertThat(feature.getName(), is("Simple placemark"));
  }
}
