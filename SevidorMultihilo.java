import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



public class SevidorMultihilo{
    public static void main(String[] args) throws IOException {
        try(ServerSocket listener = new ServerSocket(20064)){
            System.out.println("El servidor multihilo se ha iniciado... \n");

            ExecutorService pool = Executors.newFixedThreadPool(20);

            while(true){
                pool.execute(new Capitalizacion(listener.accept()));
            }

        }
    }

    private static class Capitalizacion implements Runnable{
        private Socket socket;

        public Capitalizacion(Socket socket){
            this.socket = socket;

        }

        @Override
        public void run(){
            System.out.append("Conectado: " + socket);

            try{
                BufferedReader lector = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
                );
                DataOutputStream respuesta = new DataOutputStream(socket.getOutputStream());

                String lineaSolicitud = lector.readLine();
                if (lineaSolicitud == null || lineaSolicitud.isEmpty()) {
                    return;
                }

                System.out.println("REQUEST: " + lineaSolicitud);
                String lineaHeader;
                while ((lineaHeader = lector.readLine()) != null && !lineaHeader.isEmpty()) {
                    System.out.println("REQUEST: " + lineaHeader);
                }

                String[] partes = lineaSolicitud.split(" ");
                String metodo = partes.length > 0 ? partes[0] : "";
                String ruta = partes.length > 1 ? partes[1] : "";

                if (!"GET".equals(metodo)) {
                    String html = "<html><body><h1>400 Bad Request</h1></body></html>";

                    respuesta.writeBytes("HTTP/1.0 400 Bad Request\r\n");

                    respuesta.writeBytes("Content-Type: text/html\r\n");

                    respuesta.writeBytes("Content-Length: " + html.getBytes().length + "\r\n");

                    respuesta.writeBytes("\r\n");

                    respuesta.writeBytes(html);
                    
                    return;
                }

                if (ruta.startsWith("/")) {
                    ruta = ruta.substring(1);
                }
                if (ruta.isEmpty()) {
                    ruta = "index.html";
                }

                File archivo = new File(ruta);
                if (!archivo.exists() || archivo.isDirectory()) {
                    String html = "<html><body><h1>404 Not Found</h1></body></html>";
                    respuesta.writeBytes("HTTP/1.0 404 Not Found\r\n");
                    respuesta.writeBytes("Content-Type: text/html\r\n");
                    respuesta.writeBytes("Content-Length: " + html.getBytes().length + "\r\n");
                    respuesta.writeBytes("\r\n");
                    respuesta.writeBytes(html);
                    return;
                }

                FileInputStream archivoDeEntrada = new FileInputStream(archivo);
                int cantidadDeBytes = (int) archivo.length();
                byte[] archivoEnBytes = new byte[cantidadDeBytes];
                archivoDeEntrada.read(archivoEnBytes);

                respuesta.writeBytes("HTTP/1.0 200 OK\r\n");
                if (ruta.endsWith(".jpg") || ruta.endsWith(".jpeg"))
                    respuesta.writeBytes("Content-Type: image/jpeg\r\n");
                else if (ruta.endsWith(".gif"))
                    respuesta.writeBytes("Content-Type: image/gif\r\n");
                else if (ruta.endsWith(".html") || ruta.endsWith(".htm"))
                    respuesta.writeBytes("Content-Type: text/html\r\n");
                else
                    respuesta.writeBytes("Content-Type: application/octet-stream\r\n");
                respuesta.writeBytes("Content-Length: " + cantidadDeBytes + "\r\n");
                respuesta.writeBytes("\r\n");
                respuesta.write(archivoEnBytes, 0, cantidadDeBytes);

                archivoDeEntrada.close();
            }catch(IOException e) {
                System.out.println("Errir -> " + e.getMessage());
                

            }finally {
                try{

                    socket.close();

                }catch(IOException e){}

                System.out.println("Cerrado: " + socket);
            }

        }
    }
}