package crm.bot.telegramm.model;



public abstract class BaseEntity {

	private Company company;

	public Company getCompany() {
		return company;
	}

	public void setCompany(Company company) {
		this.company = company;
	}
}