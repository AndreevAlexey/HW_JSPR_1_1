import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class Connection implements Runnable{
    private final Socket socket;
    private BufferedOutputStream out;

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
            final String path = parts[1];
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
