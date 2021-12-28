import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;


public class Connection implements Runnable{
    private final Socket socket;
    private BufferedOutputStream out;
    private static final int LIMIT = 4096;

    private Map<String, List<String>> queryParams;

    // конструктор
    public Connection(Socket socket) {
        this.socket = socket;
    }

    // формирование сообщения 404 Error
    private String getHttp404Text() {
        return "HTTP/1.1 404 Not Found\r\n" +
                "Content-Length: 0\r\n" +
                "Connection: close\r\n" +
                "\r\n";
    }

    // формирование сообщения 200 ОК
    private String getHttp200Text(String mimeType, Long length) {
        return "HTTP/1.1 200 OK\r\n" +
                "Content-Type: " + mimeType + "\r\n" +
                "Content-Length: " + length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";
    }

    // обработать запрос на получение CLASSIC_HTML
    private void responseClassicHtml(Path filePath, String mimeType) {
        try {
            final String template = Files.readString(filePath);
            final byte[] content = template.replace(
                    "{time}",
                    LocalDateTime.now().toString()
            ).getBytes();
            out.write((getHttp200Text(mimeType, (long) content.length)).getBytes());
            out.write(content);
            out.flush();
        } catch (IOException exp) {
            exp.printStackTrace();
        }
    }

    // обработать запрос на получение файла
    private void responseDefault(Path filePath, String mimeType) {
        try {
            final long length = Files.size(filePath);
            out.write((getHttp200Text(mimeType, length)).getBytes());
            Files.copy(filePath, out);
            out.flush();
        } catch (IOException exp) {
            exp.printStackTrace();
        }
    }

    // декодер
    private String decode(String value) {
        String result = "";
        try {
            result = URLDecoder.decode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }
    // получить список параметров из строки запроса
    private Map<String, List<String>> getQueryParams(String query) {
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

    // обработка запроса
    @Override
    public void run() {
        try (
             final BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             final BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream()))
        {
            out = output;
            // read only request line for simplicity
            // must be in form GET /path HTTP/1.1
            final String requestLine = input.readLine();
            // проверка на пустую строку
            if(requestLine == null) return;
            // разбиение строки
            final String[] parts = requestLine.split(" ");
            // проверка на формат строки
            if (parts.length != 3) {
                // just close socket
                return;
            }
            // путь к файлу
            String path = "";
            try {
                URI uri = new URI(parts[1]);
                path = uri.getPath();
                queryParams = getQueryParams(uri.getRawQuery());
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            // ссылка на путь из списка доступных сервера
            final ValidPaths validPaths = ValidPaths.getValueByPath(path);
            // проверка на наличие в списке доступных
            if (validPaths == null) {
                // ответ Error 404
                out.write((getHttp404Text()).getBytes());
                out.flush();
                return;
            }
            // путь к файлу
            final Path filePath = Path.of(".", "public", path);
            // тип содержимого файла
            final String mimeType = Files.probeContentType(filePath);
            // отправка ответа
            switch (validPaths) {
                case CLASSIC_HTML:
                    responseClassicHtml(filePath, mimeType);
                    break;
                default:
                    responseDefault(filePath, mimeType);
                    break;
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
