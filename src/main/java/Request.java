import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Request {
    private Methods method;
    private ValidPaths path;
    private Map<String, List<String>> queryParams;
    private Map<String, List<String>> bodyParams;

    private static final int LIMIT = 4096;
    private static final String xwwwform = "application/x-www-form-urlencoded";
    private static final String multipart = "multipart/form-data";

    // геттеры-сеттеры
    public Methods getMethod() {
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
    public static Request get(BufferedInputStream in) {
        Request request = null;
        try {
            // метка на количество байт
            in.mark(LIMIT);
            final byte[] buffer = new byte[LIMIT];
            final int read = in.read(buffer);

            // ищем request line
            final byte[] requestLineDelimiter = new byte[]{'\r', '\n'};
            final int requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
            if (requestLineEnd == -1) {
                return null;
            }

            // читаем request line
            final String[] requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
            if (requestLine.length != 3) {
                return null;
            }
            // создаем объект
            request = new Request();
            // метод
            request.method = Methods.getValueByName(requestLine[0]);
            // проверка метода
            if(request.method == null) {
                return null;
            }
            // путь к файлу
            String path = "";
            try {
                URI uri = new URI(requestLine[1]);
                path = uri.getPath();
                // путь
                request.path = ValidPaths.getValueByPath(path);
                // параметры запроса
                request.queryParams = getQueryParams(uri.getRawQuery());
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            // проверка пути
            if (!path.startsWith("/")) {
                return null;
            }
            System.out.println(requestLine[0]);
            System.out.println(path);

            // ищем заголовки
            final byte[] headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
            final int headersStart = requestLineEnd + requestLineDelimiter.length;
            final int headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
            if (headersEnd == -1) {
                return null;
            }
            // отматываем на начало буфера
            in.reset();
            // пропускаем requestLine
            in.skip(headersStart);
            // заголовки
            final byte[] headersBytes = in.readNBytes(headersEnd - headersStart);
            final List<String> headers = Arrays.asList(new String(headersBytes).split("\r\n"));
            System.out.println(headers);

            // POST
            if (request.method == Methods.POST) {
                in.skip(headersDelimiter.length);
                // вычитываем Content-Length, чтобы прочитать body
                final Optional<String> contentLength = extractHeader(headers, "Content-Length");
                // есть контент
                if (contentLength.isPresent()) {
                    // тип контента
                    final Optional<String> contentType = extractHeader(headers, "Content-Type");
                    // размер контента
                    final int length = Integer.parseInt(contentLength.get());
                    final byte[] bodyBytes = in.readNBytes(length);
                    // тело запроса
                    final String body = new String(bodyBytes);
                    System.out.println(body);
                    // нужный формат
                    if(xwwwform.equals(contentType.get())) {
                        // параметры из тела запроса
                        request.bodyParams = getQueryParams(body);
                        System.out.println(request.bodyParams );
                    }
                }
            // GET
            } else {
                // проверка на наличие в списке доступных
                if (request.path == null) {
                    return null;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return request;
    }

    // from google guava with modifications
    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }
}