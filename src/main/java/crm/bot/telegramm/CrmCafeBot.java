package crm.bot.telegramm;


import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

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
				sendMessageWithText(chatId, "Вы уже авторизованы, чтобы перезайти под другим пользователем наберите команду /clean");
				return;
			}

			//если у нас нет записи о том что мы заходили в меню аунтификации
			if (!(context.containsValue("/auth")) && messageText.equalsIgnoreCase("/auth")) {
				sendMessageWithText(chatId, "Введите логин");
				context.put(chatId, messageText);
				return;
			}

			if (context.containsValue("/auth") && !authDetails.containsKey("username")) {
				sendMessageWithText(chatId, "Введите пароль");
				authDetails.put("username", messageText);
				return;
			}

			if (context.containsValue("/auth") && authDetails.containsKey("username")) {
				sendMessageWithText(chatId, "Сохранено, выберите стол /chooseTable");
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
					!context.containsValue("/chooseTable")) {

				sendMessageWithText(chatId, "Выберите стол" + "\n" + sessionHandler.getStringOfBoards());
				context.put(chatId, messageText);

			} else if (context.containsValue("/chooseTable") && !tableDetails.containsKey("boardId")) {
				sendMessageWithText(chatId, "Ок, сколько человек будет сидеть за столом " + sessionHandler.getBoardById(messageText));
				tableDetails.put("boardId", messageText);

			} else if (tableDetails.containsKey("boardId") && !tableDetails.containsKey("number")) {
				sendMessageWithText(chatId, "Введите описание стола(Оно не может быть пустым)");
				tableDetails.put("number", messageText);

			} else if (context.containsValue("/chooseTable") &&
					tableDetails.containsKey("boardId") &&
					tableDetails.containsKey("number")) {

				sendMessageWithText(chatId, "Счёт добавлен в систему");

				tableDetails.put("description", messageText);
				sessionHandler.sendRequestOnAddTable(tableDetails);
				//выходим из меню, но остаётся аунтификация
				context.clear();
				tableDetails.clear();
			}

			if (messageText.equalsIgnoreCase("/clean")) {
				context.clear();
				authDetails.clear();
				tableDetails.clear();

				sendMessageWithText(chatId, "Все настройки сброшены");
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
