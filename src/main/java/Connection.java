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
             final BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             final BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream()))
        {
            // запрос
            Request request = Request.get(input);
            // ответ
            Response response = new Response(output);
            // отправить ответ
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
