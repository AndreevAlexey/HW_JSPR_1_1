import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class Connection {
    private final Socket socket;
    private BufferedOutputStream out;
    private BufferedReader in;

    // конструктор
    public Connection(Socket socket) {
        this.socket = socket;
        try {
            out = new BufferedOutputStream(socket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public BufferedOutputStream getOut() {
        return out;
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
    public void responseDefault(Path filePath, String mimeType) {
        try {
            final long length = Files.size(filePath);
            out.write((getHttp200Text(mimeType, length)).getBytes());
            Files.copy(filePath, out);
            out.flush();
        } catch (IOException exp) {
            exp.printStackTrace();
        }
    }

    // получить Request
    public Request getRequest() {
        Request request;
        try
        {
            // read only request line for simplicity
            // must be in form GET /path HTTP/1.1
            String requestLine = in.readLine();
            // проверка на пустую строку
            if(requestLine == null)
                return null;
            // разбиение строки
            final String[] parts = requestLine.split(" ");
            // проверка на формат строки
            if (parts.length != 3) {
                // just close socket
                return null;
            }
            // запрос
            request = new Request(parts[0]);
            // путь к файлу
            request.setPath(parts[1]);
            // строки запроса
            List<String> lines = new ArrayList<>();
            lines.add(requestLine);
            while ((requestLine = in.readLine()) != null) {
                // получили пустую строку: конец блока или сообщения
                if (requestLine.length() == 0) {
                    // заголовки
                    request.setHeaders(lines);
                    lines.clear();
                    break;
                }
                lines.add(requestLine);
            }
            // тело запроса
            request.setBody(in.toString());

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return request;
    }

    // закрыть соединение
    public void close() {
        try {
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
