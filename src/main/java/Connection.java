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
    private static final List<String> allowedMethods = List.of("GET", "POST");
    private static final String xwwwform = "application/x-www-form-urlencoded";
    private static final String multipart = "multipart/form-data";

    private Map<String, List<String>> queryParams;
    private Map<String, List<String>> bodyParams;

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
    // ответ на запрос Error
    private void badRequest(BufferedOutputStream out) throws IOException {
        out.write(getHttp404Text().getBytes());
        out.flush();
    }

    // положительный ответ на запрос
    private void response200() {
        try {
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
        } catch (IOException exp) {
            exp.printStackTrace();
        }
    }

    // ответ на запрос на получение файла
    private void responseDefaultGET(Path filePath, String mimeType) {
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

    // параметры строки запроса
    private Map<String, List<String>> getQueryParams(String query) {
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

    // получить значения параметра
    private String getParamValue(String name) {
        if(!queryParams.containsKey(name)) return null;
        StringBuilder result = new StringBuilder();
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

    // получить значения параметра
    private List<String> getParamValuesList(String name) {
        if(!queryParams.containsKey(name)) return null;
        return queryParams.get(name);
    }

    // обработка запроса
    @Override
    public void run() {
        try (
             final BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
             final BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream()))
        {
            out = output;
            // метка на количество байт
            in.mark(LIMIT);
            final byte[] buffer = new byte[LIMIT];
            final int read = in.read(buffer);

            // ищем request line
            final byte[] requestLineDelimiter = new byte[]{'\r', '\n'};
            final int requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
            if (requestLineEnd == -1) {
                badRequest(out);
                return;
            }

            // читаем request line
            final String[] requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
            if (requestLine.length != 3) {
                badRequest(out);
                return;
            }
            // путь к файлу
            String path = "";
            try {
                URI uri = new URI(requestLine[1]);
                path = uri.getPath();
                queryParams = getQueryParams(uri.getRawQuery());
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }

            final String method = requestLine[0];
            if (!allowedMethods.contains(method)) {
                badRequest(out);
                return;
            }
            System.out.println(method);

            if (!path.startsWith("/")) {
                badRequest(out);
                return;
            }
            System.out.println(path);

            // ищем заголовки
            final byte[] headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
            final int headersStart = requestLineEnd + requestLineDelimiter.length;
            final int headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
            if (headersEnd == -1) {
                badRequest(out);
                return;
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
            if (method.equals("POST")) {
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
                    // нужный формат
                    if(xwwwform.equals(contentType.get())) {
                        // параметры из тела запроса
                        bodyParams = getQueryParams(body);
                    }
                    // ответ ОК 200
                    response200();
                }
            } else {
                // ссылка на путь из списка доступных сервера
                final ValidPaths validPaths = ValidPaths.getValueByPath(path);
                // проверка на наличие в списке доступных
                if (validPaths == null) {
                    // ответ Error 404
                    badRequest(out);
                    return;
                }
                // путь к файлу
                final Path filePath = Path.of(".", "public", path);
                // тип содержимого файла
                final String mimeType = Files.probeContentType(filePath);
                // отправка ответа
                if (validPaths == ValidPaths.CLASSIC_HTML) {
                    responseClassicHtml(filePath, mimeType);
                } else {
                    responseDefaultGET(filePath, mimeType);
                }
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

    private Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }


}
