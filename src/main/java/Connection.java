import java.io.*;
import java.net.*;

public class Connection implements Runnable{
    private final Socket socket;

    // конструктор
    public Connection(Socket socket) {
        this.socket = socket;
    }

    // обработка запроса
    @Override
    public void run() {
        try (
             final BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
             final BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream()))
        {
            // Запрос
            Request request = Request.get(in);
            // Ответ
            Response response = new Response(out);
            // отправить ответ клиенту
            response.sendOnRequest(request);

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
