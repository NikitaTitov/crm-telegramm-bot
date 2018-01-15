package crm.bot.telegramm.utils;

import crm.bot.telegramm.model.Calculate;
import crm.bot.telegramm.model.Category;
import crm.bot.telegramm.model.Client;
import crm.bot.telegramm.model.Product;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class NameAndIDGetters {

    public static List<String> getListProductId(List<Product> products) {
        return products.stream().map(Product::getId).map(String::valueOf).collect(Collectors.toList());
    }

    public static List<String> getListProductNames(List<Product> products) {
    	List<String> productNames = new ArrayList<>();
    	for (Product product : products) {
    		if (product.getCategory().isFloatingPrice()) {
    			productNames.add(product.getName());
			} else {
    			productNames.add(product.getName() + " : " + String.valueOf(product.getCost()) + "p");
			}
		}
        return productNames;
    }

    public static List<String> getListCalculateNames(Set<Calculate> calculates) {
        return calculates.stream().map(Calculate::getDescription).collect(Collectors.toList());
    }

    public static List<String> getCalculateListId(Set<Calculate> calculates) {
        return calculates.stream().map(Calculate::getId).map(String::valueOf).collect(Collectors.toList());
    }

    public static List<Client> getClientsFromCalculateWithNotEmptyDescription(String calcId, Set<Calculate> calculates) throws IOException {
        List<Client> clients = new ArrayList<>();
        for (Calculate calculate : calculates) {
            if (calculate.getId().equals(Long.valueOf(calcId))) {
                clients = calculate.getClient().stream()
						.filter(client -> client.isState() && !client.isDeleteState())
                        .filter(client -> !(client.getDescription() == null || client.getDescription().isEmpty()))
						.collect(Collectors.toList());
                }
            }
        return clients;
    }

    public static List<Client> getClientsFromCalculate(String calcId, Set<Calculate> calculates) throws IOException {
        List<Client> clients = new ArrayList<>();
        for (Calculate calculate : calculates) {
            if (calculate.getId().equals(Long.valueOf(calcId))) {
                return calculate.getClient().stream()
						.filter(client -> client.isState() && !client.isDeleteState())
						.collect(Collectors.toList());
            }
        }
        return clients;
    }

    public static List<String> getListClientsNames(List<Client> clients) {
        return clients.stream().map(Client::getDescription).collect(Collectors.toList());
    }

    public static List<String> getListClientsId(List<Client> clients) {
        return clients.stream().map(Client::getId).map(String::valueOf).collect(Collectors.toList());
    }

    public static List<String> getListCategoryNames(List<Category> categories) {
        return categories.stream().map(Category::getName).collect(Collectors.toList());
    }

    public static List<String> getListCategoryId(List<Category> categories) {
        return categories.stream().map(Category::getId).map(String::valueOf).collect(Collectors.toList());
    }

    public static Category getCategory(String categoryId, List<Category> categories) throws IOException {
		for (Category category : categories) {
			if (category.getId().equals(Long.valueOf(categoryId))) {
				return category;
			}
		}
		throw new IOException("Category with id:" + categoryId + " has bean removed");
	}

    public static List<Product> getListProductsByCategoryId(String categoryId, List<Category> categories) throws IOException {
        for (Category category : categories) {
            if (category.getId().equals(Long.valueOf(categoryId))) {
                return category.getProducts().stream().peek(p -> p.setCategory(category)).collect(Collectors.toList());
            }
        }
        throw new IOException("There is no table with " + categoryId + " category id");
    }
}
