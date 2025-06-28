package com.flipkart.krystal.lattice.rest;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import org.jboss.resteasy.specimpl.ResteasyUriInfo;

final class UriInfoImpl implements UriInfo {

  private final ResteasyUriInfo delegate;

  UriInfoImpl(UriInfo delegate) {
    this.delegate = new ResteasyUriInfo(delegate.getRequestUri());
  }

  @Override
  public String getPath() {
    return delegate.getPath();
  }

  @Override
  public String getPath(boolean decode) {
    return delegate.getPath(decode);
  }

  @Override
  public List<PathSegment> getPathSegments() {
    return delegate.getPathSegments();
  }

  @Override
  public List<PathSegment> getPathSegments(boolean decode) {
    return delegate.getPathSegments(decode);
  }

  @Override
  public URI getRequestUri() {
    return delegate.getRequestUri();
  }

  @Override
  public UriBuilder getRequestUriBuilder() {
    return delegate.getRequestUriBuilder();
  }

  @Override
  public URI getAbsolutePath() {
    return delegate.getAbsolutePath();
  }

  @Override
  public UriBuilder getAbsolutePathBuilder() {
    return delegate.getAbsolutePathBuilder();
  }

  @Override
  public URI getBaseUri() {
    return delegate.getBaseUri();
  }

  @Override
  public UriBuilder getBaseUriBuilder() {
    return delegate.getBaseUriBuilder();
  }

  @Override
  public MultivaluedMap<String, String> getPathParameters() {
    return delegate.getPathParameters();
  }

  @Override
  public MultivaluedMap<String, String> getPathParameters(boolean decode) {
    return delegate.getPathParameters(decode);
  }

  @Override
  public MultivaluedMap<String, String> getQueryParameters() {
    return delegate.getQueryParameters();
  }

  @Override
  public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
    return delegate.getQueryParameters(decode);
  }

  @Override
  public List<String> getMatchedURIs() {
    return delegate.getMatchedURIs();
  }

  @Override
  public String getMatchedResourceTemplate() {
    return delegate.getMatchedResourceTemplate();
  }

  @Override
  public List<String> getMatchedURIs(boolean decode) {
    return delegate.getMatchedURIs(decode);
  }

  @Override
  public List<Object> getMatchedResources() {
    return delegate.getMatchedResources();
  }

  @Override
  public URI resolve(URI uri) {
    return delegate.resolve(uri);
  }

  @Override
  public URI relativize(URI uri) {
    return delegate.relativize(uri);
  }
}
