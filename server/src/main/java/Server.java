import db.*;
import requests.Request;
import responses.ErrorResponse;
import responses.Response;
import utility.CollectionManager;
import utility.CommandManager;
import utility.CommandWrapper;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.logging.*;

public class Server {
    private static final int port = 12348;
    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private static volatile boolean running = true;

    public static void main(String[] args) {
        configureLogger();

        try {
            DBManager dbManager = new DBManager();
            OrganizationDAO organizationDAO = new OrganizationDAO(dbManager.getConnection());
            ProductDAO productDAO = new ProductDAO(dbManager.getConnection(), organizationDAO);
            UserDAO userDAO = new UserDAO(dbManager);

            CollectionManager collectionManager = new CollectionManager(productDAO);
            AuthManager authManager = new AuthManager(userDAO);
            CommandManager commandManager = new CommandManager(collectionManager, authManager, productDAO);


            ForkJoinPool readPool = new ForkJoinPool(Math.max(2, Runtime.getRuntime().availableProcessors() / 2));
            ForkJoinPool processPool = new ForkJoinPool();

            DatagramSocket socket = new DatagramSocket(port);
            logger.info("Сервер запущен на порту " + port);


            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Получен сигнал завершения. Останавливаем сервер...");
                running = false;
                try {
                    socket.close();
                } catch (Exception ignored) { }
                readPool.shutdown();
                processPool.shutdown();
                try {
                    readPool.awaitTermination(5, TimeUnit.SECONDS);
                    processPool.awaitTermination(10, TimeUnit.SECONDS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                dbManager.close();
                logger.info("Сервер остановлен.");
            }));


            while (running) {
                try {
                    byte[] buf = new byte[8192];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);

                    socket.receive(packet);

                    // Передаем десериализацию/валидацию в readPool
                    readPool.execute(() -> {
                        try {
                            RequestFrame frame = deserialize(packet);
                            // передаем логику команды в processPool
                            processPool.execute(() -> {
                                Response response = safeExecute(frame.wrapper, commandManager);
                                // отправляем ответ в отдельном Thread
                                new Thread(() -> {
                                    try {
                                        sendResponse(socket, response, frame.clientAddr, frame.clientPort);
                                    } catch (IOException e) {
                                        logger.log(Level.WARNING, "Ошибка отправки ответа", e);
                                    }
                                }, "send-" + frame.clientAddr + ":" + frame.clientPort).start();
                            });
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "Ошибка при чтении пакета", e);
                            InetAddress addr = packet.getAddress();
                            int port = packet.getPort();
                            Response err = new ErrorResponse("Ошибка чтения/десериализации запроса");
                            new Thread(() -> {
                                try {
                                    sendResponse(socket, err, addr, port);
                                } catch (IOException ignored) { }
                            }, "send-error-" + addr + ":" + port).start();
                        }
                    });

                } catch (SocketException se) {
                    if (running) {
                        logger.log(Level.WARNING, "Ошибка при приёме пакета", se);
                    }
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Ошибка при приёме пакета", e);
                }
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка запуска сервера: " + e.getMessage(), e);
        }
    }

    //десериализованное содержимое + адрес клиента
    private static class RequestFrame {
        final CommandWrapper wrapper;
        final InetAddress clientAddr;
        final int clientPort;

        RequestFrame(CommandWrapper wrapper, InetAddress clientAddr, int clientPort) {
            this.wrapper = wrapper;
            this.clientAddr = clientAddr;
            this.clientPort = clientPort;
        }
    }

    private static RequestFrame deserialize(DatagramPacket packet) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(packet.getData(), 0, packet.getLength()))) {
            CommandWrapper wrapper = (CommandWrapper) in.readObject();
            return new RequestFrame(wrapper, packet.getAddress(), packet.getPort());
        }
    }

    private static Response safeExecute(CommandWrapper wrapper, CommandManager commandManager) {
        if (wrapper == null) {
            return new ErrorResponse("Пустой запрос");
        }
        String name = wrapper.getCommandName();
        Request request = wrapper.getRequest();

        try {
            logger.info(() -> "Выполняется команда: " + name +
                    (request != null && request.getUsername() != null ? (" от " + request.getUsername()) : ""));

            var cmd = commandManager.getCommand(name);
            if (cmd == null) {
                return new ErrorResponse("Команда не найдена: " + name);
            }
            Response resp = cmd.execute(request);
            return resp != null ? resp : new ErrorResponse("Команда вернула пустой ответ");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Ошибка при выполнении команды " + name, e);
            return new ErrorResponse("Внутренняя ошибка сервера: " + e.getMessage());
        }
    }

    private static void sendResponse(DatagramSocket socket, Response response,
                                     InetAddress clientAddress, int clientPort) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(response);
            out.flush();
        }
        byte[] data = bos.toByteArray();
        DatagramPacket resp = new DatagramPacket(data, data.length, clientAddress, clientPort);
        socket.send(resp);
        logger.info("Ответ отправлен клиенту: " + clientAddress + ":" + clientPort);
    }

    private static void configureLogger() {
        Logger root = Logger.getLogger("");
        for (Handler h : root.getHandlers()) root.removeHandler(h);
        ConsoleHandler ch = new ConsoleHandler();
        ch.setLevel(Level.INFO);
        ch.setFormatter(new SimpleFormatter());
        root.setLevel(Level.INFO);
        root.addHandler(ch);
        root.setUseParentHandlers(false);
    }
}

