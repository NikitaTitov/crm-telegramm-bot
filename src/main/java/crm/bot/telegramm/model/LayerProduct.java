package crm.bot.telegramm.model;


import com.fasterxml.jackson.annotation.*;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonIdentityInfo(
		generator = ObjectIdGenerators.PropertyGenerator.class,
		property = "id", scope = LayerProduct.class)
public class LayerProduct {

	private Long id;

	private String name;

	private String description;

	private Double cost;

	private boolean dirtyProfit = true;

	private boolean floatingPrice;

	private boolean accountability;

	private Long productId;

	private List<Client> clients;

	public LayerProduct() {
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Double getCost() {
		return cost;
	}

	public void setCost(Double cost) {
		this.cost = cost;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public boolean isDirtyProfit() {
		return dirtyProfit;
	}

	public void setDirtyProfit(boolean dirtyProfit) {
		this.dirtyProfit = dirtyProfit;
	}

	public List<Client> getClients() {
		return clients;
	}

	public void setClients(List<Client> clients) {
		this.clients = clients;
	}

	public boolean isFloatingPrice() {
		return floatingPrice;
	}

	public void setFloatingPrice(boolean floatingPrice) {
		this.floatingPrice = floatingPrice;
	}

	public boolean isAccountability() {
		return accountability;
	}

	public void setAccountability(boolean accountability) {
		this.accountability = accountability;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		LayerProduct that = (LayerProduct) o;

		if (id != null ? !id.equals(that.id) : that.id != null) return false;
		return name != null ? name.equals(that.name) : that.name == null;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (name != null ? name.hashCode() : 0);
		return result;
	}

	public Long getProductId() {
		return productId;
	}

	public void setProductId(Long productId) {
		this.productId = productId;
	}
}
