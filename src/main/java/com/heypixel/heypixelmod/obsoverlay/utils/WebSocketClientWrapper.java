package com.heypixel.heypixelmod.obsoverlay.utils;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

/**
 * WebSocket客户端封装类喵~
 */
public abstract class WebSocketClientWrapper extends WebSocketClient {
    
    public WebSocketClientWrapper(URI serverUri) {
        super(serverUri);
    }
    
    @Override
    public abstract void onOpen(ServerHandshake handshake);
    
    @Override
    public abstract void onMessage(String message);
    
    @Override
    public abstract void onClose(int code, String reason, boolean remote);
    
    @Override
    public abstract void onError(Exception ex);
    
    /**
     * 连接到服务器喵~
     */
    public void connect() {
        try {
            super.connect();
        } catch (Exception e) {
            onError(e);
        }
    }
    
    /**
     * 发送消息喵~
     */
    public void send(String message) {
        if (isOpen()) {
            super.send(message);
        }
    }
    
    /**
     * 关闭连接喵~
     */
    public void close() {
        super.close();
    }
    
    /**
     * 检查连接是否打开喵~
     */
    public boolean isOpen() {
        return super.isOpen();
    }
}