package ddf.platform.solr.security.security;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.io.InputStream;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("admin/authentication")
public interface SolrAdminClient {

  @POST
  @Produces(APPLICATION_JSON)
  @Consumes(APPLICATION_JSON)
  Response sendRequest(InputStream is);
}
