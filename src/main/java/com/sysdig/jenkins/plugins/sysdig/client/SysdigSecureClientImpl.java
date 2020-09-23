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

import hudson.AbortException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

public class SysdigSecureClientImpl implements SysdigSecureClient {
  private final String token;
  private final String apiURL;
  private boolean verifySSL;

  private SysdigSecureClientImpl(String token, String apiURL) {
    this.token = token;
    this.apiURL = apiURL.replaceAll("/+$", "");
  }

  public static SysdigSecureClient newClient(String token, String apiURL) {
    SysdigSecureClientImpl client = new SysdigSecureClientImpl(token, apiURL);
    client.verifySSL = true;
    return client;
  }

  public static SysdigSecureClient newInsecureClient(String token, String apiURL) {
    SysdigSecureClientImpl client = new SysdigSecureClientImpl(token, apiURL);
    client.verifySSL = false;
    return client;
  }


  @Override
  public ImageScanningSubmission submitImageForScanning(String tag, String dockerFile) throws ImageScanningException {
    String imagesUrl = String.format("%s/api/scanning/v1/anchore/images", apiURL);

    JSONObject jsonBody = new JSONObject();
    jsonBody.put("tag", tag);
    if (null != dockerFile) {
      jsonBody.put("dockerfile", dockerFile);
    }

    try (CloseableHttpClient httpclient = makeHttpClient(verifySSL)) {
      String body = jsonBody.toString();

      HttpPost httppost = new HttpPost(imagesUrl);
      httppost.addHeader("Content-Type", "application/json");
      httppost.addHeader("Authorization", String.format("Bearer %s", token));
      httppost.setEntity(new StringEntity(body));

      try (CloseableHttpResponse response = httpclient.execute(httppost)) {
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
          String serverMessage = EntityUtils.toString(response.getEntity());
          throw new ImageScanningException(String.format("sysdig-secure-engine add image failed. URL: %s, status: %s, error: %s", imagesUrl, response.getStatusLine(), serverMessage));
        }

        // Read the response body.
        String responseBody = EntityUtils.toString(response.getEntity());
        String imageDigest = JSONObject.fromObject(JSONArray.fromObject(responseBody).get(0)).getString("imageDigest");
        return new ImageScanningSubmission(tag, imageDigest);
      }
    } catch (Exception e) {
      throw new ImageScanningException(e);
    }
  }

  @Override
  public ImageScanningSubmission submitImageForScanning(String imageID, String imageName, String imageDigest, File scanningResult) throws ImageScanningException {
    return submitImageForScanning(imageID, imageName, imageDigest, scanningResult, false);
  }

  private ImageScanningSubmission submitImageForScanning(String imageID, String imageName, String imageDigest, File scanningResult, boolean async) throws ImageScanningException {
    String url = async ?
      String.format("%s/api/scanning/v1/import/images", apiURL) :
      String.format("%s/api/scanning/v1/sync/import/images", apiURL);

    HttpPost httpPost = new HttpPost(url);
    httpPost.addHeader("Authorization", String.format("Bearer %s", token));
    httpPost.addHeader("imageId", imageID);
    httpPost.addHeader("digestId", imageDigest);
    httpPost.addHeader("imageName", imageName);

    MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
    multipartEntityBuilder.addBinaryBody("archive_file", scanningResult);

    HttpEntity build = multipartEntityBuilder.build();
    httpPost.setEntity(build);

    try (CloseableHttpClient httpClient = makeHttpClient(verifySSL)) {
      try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
        String responseStr = EntityUtils.toString(response.getEntity());
        if (response.getStatusLine().getStatusCode() == 404 && !async) {
          // If the endpoint doesn't exist, maybe the installation is older,
          // so we try again with the async one.
          return submitImageForScanning(imageID, imageName, imageDigest, scanningResult, true);
        }
        if (response.getStatusLine().getStatusCode() != 200) {
          throw new ImageScanningException(String.format("Error while pushing the image scanning results: %s", responseStr));
        }
      }
    } catch (IOException e) {
      throw new ImageScanningException(e);
    }
    return new ImageScanningSubmission(imageName, imageDigest);
  }

  @Override
  public Optional<ImageScanningResult> retrieveImageScanningResults(String tag, String imageDigest) throws ImageScanningException {
    String url = String.format("%s/api/scanning/v1/anchore/images/%s/check?tag=%s&detail=true", apiURL, imageDigest, tag);

    HttpGet httpget = new HttpGet(url);
    httpget.addHeader("Content-Type", "application/json");
    httpget.addHeader("Authorization", String.format("Bearer %s", token));

    try (CloseableHttpClient httpclient = makeHttpClient(verifySSL)) {
      try (CloseableHttpResponse response = httpclient.execute(httpget)) {
        if (response.getStatusLine().getStatusCode() != 200) {
          return Optional.empty();
        }

        // Read the response body.
        String responseBody = EntityUtils.toString(response.getEntity());
        JSONArray respJson = JSONArray.fromObject(responseBody);
        JSONObject tagEvalObj = JSONObject.fromObject(JSONObject.fromObject(respJson.get(0)).getJSONObject(imageDigest));
        JSONArray tagEvals = null;
        for (Object key : tagEvalObj.keySet()) {
          tagEvals = tagEvalObj.getJSONArray((String) key);
          break;
        }

        if (null == tagEvals) {
          throw new AbortException(String.format("Failed to analyze %s due to missing tag eval records in sysdig-secure-engine policy evaluation response", tag));
        }
        if (tagEvals.size() < 1) {
          return Optional.empty();
        }
        String evalStatus = tagEvals.getJSONObject(0).getString("status");
        JSONObject gateResult = tagEvals.getJSONObject(0).getJSONObject("detail").getJSONObject("result").getJSONObject("result");

        return Optional.of(new ImageScanningResult(evalStatus, gateResult));
      }
    } catch (Exception e) {
      throw new ImageScanningException(e);
    }
  }

  @Override
  public ImageScanningVulnerabilities retrieveImageScanningVulnerabilities(String tag, String imageDigest) throws ImageScanningException {
    try (CloseableHttpClient httpclient = makeHttpClient(verifySSL)) {

      String url = String.format("%s/api/scanning/v1/anchore/images/%s/vuln/all", apiURL, imageDigest);

      HttpGet httpget = new HttpGet(url);
      httpget.addHeader("Content-Type", "application/json");
      httpget.addHeader("Authorization", String.format("Bearer %s", token));

      JSONArray dataJson = new JSONArray();
      try (CloseableHttpResponse response = httpclient.execute(httpget)) {
        if (response.getStatusLine().getStatusCode() != 200) {
          String responseStr = EntityUtils.toString(response.getEntity());
          throw new ImageScanningException(String.format("Error while retrieving the image vulnerabilities: %s", responseStr));
        }

        String responseBody = EntityUtils.toString(response.getEntity());
        JSONObject responseJson = JSONObject.fromObject(responseBody);
        JSONArray vulList = responseJson.getJSONArray("vulnerabilities");
        for (int i = 0; i < vulList.size(); i++) {
          JSONObject vulnJson = vulList.getJSONObject(i);
          JSONArray vulnArray = new JSONArray();
          vulnArray.addAll(Arrays.asList(
            tag,
            vulnJson.getString("vuln"),
            vulnJson.getString("severity"),
            vulnJson.getString("package"),
            vulnJson.getString("fix"),
            String.format("<a href='%s'>%s</a>", vulnJson.getString("url"), vulnJson.getString("url"))));
          dataJson.add(vulnArray);
        }

        return new ImageScanningVulnerabilities(dataJson);
      }
    } catch (IOException e) {
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
