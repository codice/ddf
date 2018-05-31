package org.codice.ddf.spatial.geocoding.context.impl;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.codice.ddf.spatial.geocoding.Suggestion;
import org.junit.Test;

public class SuggestionImplTest {

  @Test
  public void testConstructor() {
    Suggestion suggestion = new SuggestionImpl("id1", "name1");
    assertThat(suggestion.getId(), is("id1"));
    assertThat(suggestion.getName(), is("name1"));
  }
}
