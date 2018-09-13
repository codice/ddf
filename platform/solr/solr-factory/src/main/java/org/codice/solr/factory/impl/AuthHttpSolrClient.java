package org.codice.solr.factory.impl;

import java.io.IOException;
import org.apache.http.util.Args;
import org.apache.solr.client.solrj.ResponseParser;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.StreamingResponseCallback;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.StreamingBinaryResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.SolrParams;

public class AuthHttpSolrClient extends HttpSolrClient {

  private String basicAuthUser;
  private String basicAuthCred;
  private boolean preemptiveAuth;

  public static class Builder extends HttpSolrClient.Builder {
    public Builder(String baseSolrUrl) {
      super(baseSolrUrl);
    }

    @Override
    public HttpSolrClient build() {
      return new AuthHttpSolrClient(this);
    }
  }

  protected AuthHttpSolrClient(Builder builder) {
    super(builder);
  }

  public SolrClient enableAuth(String user, String credentials) {
    Args.notBlank(user, "user name");
    Args.notBlank(credentials, "credentials");
    preemptiveAuth = true;
    this.basicAuthUser = user;
    this.basicAuthCred = credentials;
    return this;
  }

  public SolrClient disableAuth() {
    preemptiveAuth = false;
    return this;
  }

  @Override
  public QueryResponse query(String collection, SolrParams params)
      throws SolrServerException, IOException {
    return getQueryRequest(params).process(this, collection);
  }

  @Override
  public QueryResponse query(String collection, SolrParams params, METHOD method)
      throws SolrServerException, IOException {
    return getQueryRequest(params, method).process(this, collection);
  }

  @Override
  public QueryResponse queryAndStreamResponse(
      String collection, SolrParams params, StreamingResponseCallback callback)
      throws SolrServerException, IOException {
    ResponseParser parser = new StreamingBinaryResponseParser(callback);
    QueryRequest req = getQueryRequest(params);
    req.setStreamingResponseCallback(callback);
    req.setResponseParser(parser);
    return req.process(this, collection);
  }

  private QueryRequest getQueryRequest(SolrParams params, METHOD method) {
    QueryRequest queryRequest = new QueryRequest(params, method);
    setBasicAuthCredentials(queryRequest);
    return queryRequest;
  }

  private QueryRequest getQueryRequest(SolrParams params) {
    QueryRequest queryRequest = new QueryRequest(params);
    setBasicAuthCredentials(queryRequest);
    return queryRequest;
  }

  private void setBasicAuthCredentials(QueryRequest queryRequest) {
    if (preemptiveAuth) {
      queryRequest.setBasicAuthCredentials(basicAuthUser, basicAuthCred);
    }
  }
}
