package crm.bot.telegramm.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonIdentityInfo(
		generator = ObjectIdGenerators.PropertyGenerator.class,
		property = "id", scope = Category.class)
public class Category {

	private Long id;

	private String name;

	private List<Product> products;

	private boolean dirtyProfit = true;

	private boolean floatingPrice;

	private boolean accountability;

	public Category(String name) {
		this.name = name;
	}

	public Category() {

	}

	public Long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Product> getProducts() {
		return products;
	}

	public void setProducts(List<Product> products) {
		this.products = products;
	}

	public boolean isDirtyProfit() {
		return dirtyProfit;
	}

	public void setDirtyProfit(boolean dirtyProfit) {
		this.dirtyProfit = dirtyProfit;
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

		Category category = (Category) o;

		if (id != category.id) return false;
		return name != null ? name.equals(category.name) : category.name == null;
	}

	@Override
	public int hashCode() {
		int result = getId() != null ? getId().hashCode() : 0;
		result = 31 * result + (getName() != null ? getName().hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "Category{" +
				"id=" + id +
				", name='" + name + '\'' +
				'}';
	}
}
