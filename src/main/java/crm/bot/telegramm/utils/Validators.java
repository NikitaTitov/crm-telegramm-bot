package crm.bot.telegramm.utils;

public class Validators {

	public static boolean isValidPrice(String price) {
		try {
			Long longPrice = Long.parseLong(price);
			if (Math.ceil(Math.log10(longPrice + 0.5)) < 7) {
				return true;
			}
		} catch (NumberFormatException ignored) {
		}
		return false;
	}
}
