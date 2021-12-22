import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServer {
    private final static int THREADS_POOL_CNT = 64;
    private final int port;

    private static ConcurrentHashMap<String, Handler> handlers = new ConcurrentHashMap<>();

    // конструктор
    public HttpServer(int port) {
        this.port = port;
    }

    public void addHandler(String type, String path, Handler handler) {
        String key = type + path;
        if(!handlers.containsKey(key)) {
            handlers.put(key, handler);
        }
    }

    public Handler getHandler(String type, String path) {
        String key = type + path;
        return handlers.getOrDefault(key, null);
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
                // запрос
                Request request = connection.getRequest();
                // проверка
                if(request == null) {
                    connection.close();
                    continue;
                }
                // обработчик
                Handler handler = getHandler(request.getReqType(), request.getPath());
                // передача пулу-потоков задания
                if(handler != null) {
                    service.submit(() -> handler.handle(request, connection));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // остановка пула-потоков
        service.shutdown();
    }
}
