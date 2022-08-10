/*
Copyright (C) 2016-2020 Sysdig

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.sysdig.jenkins.plugins.sysdig.client;

import com.google.common.base.Strings;
import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.regex.Pattern;

public class SysdigSecureClientImpl implements SysdigSecureClient {
  private final String token;
  private final String apiURL;
  private final boolean verifySSL;
  private final SysdigLogger logger;

  public SysdigSecureClientImpl(String token, String apiURL, boolean verifySSL, SysdigLogger logger) {
    this.token = token;
    this.apiURL = apiURL.replaceAll("/+$", "");
    this.verifySSL = verifySSL;
    this.logger = logger;
  }

  @Override
  public String submitImageForScanning(String tag, String dockerFileContents, Map<String, String> annotations, boolean forceScan) throws ImageScanningException {
    try (CloseableHttpClient httpclient = makeHttpClient(verifySSL)) {
      String imagesUrl = String.format("%s/api/scanning/v1/anchore/images?force=%b", apiURL, forceScan);

      JSONObject jsonBody = new JSONObject();
      jsonBody.put("tag", tag);

      if (null != dockerFileContents) {
        jsonBody.put("dockerfile", dockerFileContents);
      }

      if (null != annotations) {
        jsonBody.put("annotations", annotations);
      }

      String body = jsonBody.toString();

      HttpPost httppost = new HttpPost(imagesUrl);
      httppost.addHeader("Content-Type", "application/json");
      httppost.addHeader("Authorization", String.format("Bearer %s", token));
      httppost.setEntity(new StringEntity(body));

      logger.logDebug("Sending request: " + httppost.toString());
      logger.logDebug("Body:\n" + body);

      try (CloseableHttpResponse response = httpclient.execute(httppost)) {
        String responseBody = EntityUtils.toString(response.getEntity());
        logger.logDebug("Response: " + response.getStatusLine().toString());
        logger.logDebug("Response body:\n" + responseBody);

        int statusCode = response.getStatusLine().getStatusCode();

        if (statusCode != 200) {
          throw new ImageScanningException(String.format("sysdig-secure-engine add image failed. URL: %s, status: %s, error: %s", imagesUrl, response.getStatusLine(), responseBody));
        }

        return JSONObject.fromObject(JSONArray.fromObject(responseBody).get(0)).getString("imageDigest");
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      logger.logDebug("Error: ", e);
      throw new ImageScanningException(e);
    }
  }

  @Override
  public JSONObject retrieveImageScanningVulnerabilities(String imageDigest) throws ImageScanningException {
    try (CloseableHttpClient httpclient = makeHttpClient(verifySSL)) {
      String url = String.format("%s/api/scanning/v1/anchore/images/%s/vuln/all", apiURL, imageDigest);

      HttpGet httpget = new HttpGet(url);
      httpget.addHeader("Content-Type", "application/json");
      httpget.addHeader("Authorization", String.format("Bearer %s", token));

      logger.logDebug("Sending request: " + httpget.toString());

      try (CloseableHttpResponse response = httpclient.execute(httpget)) {
        String responseBody = EntityUtils.toString(response.getEntity());
        logger.logDebug("Response: " + response.getStatusLine().toString());
        logger.logDebug("Response body:\n" + responseBody);

        if (response.getStatusLine().getStatusCode() != 200) {
          throw new ImageScanningException(String.format("Error while retrieving the image vulnerabilities: %s", responseBody));
        }

        return JSONObject.fromObject(responseBody);
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      logger.logDebug("Error: ", e);
      throw new ImageScanningException(e);
    }
  }

    @Override
  public JSONArray retrieveImageScanningResults(String tag, String imageDigest) throws ImageScanningException {
    try (CloseableHttpClient httpclient = makeHttpClient(verifySSL)) {
      String url = String.format("%s/api/scanning/v1/anchore/images/%s/check?tag=%s&detail=true", apiURL, imageDigest, tag);

      HttpGet httpget = new HttpGet(url);
      httpget.addHeader("Content-Type", "application/json");
      httpget.addHeader("Authorization", String.format("Bearer %s", token));

      logger.logDebug("Sending request: " + httpget.toString());

      try (CloseableHttpResponse response = httpclient.execute(httpget)) {
        String responseBody = EntityUtils.toString(response.getEntity());

        logger.logDebug("Response: " + response.getStatusLine().toString());
        logger.logDebug("Response body:\n" + responseBody);

        if (response.getStatusLine().getStatusCode() != 200) {
          throw new ImageScanningException(String.format("Error while retrieving the image scanning results: %s", responseBody));
        }

        return JSONArray.fromObject(responseBody);
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      logger.logDebug("Error: ", e);
      throw new ImageScanningException(e);
    }
  }

  private static CloseableHttpClient makeHttpClient(boolean verifySSL) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
    HttpClientBuilder clientBuilder = HttpClients.custom();

    // Option to skip TLS certificate verification
    if (!verifySSL) {
      SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
      sslContextBuilder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
      SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
        sslContextBuilder.build(),
        NoopHostnameVerifier.INSTANCE);
      clientBuilder.setSSLSocketFactory(sslsf);
    }

    clientBuilder.useSystemProperties();

    // Add proxy configuration to the client
    ProxyConfiguration proxyConfiguration = Jenkins.getInstance().proxy;
    if (proxyConfiguration != null && !Strings.isNullOrEmpty(proxyConfiguration.name)) {
      HttpHost proxy = new HttpHost(proxyConfiguration.name, proxyConfiguration.port, "http");

      if (proxyConfiguration.getNoProxyHostPatterns().size() > 0) {
        HttpRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy) {

          @Override
          public HttpRoute determineRoute(
            final HttpHost host,
            final HttpRequest request,
            final HttpContext context) throws HttpException {
            String hostname = host.getHostName();
            for (Pattern p: proxyConfiguration.getNoProxyHostPatterns()) {
              if (p.matcher(hostname).matches()) {
                // Return direct route
                return new HttpRoute(host);
              }
            }
            return super.determineRoute(host, request, context);
          }
        };

        clientBuilder.setRoutePlanner(routePlanner);
      }

      clientBuilder.setProxy(proxy);

      if (!Strings.isNullOrEmpty(proxyConfiguration.getUserName())) {
        Credentials credentials = new UsernamePasswordCredentials(proxyConfiguration.getUserName(),proxyConfiguration.getPassword());
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials( new AuthScope(proxyConfiguration.name, proxyConfiguration.port), credentials);
        clientBuilder.setDefaultCredentialsProvider(credsProvider);
        clientBuilder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());
      }
    }
    return clientBuilder.build();
  }
}
