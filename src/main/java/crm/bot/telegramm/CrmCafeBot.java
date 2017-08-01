package crm.bot.telegramm;


import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CrmCafeBot extends TelegramLongPollingBot {

	private List<String> context = new ArrayList<>();
	private Map<String, String> authDetails = new HashMap<>();
	private Map<String, String> tableDetails = new HashMap<>();
	private HttpSessionHandler sessionHandler = new HttpSessionHandler();

	//в этом методе порядок строк влияет на работоспособность приложения.
	@Override
	public void onUpdateReceived(Update update) {

		if (update.hasMessage() && update.getMessage().hasText()) {

			String messageText = update.getMessage().getText();
			long chatId = update.getMessage().getChatId();

			if (messageText.equalsIgnoreCase("/Out")) {
				context.clear();
				authDetails.clear();
				tableDetails.clear();

				sendMessageWithText(chatId, "Все настройки сброшены");
			}

			if (messageText.equalsIgnoreCase("/Cancel")) {
				context.clear();
				tableDetails.clear();
				messageText = "/chooseTable";
			}

			if (messageText.equalsIgnoreCase("/auth") && authDetails.containsKey("username") && authDetails.containsKey("password")) {
				sendMessageWithText(chatId, "Вы уже авторизованы, чтобы перезайти под другим пользователем наберите команду /clean");
				return;
			}

			//если у нас нет записи о том что мы заходили в меню аунтификации
			if (!(context.contains("/auth")) && messageText.equalsIgnoreCase("/auth")) {
				sendMessageWithText(chatId, "Введите логин");
				context.add(messageText);
				return;
			}

			if (context.contains("/auth") && !authDetails.containsKey("username")) {
				sendMessageWithText(chatId, "Введите пароль");
				authDetails.put("username", messageText);
				return;
			}

			if (context.contains("/auth") && authDetails.containsKey("username")) {
				sendMessageWithText(chatId, "Сохранено, выберите стол " + "\n" + "/chooseTable");
				authDetails.put("password", messageText);
				sessionHandler.setAuthDetails(authDetails);
				//выходим из меню заполнения логина пароля
				context.clear();
				return;
			}

			//если у нас есть пароль, логин и нас просят добавить стол, а так же мы еще не заходили в это меню
			if (authDetails.containsKey("username") &&
					authDetails.containsKey("password") &&
					messageText.equalsIgnoreCase("/chooseTable") &&
					!context.contains("/chooseTable")) {

				sendMessageWithText(chatId, "Выберите стол" + "\n" + sessionHandler.getStringOfBoards());
				context.add(messageText);

			} else if (context.contains("/chooseTable") && !tableDetails.containsKey("boardId")) {
				sendMessageWithText(chatId, "Ок, сколько человек будет сидеть за столом " + sessionHandler.getBoardById(messageText));
				sendMessageWithText(chatId, "/Cancel");
				tableDetails.put("boardId", messageText);

			} else if (tableDetails.containsKey("boardId") && !tableDetails.containsKey("number") && !(messageText.equalsIgnoreCase("/Yes") || messageText.equalsIgnoreCase("/No"))) {
				sendMessageWithText(chatId, "Хотите добавить описание стола?" + "\n" + "/Yes" + "\n" + "/No");
				tableDetails.put("number", messageText);

			} else if (tableDetails.containsKey("boardId") &&
					tableDetails.containsKey("number") &&
					messageText.equalsIgnoreCase("/Yes")) {

				sendMessageWithText(chatId, "Введите описание стола");
				context.add("/Yes");

			} else if (context.contains("/chooseTable") &&
					tableDetails.containsKey("boardId") &&
					tableDetails.containsKey("number") &&
					messageText.equalsIgnoreCase("/No")) {

				sendMessageWithText(chatId, "Счёт добавлен в систему");

				tableDetails.put("description", "");
				sessionHandler.sendRequestOnAddTable(tableDetails);
				//выходим из меню, но остаётся аунтификация
				context.clear();
				tableDetails.clear();
			} else if (context.contains("/chooseTable") &&
					tableDetails.containsKey("boardId") &&
					tableDetails.containsKey("number") &&
					context.contains("/Yes")) {

				sendMessageWithText(chatId, "Счёт добавлен в систему");

				tableDetails.put("description", "");
				sessionHandler.sendRequestOnAddTable(tableDetails);
				//выходим из меню, но остаётся аунтификация
				context.clear();
				tableDetails.clear();
			}


		}
	}

	private void sendMessageWithText(Long chatId, String text) {

		SendMessage message = new SendMessage()
				.setChatId(chatId)
				.setText(text);
		try {
			sendMessage(message);
		} catch (TelegramApiException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getBotUsername() {

		return "CAFEcrmBot";
	}

	@Override
	public String getBotToken() {

		return "372169972:AAHGBe36HPmH3vRo07UmwTvjU0Ho0wTOS8s";
	}
}
