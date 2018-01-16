package crm.bot.telegramm;


import crm.bot.telegramm.handlers.HttpSessionHandler;
import crm.bot.telegramm.model.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static crm.bot.telegramm.handlers.ButtonsHandler.*;
import static crm.bot.telegramm.handlers.UserHandler.isEmptyUser;
import static crm.bot.telegramm.utils.NameAndIDGetters.*;
import static crm.bot.telegramm.utils.Validators.*;

public class CrmCafeBot extends TelegramLongPollingBot {

    private List<String> context = new ArrayList<>();
    private Map<String, String> authDetails = new HashMap<>();
    private Map<String, String> tableDetails = new HashMap<>();

    private HttpSessionHandler sessionHandler = new HttpSessionHandler();
    private final static Logger logger = LogManager.getRootLogger();

    //в этом методе порядок строк влияет на работоспособность приложения.
    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {

            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageText.equalsIgnoreCase("/start")) {
                printHelloOnStart(chatId);
                return;
            }

            if (context.contains("/auth") && !authDetails.containsKey("username")) {
                sendMessageWithText(chatId, "Введите пароль");
                authDetails.put("username", messageText);
                return;
            }

            if (context.contains("/auth") && authDetails.containsKey("username")) {
                authDetails.put("password", messageText);
                sessionHandler.setAuthDetails(authDetails);
                User cafeUser = sessionHandler.hasUserWithThisLoginOnServer();
                if (isEmptyUser(cafeUser)) {
                    sendMessageWithText(chatId, cafeUser.getFirstName() + ", здравствуйте!");
                    printMainMenu(chatId);
                    context.clear();
                } else {
                    sendMessageWithTextAndInlineKeyboard(chatId, "Вы ввели неправильный логин/пароль попробуйте войти заново \n", AUTH_BUTTON);
                    authDetails.clear();
                    context.clear();
                }
                return;
            }

            if (tableDetails.containsKey("boardId") &&
                    !tableDetails.containsKey("number")) {
                if (correctValueOfPeople(messageText)) {
                    sendMessageWithTextAndInlineKeyboard(chatId, "Добавьте описание стола \n", CANCEL_BUTTON);
                    tableDetails.put("number", messageText);
                } else {
                    sendMessageWithTextAndInlineKeyboard(chatId, "Вы ввели не корректное число попробуйте еще раз");
                }
                return;
            }

            if (context.contains("/chooseTable") &&
                    tableDetails.containsKey("boardId") &&
                    tableDetails.containsKey("number") &&
                    !context.contains("/change")) {

                sendMessageWithTextAndInlineKeyboard(chatId, "Хотите изменить время посадки? \n", CHANGE_SEAT_TIME_BUTTON, LEAVE_SEAT_TIME_BUTTON, CANCEL_BUTTON);
                tableDetails.put("description", messageText);
                return;
            }


            if (context.contains("/chooseTable") &&
                    tableDetails.containsKey("boardId") &&
                    tableDetails.containsKey("number") &&
                    context.contains("/change")) {

                tableDetails = parceInputTime(messageText, tableDetails);
                if (tableDetails.containsKey("hours") && tableDetails.containsKey("minutes")) {
                    sessionHandler.sendRequestOnAddTable(tableDetails);
                    sessionHandler.sendEditClientTimeStart(tableDetails);
                    sendMessageWithText(chatId, "Счёт добавлен в систему");
                    //выходим из меню, но остаётся аунтификация
                    context.clear();
                    tableDetails.clear();
                    printMainMenu(chatId);
                } else {
                    sendMessageWithTextAndInlineKeyboard(chatId, "Вы ввели время в неправильном формате или указали неправильное время попробуйте еще раз изменить время \n", CHANGE_SEAT_TIME_BUTTON);
                    context.remove("/change");
                }
            }

			if (context.contains("/menu") && tableDetails.containsKey("calculateId") && tableDetails.containsKey("clientsId") && tableDetails.containsKey("categoryId") && tableDetails.containsKey("productId")) {
				if (isValidPrice(messageText)) {
					tableDetails.put("productPrice", messageText);
					sessionHandler.sendRequestOnAddProductWithFloatingPriceToClient(tableDetails);
					sendMessageWithText(chatId, "Заказ ушел на стол");
					//выходим из меню, но остаётся аунтификация
					context.clear();
					tableDetails.clear();
					printMainMenu(chatId);
				} else {
					sendMessageWithTextAndInlineKeyboard(chatId, "Вы ввели некорректное число или слишком большую стоимость, попробуйте еще раз или вернитесь в меню\n", CANCEL_BUTTON);
				}
			}
        } else if (update.hasCallbackQuery()) {

            String callData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callData.equals("/auth")) {
                sendMessageWithText(chatId, "Введите логин");
                context.add(callData);
                return;
            }
            if (callData.equals("/out")) {
                context.clear();
                authDetails.clear();
                tableDetails.clear();
                printOut(chatId);
                return;
            }
            if (callData.equals("/cancel")) {
                context.clear();
                tableDetails.clear();
                printMainMenu(chatId);
                return;
            }
            if (callData.equals("/auth") && authDetails.containsKey("username") && authDetails.containsKey("password")) {
                sendMessageWithTextAndInlineKeyboard(chatId, "Вы уже авторизованы, выйдите из системы чтобы зайти под другим пользователем \n", OUT_BUTTON);
                return;
            }
            //если у нас нет записи о том что мы заходили в меню аунтификации
            if (!(context.contains("/auth")) && callData.equals("/auth")) {
                sendMessageWithText(chatId, "Введите логин");
                context.add(callData);
                return;
            }

            //если у нас есть пароль, логин и нас просят добавить стол, а так же мы еще не заходили в это меню
            if (authDetails.containsKey("username") &&
                    authDetails.containsKey("password") &&
                    callData.equals("/chooseTable") &&
                    !context.contains("/chooseTable")) {
                try {
                    List<String> boardList = sessionHandler.getBoardListName();
                    if (!boardList.isEmpty()) {
                        sendMessageWithTextAndInlineKeyboard(chatId, "Выберите стол \n", boardList, sessionHandler.getBoardListId());
                    } else {
                        sendMessageWithTextAndInlineKeyboard(chatId, "В кафе нет созданных столов!\n", CANCEL_BUTTON);
                        return;
                    }
                } catch (IOException e) {
                    sendMessageWithText(chatId, "Смена еще не началась");
                    return;
                }
                context.add(callData);
                return;
            }

            if (callData.contains("/menu")) {
                    Set<Calculate> calculates = sessionHandler.getCalculateList();
                    if (!calculates.isEmpty()) {
                        sendMessageWithTextAndInlineKeyboard(chatId, "Выберите счёт \n", getListCalculateNames(calculates), getCalculateListId(calculates));
                    } else {
                        sendMessageWithTextAndInlineKeyboard(chatId, "В кафе нет созданных счетов!\n", CANCEL_BUTTON);
                    }
                context.add(callData);
                return;
            }

            if (context.contains("/menu") && !tableDetails.containsKey("calculateId")) {
                try {
                    Set<Calculate> calculates = sessionHandler.getCalculateList();
                    List<Client> clientsWithNotEmptyDescription = getClientsFromCalculateWithNotEmptyDescription(callData, calculates);
                    List<Client> allClietns = getClientsFromCalculate(callData, calculates);

                    if (!clientsWithNotEmptyDescription.isEmpty()) {
                        sendMessageWithTextAndInlineKeyboard(chatId, "Ок, какому клиенту хотите добавить заказ?\n", getListClientsNames(clientsWithNotEmptyDescription), getListClientsId(clientsWithNotEmptyDescription));
                        sendMessageWithTextAndInlineKeyboard(chatId, "Или добавить всем?\n", TO_ALL_CLIENTS_BUTTON.setCallbackData(getListClientsId(allClietns).toString()));
                    } else {
                        tableDetails.put("calculateId", callData);
                        tableDetails.put("clientsId", getListClientsId(allClietns).toString());
                        List<Category> categories = sessionHandler.getCategoryList();
                        if (!categories.isEmpty()) {
                            sendMessageWithTextAndInlineKeyboard(chatId, "Выберите категорию продуктов \n", getListCategoryNames(categories), getListCategoryId(categories));
                        } else {
                            sendMessageWithTextAndInlineKeyboard(chatId, "В кафе нет созданных категорий!\n", CANCEL_BUTTON);
                        }
                        return;
                    }

                    tableDetails.put("calculateId", callData);
                } catch (IOException e) {
                    sendMessageWithText(chatId, "Смена еще не началась");
                }
                return;
            }

            if (context.contains("/menu") && tableDetails.containsKey("calculateId") && !tableDetails.containsKey("clientsId")) {
                    List<Category> categories = sessionHandler.getCategoryList();
                    if (!categories.isEmpty()) {
                        sendMessageWithTextAndInlineKeyboard(chatId, "Выберите категорию продуктов \n", getListCategoryNames(categories), getListCategoryId(categories));
                        tableDetails.put("clientsId", callData);
                    } else {
                        sendMessageWithTextAndInlineKeyboard(chatId, "В кафе нет созданных категорий!\n", CANCEL_BUTTON);
                    }
               return;
            }

            if (context.contains("/menu") && tableDetails.containsKey("calculateId") && tableDetails.containsKey("clientsId") && !tableDetails.containsKey("categoryId")) {
                try {
                    List<Category> categories = sessionHandler.getCategoryList();
                    List<Product> products = getListProductsByCategoryId(callData, categories);
                    sendMessageWithTextAndInlineKeyboard(chatId, "Выберите продукт для добавления \n", getListProductNames(products), getListProductId(products));
                    tableDetails.put("categoryId", callData);
                } catch (IOException e) {
                    sendMessageWithTextAndInlineKeyboard(chatId, "В кафе нет созданных продуктов!\n", CANCEL_BUTTON);
                }
                return;
            }

            if (context.contains("/menu") && tableDetails.containsKey("calculateId") && tableDetails.containsKey("clientsId") && tableDetails.containsKey("categoryId") && !tableDetails.containsKey("productId")) {
				try {
					List<Category> categories = sessionHandler.getCategoryList();
					Category category = getCategory(tableDetails.get("categoryId"), categories);
					tableDetails.put("productId", callData);
					if (category.isFloatingPrice()) {
						sendMessageWithText(chatId, "Введите цену");
					} else {
						sessionHandler.sendRequestOnAddProductToClient(tableDetails);
						sendMessageWithText(chatId, "Заказ ушел на стол");
						//выходим из меню, но остаётся аунтификация
						context.clear();
						tableDetails.clear();
						printMainMenu(chatId);
					}
				} catch (IOException e) {
					sendMessageWithTextAndInlineKeyboard(chatId, "Категория была удалена перед отправкой заказа!\n", CANCEL_BUTTON);
				}
                return;
            }

            if (context.contains("/chooseTable") && !tableDetails.containsKey("boardId")) {
                try {
                    sendMessageWithTextAndInlineKeyboard(chatId, "Ок, сколько человек будет сидеть за столом " + sessionHandler.getBoardByName(callData) + "\n", CANCEL_BUTTON);
                    tableDetails.put("boardId", sessionHandler.getBoardId(callData));
                } catch (IOException e) {
                    sendMessageWithText(chatId, "Смена еще не началась");
                    return;
                }
                return;
            }

            /*if (tableDetails.containsKey("boardId") &&
                    tableDetails.containsKey("number") &&
                    callData.equals("/yes")) {

                sendMessageWithTextAndInlineKeyboard(chatId, "Введите описание стола", CANCEL_BUTTON);
                context.add("/yes");
                return;
            }*/

            /*if (context.contains("/chooseTable") &&
                    tableDetails.containsKey("boardId") &&
                    tableDetails.containsKey("number") &&
                    callData.equals("/no")) {

                sendMessageWithTextAndInlineKeyboard(chatId, "Хотите изменить время посадки? \n", CHANGE_SEAT_TIME_BUTTON, LEAVE_SEAT_TIME_BUTTON, CANCEL_BUTTON);
                tableDetails.put("description", "");
                return;
            }*/

            if (context.contains("/chooseTable") &&
                    tableDetails.containsKey("boardId") &&
                    tableDetails.containsKey("number") &&
                    callData.equals("/change")) {

                sendMessageWithText(chatId, "Введите новое время в формате Часы:Минуты");
                context.add("/change");
                return;
            }

            if (context.contains("/chooseTable") &&
                    tableDetails.containsKey("boardId") &&
                    tableDetails.containsKey("number") &&
                    callData.equalsIgnoreCase("/leave")) {

                sessionHandler.sendRequestOnAddTable(tableDetails);
                sendMessageWithText(chatId, "Счёт добавлен в систему");
                //выходим из меню, но остаётся аунтификация
                context.clear();
                tableDetails.clear();
                printMainMenu(chatId);
                return;
            }
        }
    }

    private boolean correctValueOfPeople(String messageText) {
        int value;
        try {
            value = Integer.valueOf(messageText);
        } catch (NumberFormatException e) {
            return false;
        }
        return value > 0 && value <= 100;
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
            logger.error("Error sending message" + e.getLocalizedMessage());
        }
    }

    private void sendMessageWithTextAndInlineKeyboard(Long chatId, String messageText, List<String> inlineKeyboardText, List<String> callbackData) {

        SendMessage message = new SendMessage()
                .setChatId(chatId)
                .setText(messageText);
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        try {
            for (int i = 0; i < inlineKeyboardText.size(); i++) {
                List<InlineKeyboardButton> rowInline = new ArrayList<>();
                rowInline.add(new InlineKeyboardButton().setText(inlineKeyboardText.get(i)).setCallbackData(callbackData.get(i)));
                rowsInline.add(rowInline);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            logger.error("Не правильно заполнен лист для кнопок: " + e.getLocalizedMessage());
        }
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);
        try {
            sendMessage(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending message" + e.getLocalizedMessage());
        }
    }

    private void sendMessageWithTextAndInlineKeyboard(Long chatId, String messageText, InlineKeyboardButton... buttons) {

        SendMessage message = new SendMessage()
                .setChatId(chatId)
                .setText(messageText);
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        try {
            for (InlineKeyboardButton button : buttons) {
                List<InlineKeyboardButton> rowInline = new ArrayList<>();
                rowInline.add(button);
                rowsInline.add(rowInline);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            logger.error("Не правильно заполнен лист для кнопок: " + e.getLocalizedMessage());
        }
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);
        try {
            sendMessage(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending message" + e.getLocalizedMessage());
        }
    }

    private void printMainMenu(Long chatId) {

        sendMessageWithTextAndInlineKeyboard(chatId, "Вы находитесь в главном меню" + "\n"
                + "Вам доступны следующие команды: " + "\n", CHOOSE_TABLE_BUTTON, FOOD_MENU_BUTTON, OUT_BUTTON);
    }

    private void printHelloOnStart(Long chatId) {

        sendMessageWithTextAndInlineKeyboard(chatId, "Вам нужно пройти аутентификация при помощи команды: ", AUTH_BUTTON);
    }

    private void printOut(Long chatId) {

        sendMessageWithTextAndInlineKeyboard(chatId, "Все настройки сброшены чтобы начать работу авторизуйтесь: ", AUTH_BUTTON);
    }

    @Override
    public String getBotUsername() {

        return "CAFEcrmBot";
    }

    @Override
    public String getBotToken() {
        return "372169972:AAHGBe36HPmH3vRo07UmwTvjU0Ho0wTOS8s"; //production token
        //return "430590498:AAGCvzRsWL660e5NSgAeEDDxcQj8QH4Abb0";
    }
}
