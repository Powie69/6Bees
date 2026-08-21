package powie.sixbees.modules.autoPearlStatis;

import meteordevelopment.meteorclient.utils.player.ChatUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HiveHost extends Thread {
    private ServerSocket socket;
    private Socket clientSocket;
    private String messageToSend;

    /**
     * pretty much a meteor swarm skid
     * TODO: fix this abomination
     * this is stupid i should just use http lol
     */
    public HiveHost(int port) {
        try {
            socket = new ServerSocket(port);
        } catch (IOException e) {
            socket = null;
            ChatUtils.errorPrefix("Swarm", "Couldn't start a server on port %s.", port);
            e.printStackTrace();
        }

        if (socket != null) start();
    }

    @Override
    public void run() {
        ChatUtils.infoPrefix("Auto Pearl Stasis'", "Listening for incoming connections on port %s.", socket.getLocalPort());

        while (!isInterrupted()) {
            try {
                Socket connection = socket.accept();
                assignConnectionToSubServer(connection);
            } catch (IOException e) {
                ChatUtils.errorPrefix("Auto Pearl Stasis'", "Error making a connection to worker.");
                e.printStackTrace();
            }
        }
    }

    public void assignConnectionToSubServer(Socket connection) {
        if (clientSocket == null) {
            clientSocket = connection;
            ChatUtils.infoPrefix("Auto Pearl Stasis", "New worker connected on %s.", getIp(connection.getInetAddress().getHostAddress()));
            new Thread(this::handleConnection).start();
        }
    }

    private void handleConnection() {
        try {
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());

            while (!isInterrupted() && clientSocket != null && !clientSocket.isClosed()) {
                if (messageToSend != null) {
                    try {
                        out.writeUTF(messageToSend);
                        out.flush();
                    } catch (Exception e) {
                        ChatUtils.errorPrefix("Auto Pearl Stasis", "Encountered error when sending command.");
                        e.printStackTrace();
                        break;
                    }
                    messageToSend = null;
                }
                Thread.sleep(10);
            }

            out.close();
        } catch (IOException | InterruptedException e) {
            ChatUtils.infoPrefix("Auto Pearl Stasis", "Error creating a connection with %s on port %s.", getIp(clientSocket.getInetAddress().getHostAddress()), clientSocket.getPort());
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (clientSocket != null) {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ChatUtils.infoPrefix("Auto Pearl Stasis", "Worker disconnected on ip: %s.", clientSocket.getInetAddress().getHostAddress());
            clientSocket = null;
        }

        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ChatUtils.infoPrefix("Auto Pearl Stasis'", "Server closed on port %s.", socket.getLocalPort());

        interrupt();
    }

    public void sendMessage(String s) {
        if (clientSocket != null) {
            messageToSend = s;
        }
    }

    private String getIp(String ip) {
        return ip.equals("127.0.0.1") ? "localhost" : ip;
    }
}

