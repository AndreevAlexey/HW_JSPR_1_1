import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServer {
    private final static int THREADS_POOL_CNT = 64;
    private final int port;

    // конструктор
    public HttpServer(int port) {
        this.port = port;
    }

    // старт сервера
    public void start() {
        // пул потоков
        ExecutorService service = Executors.newFixedThreadPool(THREADS_POOL_CNT);
        // запуск сервера
        try (final ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                // ожидание подключения
                final Socket socket = serverSocket.accept();
                // создание соединения
                Connection connection = new Connection(socket);
                // передача пулу-потоков задания
                service.submit(connection);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // остановка пула-потоков
        service.shutdown();
    }
}
