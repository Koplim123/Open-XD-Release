package com.heypixel.heypixelmod.obsoverlay.IRC;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

public class IRCWebSocketConnectAPI {
    private WebSocketClient client;
    private String serverUri;
    private String username;
    private volatile boolean connected = false;

    public IRCWebSocketConnectAPI(String serverUri, String username) {
        this.serverUri = serverUri;
        this.username = username;
    }

    public void connect() throws URISyntaxException {
        client = new WebSocketClient(new URI(serverUri)) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                connected = true;
                // 发送加入消息
                JsonObject joinMessage = new JsonObject();
                joinMessage.addProperty("type", "join");
                joinMessage.addProperty("username", username);
                send(joinMessage.toString());
                ChatUtils.addChatMessage("§a[IRC] Connected to server");
            }

            @Override
            public void onMessage(String message) {
                try {
                    JsonObject data = JsonParser.parseString(message).getAsJsonObject();
                    Receive.handleMessage(data);
                } catch (Exception e) {
                    ChatUtils.addChatMessage("§c[IRC] Error parsing message: " + e.getMessage());
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                connected = false;
                ChatUtils.addChatMessage("§c[IRC] Disconnected from server: " + reason);
            }

            @Override
            public void onError(Exception ex) {
                ChatUtils.addChatMessage("§c[IRC] WebSocket error: " + ex.getMessage());
                ex.printStackTrace();
            }
        };

        client.connect();
    }

    public void disconnect() {
        connected = false;
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                // 忽略关闭时的异常
            }
        }
    }

    public void sendMessage(String message) {
        if (client != null && connected) {
            client.send(message);
        } else {
            throw new IllegalStateException("WebSocket is not connected");
        }
    }

    public boolean isConnected() {
        return connected && client != null && !client.isClosing() && !client.isClosed();
    }

    public String getUsername() {
        return username;
    }
}