package crm.bot.telegramm.handlers;


import crm.bot.telegramm.Main;
;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class URLProperties {

	public String checkAuthUrl;
	public String authUrl;
	public String tableListUrl;
	public String addTableUrl;
	public String changeTimeUrl;
	public String getClientLastIdUrl;

	public URLProperties() {
		initProperty();
	}

	public void initProperty() {

		Properties prop = new Properties();
		Map<String, String> property = new HashMap<>();
		String prefix = Main.prefix;
		try (InputStream input = ClassLoader.getSystemResourceAsStream("config.properties")) {
			prop.load(input);

			checkAuthUrl = prefix + prop.getProperty("checkAuth");
			authUrl = prefix + prop.getProperty("auth");
			tableListUrl = prefix + prop.getProperty("tableList");
			addTableUrl = prefix + prop.getProperty("addTable");
			changeTimeUrl = prefix + prop.getProperty("changeTime");
			getClientLastIdUrl = prefix + prop.getProperty("lastClientId");
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}
