package crm.bot.telegramm;


import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiException;

public class Main {

	public static String prefix = "http://localhost:8080";

	public static void main(String[] args) {

		if (args.length > 0) {
			if (args[0].startsWith("crm-port:")){
				String port = args[0].substring(9, 13);
				prefix = "http://localhost:" + port;
				System.out.println("Current crm-port: " + port);
			} else {
				System.out.println("Incorrect port argument.");
				System.out.println("An example of a valid argument: \"crm-port:8090\"");
			}

		} else {
			System.out.println("Default crm-port: 8080.");
			System.out.println("If you want to change the port - pass it in the arguments. For example: java -jar crm-bot.jar crm-port:8090");
		}

		ApiContextInitializer.init();

		TelegramBotsApi botsApi = new TelegramBotsApi();

		try {
			botsApi.registerBot(new CrmCafeBot());
			System.out.println("Server started");
		} catch (TelegramApiException e) {
			e.printStackTrace();
		}
	}
}
