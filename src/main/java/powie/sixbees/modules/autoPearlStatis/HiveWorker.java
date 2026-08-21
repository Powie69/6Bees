package powie.sixbees.modules.autoPearlStatis;

import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class HiveWorker extends Thread {
    private Socket socket;

    public HiveWorker(String ip, int port) {
        try {
            socket = new Socket(ip, port);
        } catch (Exception e) {
            socket = null;
            ChatUtils.warningPrefix("Auto Pearl Stasis", "Server not found at %s on port %s.", ip, port);
            e.printStackTrace();
        }

        if (socket != null) start();
    }

    @Override
    public void run() {
        ChatUtils.infoPrefix("Auto Pearl Stasis'", "Connected to Auto Pearl Stasis' host on at %s on port %s.", getIp(socket.getInetAddress().getHostAddress()), socket.getPort());

        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());

            while (!isInterrupted()) {
                in.readUTF();
                Modules.get().get(AutoPearlStasis.class).pullPearl();
            }

            in.close();
        } catch (IOException e) {
            ChatUtils.errorPrefix("Auto Pearl Stasis'", "Error in connection to host.");
            e.printStackTrace();
            disconnect();
        }
    }

    public void disconnect() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        PathManagers.get().stop();

        ChatUtils.infoPrefix("Auto Pearl Stasis'", "Disconnected from host.");

        interrupt();
    }

    private String getIp(String ip) {
        return ip.equals("127.0.0.1") ? "localhost" : ip;
    }
}
