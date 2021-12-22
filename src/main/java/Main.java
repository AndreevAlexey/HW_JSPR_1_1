
public class Main {
    public static final int PORT = 9999;
    public static void main(String[] args) {
        // сервер
        HttpServer server = new HttpServer(PORT);
        // запуск сервера
        server.start();
    }
}


