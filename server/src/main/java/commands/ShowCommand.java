package commands;

import data.Product;
import db.AuthManager;
import exceptions.EndInputException;
import exceptions.WrongNumberOfArgsException;
import requests.Request;
import responses.ErrorResponse;
import responses.MultiResponse;
import responses.Response;
import responses.SuccessResponse;
import utility.CollectionManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ShowCommand implements Command{
    private final CollectionManager collectionManager;
    private final AuthManager authManager;

    public ShowCommand(CollectionManager collectionManager, AuthManager authManager) {
        this.collectionManager = collectionManager;
        this.authManager = authManager;
    }

    @Override
    public String getName() {
        return "show";
    }

    @Override
    public String getDescription() {
        return "вывести все элементы коллекции";
    }

    @Override
    public Response execute(Request request) throws WrongNumberOfArgsException, EndInputException, SQLException {
        String username = request.getUsername();
        String passwordHash = request.getPasswordHash();

        if (!authManager.login(username, passwordHash)) {
            return new ErrorResponse("Ошибка авторизации: неверный логин или пароль.");
        }

        List<Product> products = collectionManager.getProducts();
        if(products.isEmpty()){
            return new SuccessResponse("Коллекция пуста");
        }
        /*
        StringBuilder sb = new StringBuilder("Содержимое коллекции:\n");
        for(Product p : products){
            String productStr = p.toString() + "\n";
            if (sb.length() + productStr.length() > 3000) {
                sb.append("... (вывод обрезан, превышен лимит в 3000 символов)\n");
                break;
            }
            sb.append(productStr);
        }
        return new SuccessResponse(sb.toString());*/

        // будем нарезать на части по 1000 символов
        int maxPartSize = 1000;
        List<String> parts = new ArrayList<>();
        StringBuilder currentPart = new StringBuilder();

        for (Product p : products) {
            String productStr = p.toString() + "\n";

            // если текущее добавление превысит лимит — сохраняем кусок и начинаем новый
            if (currentPart.length() + productStr.length() > maxPartSize) {
                parts.add(currentPart.toString());
                currentPart = new StringBuilder();
            }
            currentPart.append(productStr);
        }
        if (currentPart.length() > 0) {
            parts.add(currentPart.toString());
        }

        if (parts.size() == 1) {
            return new SuccessResponse(parts.get(0));
        }
        return new MultiResponse(parts);
    }
}
