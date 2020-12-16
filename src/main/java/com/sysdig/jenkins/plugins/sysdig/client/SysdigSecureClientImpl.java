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

import com.sysdig.jenkins.plugins.sysdig.log.SysdigLogger;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Map;

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
  public String submitImageForScanning(String tag, String dockerFileContents, Map<String, String> annotations) throws ImageScanningException {
    String imagesUrl = String.format("%s/api/scanning/v1/anchore/images", apiURL);

    JSONObject jsonBody = new JSONObject();
    jsonBody.put("tag", tag);
    if (null != dockerFileContents) {
      jsonBody.put("dockerfile", dockerFileContents);
    }
    if (null != annotations) {
      jsonBody.put("annotations", annotations);
    }

    try (CloseableHttpClient httpclient = makeHttpClient(verifySSL)) {
      String body = jsonBody.toString();

      HttpPost httppost = new HttpPost(imagesUrl);
      httppost.addHeader("Content-Type", "application/json");
      httppost.addHeader("Authorization", String.format("Bearer %s", token));
      httppost.setEntity(new StringEntity(body));

      logger.logDebug("Sending request: " + httppost.toString());
      logger.logDebug("Body:\n" + body);

      try (CloseableHttpResponse response = httpclient.execute(httppost)) {
        logger.logDebug("Response: " + response.getStatusLine().toString());
        logger.logDebug("Response body:\n" + EntityUtils.toString(response.getEntity()));

        int statusCode = response.getStatusLine().getStatusCode();

        if (statusCode != 200) {
          String serverMessage = EntityUtils.toString(response.getEntity());
          throw new ImageScanningException(String.format("sysdig-secure-engine add image failed. URL: %s, status: %s, error: %s", imagesUrl, response.getStatusLine(), serverMessage));
        }


        String responseBody = EntityUtils.toString(response.getEntity());
        return JSONObject.fromObject(JSONArray.fromObject(responseBody).get(0)).getString("imageDigest");
      }
    } catch (IOException e) {
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
        logger.logDebug("Response: " + response.getStatusLine().toString());
        logger.logDebug("Response body:\n" + EntityUtils.toString(response.getEntity()));

        if (response.getStatusLine().getStatusCode() != 200) {
          String responseStr = EntityUtils.toString(response.getEntity());
          throw new ImageScanningException(String.format("Error while retrieving the image vulnerabilities: %s", responseStr));
        }

        String responseBody = EntityUtils.toString(response.getEntity());
        return JSONObject.fromObject(responseBody);
      }
    } catch (IOException e) {
      logger.logDebug("Error: ", e);
      throw new ImageScanningException(e);
    }
  }

    @Override
  public JSONArray retrieveImageScanningResults(String tag, String imageDigest) throws ImageScanningException {
    String url = String.format("%s/api/scanning/v1/anchore/images/%s/check?tag=%s&detail=true", apiURL, imageDigest, tag);

    HttpGet httpget = new HttpGet(url);
    httpget.addHeader("Content-Type", "application/json");
    httpget.addHeader("Authorization", String.format("Bearer %s", token));

      logger.logDebug("Sending request: " + httpget.toString());

      try (CloseableHttpClient httpclient = makeHttpClient(verifySSL)) {
      try (CloseableHttpResponse response = httpclient.execute(httpget)) {
        logger.logDebug("Response: " + response.getStatusLine().toString());
        logger.logDebug("Response body:\n" + EntityUtils.toString(response.getEntity()));

        if (response.getStatusLine().getStatusCode() != 200) {
          String responseStr = EntityUtils.toString(response.getEntity());
          throw new ImageScanningException(String.format("Error while retrieving the image scanning results: %s", responseStr));
        }

        String responseBody = EntityUtils.toString(response.getEntity());
        return JSONArray.fromObject(responseBody);
      }
    } catch (IOException e) {
      logger.logDebug("Error: ", e);
      throw new ImageScanningException(e);
    }
  }

  @Override
  public String getScanningAccount() throws ImageScanningException {
    String url = String.format("%s/api/scanning/v1/account", apiURL);

    HttpGet httpget = new HttpGet(url);
    httpget.addHeader("Content-Type", "application/json");
    httpget.addHeader("Authorization", String.format("Bearer %s", token));

    try (CloseableHttpClient httpClient = makeHttpClient(verifySSL)) {
      try (CloseableHttpResponse response = httpClient.execute(httpget)) {
        String responseBody = EntityUtils.toString(response.getEntity());
        if (response.getStatusLine().getStatusCode() != 200) {
          throw new ImageScanningException(String.format("Unable to retrieve the Scanning Account: %s", responseBody));
        }

        JSONObject responseJSON = JSONObject.fromObject(responseBody);
        return responseJSON.getString("name");
      }
    } catch (IOException e) {
      throw new ImageScanningException(e);
    }
  }


  private static CloseableHttpClient makeHttpClient(boolean verify) {
    CloseableHttpClient httpclient = null;
    if (verify) {
      httpclient = HttpClients.createDefault();
    } else {
      try {
        SSLContextBuilder builder = new SSLContextBuilder();
        builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build(),
          NoopHostnameVerifier.INSTANCE);
        httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
      } catch (Exception e) {
        System.out.println(e.toString());
      }
    }
    return (httpclient);
  }
}
