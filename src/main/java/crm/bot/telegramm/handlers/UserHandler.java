package crm.bot.telegramm.handlers;


import crm.bot.telegramm.model.User;

public class UserHandler {

	public static boolean isEmptyUser(User user) {

		return user.getId() != null || user.getFirstName() != null || user.getLastName() != null || user.getEmail() != null || user.getPhone() != null;
	}
}
