package ddf.catalog.impl.capability;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.source.Source;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class SourceCapabilityProviderImplTest {

  private static final String PROVIDER_ID = "id";

  private SourceCapabilityProviderImpl sourceCapabilityProvider;

  @Before
  public void setup() {
    sourceCapabilityProvider = new SourceCapabilityProviderImpl(PROVIDER_ID);
  }

  @Test
  public void testGetCapabilities() {

    String sourceId = "ddf.distribution";
    List<String> capabilities = Collections.singletonList("capabilty");

    sourceCapabilityProvider.setCapabilities(capabilities);
    sourceCapabilityProvider.setSourceId(sourceId);

    Source source = mock(Source.class);
    when(source.getId()).thenReturn(sourceId);

    assertThat(sourceCapabilityProvider.getSourceCapabilities(source), is(capabilities));
  }

  @Test
  public void testGetId() {
    assertThat(sourceCapabilityProvider.getId(), is(PROVIDER_ID));
  }

  @Test
  public void testGetCapabilitiesWithDifferentSourceId() {

    sourceCapabilityProvider.setSourceId("sourceId");

    Source source = mock(Source.class);
    when(source.getId()).thenReturn("difSourceId");

    assertThat(sourceCapabilityProvider.getSourceCapabilities(source), nullValue());
  }
}
