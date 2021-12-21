
public class Main {
    public static void main(String[] args) {
        // сервер
        HttpServer server = new HttpServer(9999);
        // запуск сервера
        server.start();
    }
}


