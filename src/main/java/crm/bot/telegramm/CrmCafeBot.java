package crm.bot.telegramm;


import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CrmCafeBot extends TelegramLongPollingBot {

	Map<Long, String> context = new HashMap<>();
	Map<String, String> authDetails = new HashMap<>();
	Map<String, String> tableDetails = new HashMap<>();
	HttpSessionHandler sessionHandler = new HttpSessionHandler();

	//в этом методе порядок строк влияет на работоспособность приложения.
	@Override
	public void onUpdateReceived(Update update) {

		if (update.hasMessage() && update.getMessage().hasText()) {

			String messageText = update.getMessage().getText();
			long chatId = update.getMessage().getChatId();

			if (messageText.equalsIgnoreCase("/auth") && authDetails.containsKey("username") && authDetails.containsKey("password")) {
				SendMessage message = new SendMessage()
						.setChatId(chatId)
						.setText("Вы уже авторизованы, чтобы перезайти под другим пользователем наберите команду /clean");
				try {
					sendMessage(message);
				} catch (TelegramApiException e) {
					e.printStackTrace();
				}
				return;
			}

			//если у нас нет записи о том что мы заходили в меню аунтификации
			if (!(context.containsValue("/auth")) && messageText.equalsIgnoreCase("/auth")) {
				SendMessage message = new SendMessage()
						.setChatId(chatId)
						.setText("Введите логин");
				context.put(chatId, messageText);
				try {
					sendMessage(message);
				} catch (TelegramApiException e) {
					e.printStackTrace();
				}
				return;
			}

			if (context.containsValue("/auth") && !authDetails.containsKey("username")) {
				SendMessage message = new SendMessage()
						.setChatId(chatId)
						.setText("Введите пароль");
				authDetails.put("username", messageText);
				try {
					sendMessage(message);
				} catch (TelegramApiException e) {
					e.printStackTrace();
				}
				return;
			}

			if (context.containsValue("/auth") && authDetails.containsKey("username")) {
				SendMessage message = new SendMessage()
						.setChatId(chatId)
						.setText("Сохранено, выберите стол /chooseTable");
				authDetails.put("password", messageText);
				sessionHandler.setAuthDetails(authDetails);
				//выходим из меню заполнения логина пароля
				context.clear();
				try {
					sendMessage(message);
				} catch (TelegramApiException e) {
					e.printStackTrace();
				}
				return;
			}

			//если у нас есть пароль, логин и нас просят добавить стол, а так же мы еще не заходили в это меню
			if (authDetails.containsKey("username") &&
					authDetails.containsKey("password") &&
					messageText.equalsIgnoreCase("/chooseTable") &&
					!context.containsValue("/chooseTable")) {

				SendMessage message = new SendMessage()
						.setChatId(chatId)
						.setText("Выберите стол" + "\n" + sessionHandler.getStringOfBoards());
				context.put(chatId, messageText);
				try {
					sendMessage(message);
				} catch (TelegramApiException e) {
					e.printStackTrace();
				}
			} else if (context.containsValue("/chooseTable") && !tableDetails.containsKey("boardId")) {
				SendMessage message = new SendMessage()
						.setChatId(chatId)
						.setText("Ок, сколько человек будет сидеть за столом " + sessionHandler.getBoardById(messageText));
				tableDetails.put("boardId", messageText);
				try {
					sendMessage(message);
				} catch (TelegramApiException e) {
					e.printStackTrace();
				}
			} else if (tableDetails.containsKey("boardId") && !tableDetails.containsKey("number")) {

				SendMessage message = new SendMessage()
						.setChatId(chatId)
						.setText("Введите описание стола(Оно может быть пустым)");
				tableDetails.put("number", messageText);
				try {
					sendMessage(message);
				} catch (TelegramApiException e) {
					e.printStackTrace();
				}
			} else if (context.containsValue("/chooseTable") &&
					tableDetails.containsKey("boardId") &&
					tableDetails.containsKey("number")) {

				SendMessage message = new SendMessage()
						.setChatId(chatId)
						.setText("Счёт добавлен в систему");
				tableDetails.put("description", messageText);
				try {
					sendMessage(message);
					sessionHandler.sendRequestOnAddTable(tableDetails);
				} catch (TelegramApiException | IOException e) {
					e.printStackTrace();
				}
				//выходим из меню, но остаётся аунтификация
				context.clear();
				tableDetails.clear();
			}

			if (messageText.equalsIgnoreCase("/clean")) {
				context.clear();
				authDetails.clear();
				tableDetails.clear();

				SendMessage message = new SendMessage()
						.setChatId(chatId)
						.setText("Все настройки сброшены");
				try {
					sendMessage(message);
				} catch (TelegramApiException e) {
					e.printStackTrace();
				}
			}
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
