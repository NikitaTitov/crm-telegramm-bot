package crm.bot.telegramm;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HttpSessionHandler {

	private Map<String, String> authDetails;

	final static Logger logger = LogManager.getRootLogger();

	public HttpSessionHandler() {

	}

	public HttpSessionHandler(Map<String, String> authDetails) {

		this.authDetails = authDetails;

	}

	public void setAuthDetails(Map<String, String> authDetails) {

		this.authDetails = authDetails;
	}

	private HttpClient connectToServer() {
		HttpClient httpclient = HttpClients.createDefault();
		HttpPost httppost = new HttpPost("http://localhost:8080/processing-url");

		// Request parameters and other properties.
		List<NameValuePair> params = new ArrayList<>(2);

		params.add(new BasicNameValuePair("username", authDetails.get("username")));
		params.add(new BasicNameValuePair("password", authDetails.get("password")));
		try {
			httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
			httpclient.execute(httppost);
		} catch (UnsupportedEncodingException e) {
			logger.error("Problem with encoding: " + e.getLocalizedMessage());
		} catch (IOException e) {
			logger.error("IOException: " + e.getLocalizedMessage());
		}

		return httpclient;
	}

	public List<Board> getListOfBoards() {
		HttpClient httpClient = connectToServer();
		HttpGet httpGet = new HttpGet("http://localhost:8080/manager/rest/Table");
		ObjectMapper objectMapper = new ObjectMapper();
		List<Board> result = new ArrayList<>();

		try {
			HttpResponse response = httpClient.execute(httpGet);
			String json = EntityUtils.toString(response.getEntity(), "UTF-8");
			result = objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, Board.class));
		} catch (ClientProtocolException e) {
			logger.error("Problem with protocol: " + e.getLocalizedMessage());
		} catch (IOException e) {
			logger.error("IOException: " + e.getLocalizedMessage());
		}

		return result;
	}

	public String getStringOfBoards() {
		return getListOfBoards().stream().map(Object::toString).collect(Collectors.joining("\n"));
	}

	public Board getBoardById(String id) {
		return getListOfBoards().get(Integer.valueOf(id) - 1);
	}

	public void sendRequestOnAddTable(Map<String, String> tableDetails) {

		HttpClient httpClient = connectToServer();
		HttpPost httpPost = new HttpPost("http://localhost:8080/manager/add-calculate");

		List<NameValuePair> params = new ArrayList<>(2);

		params.add(new BasicNameValuePair("boardId", tableDetails.get("boardId")));
		params.add(new BasicNameValuePair("number", tableDetails.get("number")));
		params.add(new BasicNameValuePair("description", tableDetails.get("description")));

		try {
			httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
			HttpResponse response = httpClient.execute(httpPost);
			HttpEntity entity = response.getEntity();

			if (entity != null) {
				InputStream instream = entity.getContent();
				try {
					// do something useful
				} finally {
					instream.close();
				}
			}
		} catch (IOException e) {
			logger.error("IOException: " + e.getLocalizedMessage());
		}


	}



}