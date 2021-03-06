package edu.pdx.cs410J.web;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A helper class that provides methods for requesting resources via HTTP
 */
public class HttpRequestHelper {

  /**
   * Performs an HTTP GET on the given URL
   *
   * @param urlString  The URL to get
   * @param parameters The key/value query parameters
   * @return A <code>Response</code> summarizing the result of the GET
   */
  protected Response get(String urlString, Map<String, String> parameters) throws IOException {
    StringBuilder query = encodeParameters(parameters);
    if (query.length() > 0) {
      query.insert(0, '?');
    }

    URL url = new URL(urlString + query);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    conn.setDoOutput(true);
    conn.setDoInput(true);

    return new Response(conn);

  }

  /**
   * Performs an HTTP POST to the given URL
   * @param urlString The URL to post to
   * @param parameters The key/value parameters
   * @return A <code>Response</code> summarizing the result of the POST
   */
  protected Response post(String urlString, Map<String, String> parameters) throws IOException {
    return sendEncodedRequest(urlString, "POST", parameters);
  }

  /**
   * Performs an HTTP DELETE to the given URL
   * @param urlString The URL to post to
   * @param parameters The key/value parameters
   * @return A <code>Response</code> summarizing the result of the POST
   */
  protected Response delete(String urlString, Map<String, String> parameters) throws IOException {
    return sendEncodedRequest(urlString, "DELETE", parameters);
  }

  private Response sendEncodedRequest(String urlString, String requestMethod, Map<String, String> parameters) throws IOException {
    StringBuilder data = encodeParameters(parameters);

    URL url = new URL(urlString);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod(requestMethod);
    conn.setDoOutput(true);

    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream(), Charset.forName("UTF-8"));
    wr.write(data.toString());
    wr.flush();

    Response response = new Response(conn);
    wr.close();

    return response;
  }

  /**
   * Encodes parameters to be sent to the server via an HTTP GET, POST, or DELETE
   * @param parameters The parameter key/value pairs
   * @return The encoded parameters
   * @throws java.io.UnsupportedEncodingException If we can't encode UTF-8
   */
  private StringBuilder encodeParameters(Map<String, String> parameters) throws UnsupportedEncodingException {
    StringBuilder query = new StringBuilder();
    for (Iterator<Map.Entry<String, String>> iter = parameters.entrySet().iterator(); iter.hasNext(); ) {
      Map.Entry<String, String> pair = iter.next();
      String key = pair.getKey();
      String value = pair.getValue();
      query.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
      query.append("=");
      query.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
      if (iter.hasNext()) {
        query.append("&");
      }
    }
    return query;
  }

  /**
   * Performs an HTTP PUT on the given URL
   *
   * @param urlString The URL to put to
   * @param parameters key/value parameters to the put
   * @return A <code>Response</code> summarizing the result of the PUT
   */
  protected Response put(String urlString, Map<String, String> parameters) throws IOException {
    StringBuilder data = new StringBuilder();
        parameters.forEach((key, value) -> {
          data.append(key);
          data.append("=");
          data.append(value);
          data.append("\n");
        });

    URL url = new URL(urlString);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("PUT");
    conn.setDoOutput(true);
    conn.setDoInput(true);
    conn.setRequestProperty("Content-Type", "text/plain");
    conn.setRequestProperty("Context-Length", String.valueOf(data.length()));

    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream(), Charset.forName("UTF-8"));
    wr.write(data.toString());
    wr.flush();

    Response response = new Response(conn);
    wr.close();

    return response;
  }

  /**
   * Encapsulates a response to an HTTP request
   */
  public static class Response {

    private final int code;

    private final String content;

    private int contentLines = 0;

    private Response(HttpURLConnection conn) throws IOException {
      this.code = conn.getResponseCode();
      if (this.code != java.net.HttpURLConnection.HTTP_OK) {
        this.content = conn.getResponseMessage();
        return;
      }

      StringBuilder content = new StringBuilder();
      BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      String line;
      while ((line = rd.readLine()) != null) {
        content.append(line);
        content.append("\n");
        contentLines++;
      }
      rd.close();

      this.content = content.toString().trim();
    }

    /**
     * Returns the HTTP status code of the response
     * @see java.net.HttpURLConnection#getResponseCode()
     */
    public int getCode() {
      return code;
    }

    /**
     * Returns the (presumably textual) response from the URL
     */
    public String getContent() {
      return content;
    }

    /**
     * Returns the number of lines in the response's content
     */
    public int getContentLines() {
      return contentLines;
    }
  }


  private static Map<String, String> arrayToMap(String[] parameters) {
    Map<String, String> params = new HashMap<>();
    for (int i = 0; i < parameters.length; i++) {
      String key = parameters[i];
      i++;
      String value = parameters[i];
      params.put(key, value);
    }
    return params;
  }

  /**
   * A main method that requests a resource from a URL using a given HTTP method
   */
  public static void main(String[] args) throws IOException {
    String method = args[0];
    String url = args[1];
    String[] parameters = new String[args.length - 2];
    System.arraycopy(args, 2, parameters, 0, parameters.length);
    Map<String, String> map = HttpRequestHelper.arrayToMap(parameters);

    HttpRequestHelper helper = new HttpRequestHelper();

    Response response;
    if (method.equalsIgnoreCase("PUT")) {
      response = helper.put(url, map);

    } else if (method.equalsIgnoreCase("GET")) {
      response = helper.get(url, map);

    } else if (method.equalsIgnoreCase("POST")) {
      response = helper.post(url, map);

    } else {
      System.err.println("** Unknown method: " + method);
      return;
    }

    System.out.println("Returned code " + response.getCode() + " and " +
      response.getContentLines() + " lines of content\n");
    System.out.println(response.getContent());

  }

  public class RestException extends RuntimeException {

    private final int httpStatusCode;

    public RestException(int httpStatusCode, String message) {
      super("Got an HTTP Status Code of " + httpStatusCode + ": " + message);

      this.httpStatusCode = httpStatusCode;
    }

    public int getHttpStatusCode() {
      return this.httpStatusCode;
    }
  }
}
