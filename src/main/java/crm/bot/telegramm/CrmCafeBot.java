package crm.bot.telegramm;


import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

			if (messageText.equalsIgnoreCase("/start")) {
				sendMessageWithText(chatId, "Вам нужно пройти аунтификацию при помощи команды: " + "\n" + "/auth");
				return;
			}

			if (messageText.equalsIgnoreCase("/Out")) {
				context.clear();
				authDetails.clear();
				tableDetails.clear();

				sendMessageWithText(chatId, "Все настройки сброшены чтобы началь работу авторизуйтесь: " + "\n" + "/auth");
				return;
			}

			if (messageText.equalsIgnoreCase("/Cancel")) {
				context.clear();
				tableDetails.clear();
				printMainMenu(chatId);
				return;
			}

			if (messageText.equalsIgnoreCase("/auth") && authDetails.containsKey("username") && authDetails.containsKey("password")) {
				sendMessageWithText(chatId, "Вы уже авторизованы, чтобы перезайти под другим пользователем наберите команду /Out");
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

			if (context.contains("/auth") && authDetails.containsKey("username") && !messageText.equalsIgnoreCase("/chooseTable")) {
				authDetails.put("password", messageText);
				sessionHandler.setAuthDetails(authDetails);
				if (sessionHandler.isLoginPasswordCorrect()) {
					printMainMenu(chatId);
					context.clear();
				} else {
					sendMessageWithText(chatId, "Вы ввели неправильный логин/пароль попробуйте заново при помощи команды /auth");
					authDetails.clear();
					context.clear();
				}
				return;
			}

			//если у нас есть пароль, логин и нас просят добавить стол, а так же мы еще не заходили в это меню
			if (authDetails.containsKey("username") &&
					authDetails.containsKey("password") &&
					messageText.equalsIgnoreCase("/chooseTable") &&
					!context.contains("/chooseTable")) {

				try {
					sendMessageWithText(chatId, "Выберите стол" + "\n" + sessionHandler.getStringOfBoards());
				} catch (IOException e) {
					sendMessageWithText(chatId, "Смена еще не началась");
					return;
				}
				context.add(messageText);
				return;
			}

			if (context.contains("/chooseTable") && !tableDetails.containsKey("boardId")) {
				try {
					sendMessageWithText(chatId, "Ок, сколько человек будет сидеть за столом " + sessionHandler.getBoardById(messageText) + "\n" + "/Cancel");
				} catch (IOException e) {
					sendMessageWithText(chatId, "Смена еще не началась");
					return;
				}
				tableDetails.put("boardId", messageText);
				return;
			}

			if (tableDetails.containsKey("boardId") &&
					!tableDetails.containsKey("number") &&
					!(messageText.equalsIgnoreCase("/Yes") ||
							messageText.equalsIgnoreCase("/No"))) {

				sendMessageWithText(chatId, "Хотите добавить описание стола?" + "\n" + "/Yes" + "\n" + "/No" + "\n" + "\n" + "/Cancel");
				tableDetails.put("number", messageText);
				return;
			}

			if (tableDetails.containsKey("boardId") &&
					tableDetails.containsKey("number") &&
					messageText.equalsIgnoreCase("/Yes")) {

				sendMessageWithText(chatId, "Введите описание стола");
				context.add("/Yes");
				return;
			}

			if (context.contains("/chooseTable") &&
					tableDetails.containsKey("boardId") &&
					tableDetails.containsKey("number") &&
					messageText.equalsIgnoreCase("/No")) {

				sendMessageWithText(chatId, "Хотите изменить время посадки?" + "\n" + "/Change" + "\n" + "/Stay" + "\n" + "\n" + "/Cancel");
				tableDetails.put("description", "");
				return;
			}

			if (context.contains("/chooseTable") &&
					tableDetails.containsKey("boardId") &&
					tableDetails.containsKey("number") &&
					context.contains("/Yes") &&
					!context.contains("/Change") &&
					!(messageText.equalsIgnoreCase("/Change") ||
							messageText.equalsIgnoreCase("/Stay"))) {

				sendMessageWithText(chatId, "Хотите изменить время посадки?" + "\n" + "/Change" + "\n" + "/Stay" + "\n" + "\n" + "/Cancel");
				tableDetails.put("description", messageText);
				return;
			}

			if (context.contains("/chooseTable") &&
					tableDetails.containsKey("boardId") &&
					tableDetails.containsKey("number") &&
					messageText.equalsIgnoreCase("/Change")) {

				sendMessageWithText(chatId, "Введите новое время в формате Часы:Минуты");
				context.add("/Change");
				return;
			}

			if (context.contains("/chooseTable") &&
					tableDetails.containsKey("boardId") &&
					tableDetails.containsKey("number") &&
					messageText.equalsIgnoreCase("/Stay")) {

				sessionHandler.sendRequestOnAddTable(tableDetails);
				sendMessageWithText(chatId, "Счёт добавлен в систему");
				//выходим из меню, но остаётся аунтификация
				context.clear();
				tableDetails.clear();
				printMainMenu(chatId);
				return;
			}

			if (context.contains("/chooseTable") &&
					tableDetails.containsKey("boardId") &&
					tableDetails.containsKey("number") &&
					context.contains("/Change")) {

				sessionHandler.sendRequestOnAddTable(tableDetails);
				tableDetails = parceInputTime(messageText, tableDetails);
				if (tableDetails.containsKey("hours") && tableDetails.containsKey("minutes")) {
					sessionHandler.sendEditClientTimeStart(tableDetails);
					sendMessageWithText(chatId, "Счёт добавлен в систему");
					//выходим из меню, но остаётся аунтификация
					context.clear();
					tableDetails.clear();
					printMainMenu(chatId);
				} else {
					sendMessageWithText(chatId, "Вы ввели время в неправильном формате или указали неправильное время попробуйте еще раз изменить время /Change");
					context.remove("/Change");
				}
			}
		}
	}

	private Map<String, String> parceInputTime(String hoursMinutes, Map<String, String> tableDetails) {

		Pattern pattern = Pattern.compile("\\d{2}:\\d{2}");
		Matcher matcher = pattern.matcher(hoursMinutes);
		if (matcher.find()) {
			String[] parceTime = matcher.group(0).split(":", 2);
			int hour = Integer.parseInt(parceTime[0]);
			int minutes = Integer.parseInt(parceTime[1]);

			if (hour < 24 && minutes < 60) {
				tableDetails.put("hours", parceTime[0]);
				tableDetails.put("minutes", parceTime[1]);
			}
			return tableDetails;
		} else {
			return tableDetails;
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

	private void printMainMenu(Long chatId) {

		sendMessageWithText(chatId, "Вы находитесь в главном меню" + "\n"
				+ "Вам доступны следующие команды: " + "\n"
				+ "/chooseTable - выберите стол для посадки клиентов" + "\n"
				+ "\n"
				+ "/Out - выход из системы");
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
