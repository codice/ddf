package org.codice.ddf.spatial.kml.util;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Style;
import org.junit.Test;

public class KmlTransformationsTest {

  @Test
  public void encloseKml() {
    final Document doc = new Document();
    final String docId = "1";
    final String docName = "myDocName";

    final Kml kml = KmlTransformations.encloseKml(doc, docId, docName);

    final Document enclosedDocument = (Document) kml.getFeature();

    assertThat(enclosedDocument.getId(), is(docId));
    assertThat(enclosedDocument.getName(), is(docName));
  }

  @Test
  public void encloseDoc() {
    final Placemark placemark = new Placemark();
    final Style style = new Style();
    final String docId = "1";
    final String docName = "myDocName";

    final Document document = KmlTransformations.encloseDoc(placemark, style, docId, docName);

    assertThat(document.getId(), is(docId));
    assertThat(document.getName(), is(docName));
    assertThat(document.getStyleSelector(), hasSize(1));
    assertThat(document.getFeature(), hasSize(1));
  }

  @Test
  public void encloseDocNullPlacemarkAndStyle() {
    final String documentId = "1";
    final String docName = "myDocName";

    final Document document = KmlTransformations.encloseDoc(null, null, documentId, docName);

    assertThat(document.getId(), is(documentId));
    assertThat(document.getName(), is(docName));
    assertThat(document.getStyleSelector(), hasSize(0));
    assertThat(document.getFeature(), hasSize(0));
  }
}
