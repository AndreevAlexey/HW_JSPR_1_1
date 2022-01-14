import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class Response {
    private final BufferedOutputStream out;

    // конструктор
    public Response(BufferedOutputStream out) {
        this.out = out;
    }

    // формирование сообщения 404 Error
    public static String getHttp404Text() {
        return "HTTP/1.1 404 Not Found\r\n" +
                "Content-Length: 0\r\n" +
                "Connection: close\r\n" +
                "\r\n";
    }

    // формирование сообщения 200 ОК
    public static String getHttp200Text(String mimeType, Long length) {
        return "HTTP/1.1 200 OK\r\n" +
                "Content-Type: " + mimeType + "\r\n" +
                "Content-Length: " + length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";
    }

    // отправить Error 404
    public void send404() {
        // ответ Error 404
        try {
            out.write((getHttp404Text()).getBytes());
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

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

    // отправить ответ
    public void send(Request request) {
        // проверка запроса
        if(request == null) {
            // ответ Error 404
            send404();
            return;
        }
        // путь к файлу
        final Path filePath = Path.of(".", "public", request.getPath().toString());
        // тип содержимого файла
        final String mimeType;
        try {
            mimeType = Files.probeContentType(filePath);
            // отправка ответа
            if (request.getPath() == ValidPaths.CLASSIC_HTML) {
                responseClassicHtml(filePath, mimeType);
            } else {
                responseDefault(filePath, mimeType);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
