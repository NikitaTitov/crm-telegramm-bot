package crm.bot.telegramm.handlers;


import com.fasterxml.jackson.databind.ObjectMapper;
import crm.bot.telegramm.model.*;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.stream.Collectors;


public class HttpSessionHandler {

    private Map<String, String> authDetails;
    private final static Logger logger = LogManager.getRootLogger();
    private static Set<Calculate> calculates = new HashSet<>();
    private static List<Category> categories = new ArrayList<>();
    private URLProperties initProperty = new URLProperties();

    public HttpSessionHandler() {
    }

    public void setAuthDetails(Map<String, String> authDetails) {
        this.authDetails = authDetails;
    }

    public User hasUserWithThisLoginOnServer() {
        User user = new User();
        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost(initProperty.checkAuthUrl);
        ObjectMapper objectMapper = new ObjectMapper();
        List<NameValuePair> params = new ArrayList<>(2);

        params.add(new BasicNameValuePair("username", authDetails.get("username")));
        params.add(new BasicNameValuePair("password", authDetails.get("password")));

        try {
            httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            HttpResponse response = httpclient.execute(httppost);
            String json = EntityUtils.toString(response.getEntity(), "UTF-8");
            user = objectMapper.readValue(json, User.class);
        } catch (UnsupportedEncodingException e) {
            logger.error("Problem with encoding: " + e.getLocalizedMessage());
        } catch (IOException e) {
            logger.error("IOException: " + e.getLocalizedMessage());
        }
        return user;
    }

    public List<Board> getListOfBoards() throws IOException {
        HttpClient httpClient = connectToServer();
        HttpPost httpPost = new HttpPost(initProperty.tableListUrl);
        ObjectMapper objectMapper = new ObjectMapper();
        List<Board> result = new ArrayList<>();
        List<NameValuePair> params = new ArrayList<>(1);

        params.add(new BasicNameValuePair("username", authDetails.get("username")));

        try {
            httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            HttpResponse response = httpClient.execute(httpPost);
            String json = EntityUtils.toString(response.getEntity(), "UTF-8");
            if (Objects.equals(json, "null")) {
                throw new IOException();
            }
            result = objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, Board.class));

        } catch (ClientProtocolException e) {
            logger.error("Problem with protocol: " + e.getLocalizedMessage());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public Set<Calculate> getCalculateList() {
        calculates = (HashSet) sendGetRequest(calculates, new HttpGet(initProperty.calculateList), Calculate.class);
        calculates = calculates.stream().filter(calculate -> !(calculate.getDescription().isEmpty() ||  calculate.getDescription() == null)).filter(Calculate::isState).collect(Collectors.toSet());
        return calculates;
    }

    @SuppressWarnings("unchecked")
    public List<Category> getCategoryList() {
        categories = (ArrayList) sendGetRequest(categories, new HttpGet(initProperty.categoryList), Category.class);
        return categories;
    }

    public List<String> getBoardListName() throws IOException {
        return getListOfBoards().stream().map(Board::toString).collect(Collectors.toList());
    }

    public List<String> getBoardListId() throws IOException {
        return getListOfBoards().stream().map(Board::getName).collect(Collectors.toList());
    }

    public Board getBoardByName(String name) throws IOException {
        for (Board board : getListOfBoards()) {
            if (board.getName().equals(name)) {
                return board;
            }
        }
        throw new IOException("There is no table with " + name + " name");
    }

    public String getBoardId(String name) throws IOException {
        for (Board board : getListOfBoards()) {
            if (board.getName().equals(name)) {
                return board.getId().toString();
            }
        }
        throw new IOException("There is no table with " + name + " name");
    }

    public void sendRequestOnAddTable(Map<String, String> tableDetails) {
        List<NameValuePair> params = new ArrayList<>(2);

        params.add(new BasicNameValuePair("boardId", tableDetails.get("boardId")));
        params.add(new BasicNameValuePair("number", tableDetails.get("number")));
        params.add(new BasicNameValuePair("description", tableDetails.get("description")));

        sendPostRequest(params, new HttpPost(initProperty.addTableUrl));
    }

    public void sendRequestOnAddProductToClient (Map<String, String> tableDetails) {
        List<NameValuePair> params = getAddProductParam(tableDetails);
        sendPostRequest(params, new HttpPost(initProperty.addProductToTable));
    }

    public void sendRequestOnAddProductWithFloatingPriceToClient (Map<String, String> tableDetails) {
        List<NameValuePair> params = getAddProductParam(tableDetails);
		params.add(new BasicNameValuePair("productPrice", tableDetails.get("productPrice")));

        sendPostRequest(params, new HttpPost(initProperty.addProductWithFloatingPriceToTable));
    }

    private void sendPostRequest(List<NameValuePair> params, HttpPost httpPost) {
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            HttpResponse response = connectToServer().execute(httpPost);
            HttpEntity entity = response.getEntity();
            System.out.println(response.getStatusLine().getReasonPhrase() + " " + response.getStatusLine().getStatusCode());
            EntityUtils.consume(entity);
        } catch (IOException e) {
            logger.error("IOException: " + e.getLocalizedMessage());
        }
    }

    public void sendEditClientTimeStart(Map<String, String> tableDetails) {
        HttpClient httpClient = connectToServer();
        HttpPost httpPost = new HttpPost(initProperty.changeTimeUrl);

        //Этот метод вызывается после отправки клиентов на сервер и затем меняет время, поэтому getClientLastId возвращает последний ID учитывая новые
        int nextIdAfterLastClientId = getClientLastId() + 1;
        int amountOfNewClients = Integer.parseInt(tableDetails.get("number"));
        int nextPositionAfterOldClientId = nextIdAfterLastClientId - amountOfNewClients;//

        for (int i = nextPositionAfterOldClientId; i < nextIdAfterLastClientId; ++i) {
            List<NameValuePair> params = new ArrayList<>(2);
            String clientId = String.valueOf(i);

            params.add(new BasicNameValuePair("clientId", clientId));
            params.add(new BasicNameValuePair("hours", tableDetails.get("hours")));
            params.add(new BasicNameValuePair("minutes", tableDetails.get("minutes")));

            try {
                httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
                EntityUtils.consume(httpClient.execute(httpPost).getEntity());//release connection
            } catch (ClientProtocolException e) {
                logger.error("Problem with protocol: " + e.getLocalizedMessage());
            } catch (IOException e) {
                logger.error("IOException: " + e.getLocalizedMessage());
            }
        }
    }

    private int getClientLastId() {
        int lastId = 0;
        HttpClient httpClient = connectToServer();
        HttpGet httpGet = new HttpGet(initProperty.getClientLastIdUrl);

        try {
            HttpResponse response = httpClient.execute(httpGet);
            lastId = Integer.parseInt(EntityUtils.toString(response.getEntity(), "UTF-8"));
        } catch (ClientProtocolException e) {
            logger.error("Problem with protocol: " + e.getLocalizedMessage());
        } catch (IOException e) {
            logger.error("IOException: " + e.getLocalizedMessage());
        }
        return lastId;
    }

	private List<NameValuePair> getAddProductParam(Map<String, String> tableDetails) {
		List<NameValuePair> params = new ArrayList<>(4);
		if (tableDetails.get("clientsId").length() > 1) {
			String[] ids = tableDetails.get("clientsId").replaceAll("]|\\[", "").split(",");
			for (String id : ids) {
				params.add(new BasicNameValuePair("clientsId", String.valueOf(id)));
			}
		} else {
			params.add(new BasicNameValuePair("clientsId", tableDetails.get("clientsId")));
		}
		params.add(new BasicNameValuePair("calculateId", tableDetails.get("calculateId")));
		params.add(new BasicNameValuePair("productId", tableDetails.get("productId")));

		return params;
	}

    private <T> Collection<T> sendGetRequest(Collection<T> collection, HttpGet httpGet, Class<T> typeParameterClass) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            HttpResponse response = connectToServer().execute(httpGet);
            String json = EntityUtils.toString(response.getEntity(), "UTF-8");
            if (Objects.equals(json, "null")) {
                throw new IOException();
            }
            collection = objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(collection.getClass(), typeParameterClass));
        } catch (ClientProtocolException e) {
            logger.error("Problem with protocol: " + e.getLocalizedMessage());
        } catch (IOException e) {
            logger.error("IOException: " + e.getLocalizedMessage());
        }
        return collection;
    }

    private HttpClient connectToServer() {
        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost(initProperty.authUrl);
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
}