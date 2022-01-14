import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Request {
    private String method;
    private ValidPaths path;
    private Map<String, List<String>> queryParams;

    // геттеры-сеттеры
    public String getMethod() {
        return method;
    }

    public ValidPaths getPath() {
        return path;
    }

    // декодер
    private static String decode(String value) {
        String result = "";
        try {
            result = URLDecoder.decode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }

    // получить список параметров из строки запроса
    private static Map<String, List<String>> getQueryParams(String query) {
        // нет параметров
        if(query == null) return null;
        String name, value;
        Map<String, List<String>> result = new HashMap<>();
        // массив пар параметр=значение
        String[] params = query.split("&");
        // цикл по парам
        for(String param : params) {
            List<String> values = new ArrayList<>();
            // разбиваем на параметр и значение
            name = param.split("=")[0];
            value = decode(param.split("=")[1]);
            // параметр уже присутствует в мапе
            if(result.containsKey(name)) {
                // список значений параметра из мапы
                values = result.get(name);
                // добавляем новое значение
                values.add(value);
                // обновляем в мапе
                result.replace(name, values);
            } else {
                // добавляем значение в список
                values.add(value);
                // добавляем в мапу
                result.put(name, values);
            }
        }
        return result;
    }

    // получить значение параметра
    private String getParamValue(String name) {
        // параметра нет в мапе
        if(!queryParams.containsKey(name)) return null;
        StringBuilder result = new StringBuilder();
        // список значений
        List<String> values = queryParams.get(name);
        switch (values.size()) {
            case 0:
                break;
            case 1:
                result.append(values.get(0));
                break;
            default:
                for (String value : values) {
                    result.append(value).append("\n");
                }
        }
        return result.toString();
    }

    // получить объект из строки запроса
    public static Request get(BufferedReader input) {
        // must be in form GET /path HTTP/1.1
        String requestLine = null;
        try {
            requestLine = input.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // проверка на пустую строку
        if(requestLine == null) return null;
        final String[] parts = requestLine.split(" ");
        // проверка на формат строки
        if (parts.length != 3) {
            // just close socket
            return null;
        }
        // создаем объект
        Request request = new Request();
        // метод
        request.method = parts[0];
        // путь к файлу
        String path;
        try {
            URI uri = new URI(parts[1]);
            path = uri.getPath();
            // путь
            request.path = ValidPaths.getValueByPath(path);
            // параметры запроса
            request.queryParams = getQueryParams(uri.getRawQuery());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        // проверка на наличие в списке доступных
        if (request.path == null) {
            return null;
        }
        return request;
    }

}
