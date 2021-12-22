import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static final int PORT = 9999;
    public static void main(String[] args) {
        // сервер
        HttpServer server = new HttpServer(PORT);
        // добавляем обработчик
        server.addHandler("GET", "/spring.png", new Handler() {
            @Override
            public void handle(Request request, Connection connection) {
                String mimeType = null;
                // путь к файлу
                final Path filePath = Path.of(".", "public", request.getPath());
                // тип содержимого файла
                try {
                    mimeType = Files.probeContentType(filePath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                connection.responseDefault(filePath, mimeType);
                connection.close();
            }
        });
        // запуск сервера
        server.start();
    }
}


