package com.heypixel.heypixelmod.obsoverlay.IRCModules;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.heypixel.heypixelmod.obsoverlay.utils.IRCLoginManager;
import com.heypixel.heypixelmod.obsoverlay.utils.WebSocketClientWrapper;
import net.minecraft.client.Minecraft;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

public class ConnectAndReveives {
    private static final String SERVER_HOST = "45.192.105.145";
    private static final int SERVER_PORT = 8080;
    private static final String SERVER_URL = "ws://" + SERVER_HOST + ":" + SERVER_PORT;
    
    private WebSocketClientWrapper webSocketClient;
    private boolean isConnected = false;
    private boolean isAuthenticated = false;
    private boolean isAuthenticating = false; // 新增状态：正在认证中
    private final Gson gson = new Gson();
    
    // 消息处理器接口
    public interface MessageHandler {
        void onMessage(String type, JsonObject data);
        void onConnected();
        void onDisconnected();
        void onError(String error);
    }
    
    private MessageHandler messageHandler;
    
    public ConnectAndReveives() {
        initializeWebSocket();
    }
    
    public void setMessageHandler(MessageHandler handler) {
        this.messageHandler = handler;
    }
    
    private void initializeWebSocket() {
        try {
            URI serverUri = new URI(SERVER_URL);
            webSocketClient = new WebSocketClientWrapper(serverUri) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    isConnected = true;
                    System.out.println("WebSocket连接已建立");
                    System.out.println("连接信息: " + handshake.getHttpStatus() + " " + handshake.getHttpStatusMessage());
                    
                    // 连接建立后立即发送认证请求
                    System.out.println("连接建立后自动尝试认证...");
                    authenticate();
                    
                    if (messageHandler != null) {
                        messageHandler.onConnected();
                    }
                }
                
                @Override
                public void onMessage(String message) {
                    System.out.println("收到原始消息: " + message);
                    handleMessage(message);
                }
                
                @Override
                public void onClose(int code, String reason, boolean remote) {
                    isConnected = false;
                    isAuthenticated = false;
                    isAuthenticating = false; // 重置认证状态
                    System.out.println("WebSocket连接已关闭: " + reason + " (代码: " + code + ", 远程关闭: " + remote + ")");
                    if (messageHandler != null) {
                        messageHandler.onDisconnected();
                    }
                }
                
                @Override
                public void onError(Exception ex) {
                    System.err.println("WebSocket错误: " + ex.getMessage());
                    ex.printStackTrace();
                    if (messageHandler != null) {
                        messageHandler.onError(ex.getMessage());
                    }
                }
            };
        } catch (URISyntaxException e) {
            System.err.println("WebSocket URL语法错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 连接到WebSocket服务器
     */
    public void connect() {
        if (webSocketClient != null && !isConnected) {
            System.out.println("正在连接到WebSocket服务器...");
            System.out.println("服务器地址: " + SERVER_URL);
            webSocketClient.connect();
        } else if (isConnected) {
            System.out.println("WebSocket已经连接");
            // 如果已经连接，直接尝试认证
            authenticate();
        } else {
            System.err.println("WebSocket客户端未初始化");
        }
    }
    
    /**
     * 断开WebSocket连接
     */
    public void disconnect() {
        if (webSocketClient != null && isConnected) {
            System.out.println("正在断开WebSocket连接...");
            webSocketClient.close();
        } else {
            System.err.println("WebSocket未连接或客户端未初始化");
        }
    }
    
    /**
     * 发送认证消息
     */
    public void authenticate() {
        if (!isConnected) {
            System.err.println("WebSocket未连接，无法认证");
            return;
        }
        
        if (isAuthenticating || isAuthenticated) {
            System.err.println("已经在认证中或已认证");
            return;
        }
        
        String ircNick = IRCLoginManager.getUsername();
        String mcName = Minecraft.getInstance().getUser().getName();
        
        if (ircNick.isEmpty()) {
            System.err.println("IRC用户名为空，无法认证");
            return;
        }
        
        // 设置认证中状态
        isAuthenticating = true;
        
        JsonObject authMessage = new JsonObject();
        authMessage.addProperty("action", "auth");
        authMessage.addProperty("irc_nick", ircNick);
        authMessage.addProperty("mc_name", mcName);
        
        String authMessageStr = authMessage.toString();
        System.out.println("准备发送认证消息: " + authMessageStr);
        sendRawMessage(authMessageStr);
        System.out.println("发送认证消息完成: IRC=" + ircNick + ", MC=" + mcName);
    }
    
    /**
     * 加入聊天
     */
    public void join() {
        if (!isAuthenticated) {
            System.err.println("未认证，无法加入聊天");
            return;
        }
        
        JsonObject joinMessage = new JsonObject();
        joinMessage.addProperty("type", "join");
        
        sendRawMessage(joinMessage.toString());
        System.out.println("发送加入消息");
    }
    
    /**
     * 发送公共消息
     */
    public void sendMessage(String message) {
        if (!isAuthenticated) {
            System.err.println("未认证，无法发送消息");
            return;
        }
        
        JsonObject messageObj = new JsonObject();
        messageObj.addProperty("type", "message");
        messageObj.addProperty("message", message);
        
        sendRawMessage(messageObj.toString());
    }
    
    /**
     * 发送私聊消息
     */
    public void sendPrivateMessage(String targetUser, String message) {
        if (!isAuthenticated) {
            System.err.println("未认证，无法发送私聊消息");
            return;
        }
        
        JsonObject privateMessage = new JsonObject();
        privateMessage.addProperty("type", "private_message");
        privateMessage.addProperty("target", targetUser);
        privateMessage.addProperty("message", message);
        
        sendRawMessage(privateMessage.toString());
        System.out.println("发送私聊消息给 " + targetUser + ": " + message);
    }
    
    /**
     * 发送Minecraft命令
     */
    public void sendMinecraftCommand(String command) {
        if (!isAuthenticated) {
            System.err.println("未认证，无法发送Minecraft命令");
            return;
        }
        
        JsonObject commandMessage = new JsonObject();
        commandMessage.addProperty("type", "minecraft_command");
        commandMessage.addProperty("command", command);
        
        sendRawMessage(commandMessage.toString());
        System.out.println("发送Minecraft命令: " + command);
    }
    
    /**
     * 请求在线用户列表
     */
    public void requestUserList() {
        if (!isAuthenticated) {
            System.err.println("未认证，无法请求用户列表");
            return;
        }
        
        JsonObject listMessage = new JsonObject();
        listMessage.addProperty("type", "list_users");
        
        sendRawMessage(listMessage.toString());
        System.out.println("请求用户列表");
    }
    
    /**
     * 发送原始消息
     */
    private void sendRawMessage(String message) {
        if (webSocketClient != null && isConnected) {
            System.out.println("发送原始消息: " + message);
            webSocketClient.send(message);
            System.out.println("消息发送完成");
        } else {
            System.err.println("WebSocket未连接或客户端未初始化，无法发送消息");
            System.err.println("当前连接状态 - isConnected: " + isConnected + ", webSocketClient is null: " + (webSocketClient == null));
        }
    }
    
    /**
     * 处理接收到的消息
     */
    private void handleMessage(String message) {
        try {
            System.out.println("开始处理消息: " + message);
            JsonObject jsonMessage = gson.fromJson(message, JsonObject.class);
            
            if (!jsonMessage.has("type")) {
                System.err.println("收到的消息缺少type字段: " + message);
                return;
            }
            
            String type = jsonMessage.get("type").getAsString();
            
            System.out.println("收到消息类型: " + type);
            
            switch (type) {
                case "auth_required":
                    System.out.println("服务器要求认证");
                    authenticate();
                    break;
                    
                case "auth_success":
                    // 只有收到服务器确认后才设置为认证成功
                    isAuthenticated = true;
                    isAuthenticating = false;
                    System.out.println("认证成功");
                    join(); // 认证成功后自动加入
                    break;
                    
                case "auth_failed":
                    // 认证失败，重置状态
                    isAuthenticated = false;
                    isAuthenticating = false;
                    String errorMessage = jsonMessage.has("message") ? jsonMessage.get("message").getAsString() : "认证失败";
                    System.err.println("认证失败: " + errorMessage);
                    break;
                    
                case "welcome":
                    System.out.println("收到欢迎消息");
                    break;
                    
                case "user_joined":
                    String joinedUser = jsonMessage.get("username").getAsString();
                    System.out.println("用户 " + joinedUser + " 加入了聊天");
                    break;
                    
                case "user_left":
                    String leftUser = jsonMessage.get("username").getAsString();
                    System.out.println("用户 " + leftUser + " 离开了聊天");
                    break;
                    
                case "message":
                    String username = jsonMessage.get("username").getAsString();
                    String chatMessage = jsonMessage.get("message").getAsString();
                    System.out.println("[" + username + "] " + chatMessage );
                    break;
                    
                case "private_message":
                    String fromUser = jsonMessage.get("from").getAsString();
                    String privateMsg = jsonMessage.get("message").getAsString();
                    System.out.println("[私聊] " + fromUser + ": " + privateMsg);
                    break;
                    
                case "private_message_sent":
                    String toUser = jsonMessage.get("to").getAsString();
                    String sentMsg = jsonMessage.get("message").getAsString();
                    System.out.println("私聊消息已发送给 " + toUser + ": " + sentMsg);
                    break;
                    
                case "minecraft_command":
                    String commandUser = jsonMessage.get("username").getAsString();
                    String command = jsonMessage.get("command").getAsString();
                    System.out.println("[命令] " + commandUser + ": " + command);
                    break;
                    
                case "user_list":
                    System.out.println("收到用户列表响应");
                    if (jsonMessage.has("users")) {
                        System.out.println("在线用户: " + jsonMessage.get("users").toString());
                    }
                    break;
                    
                case "error":
                    String errorMsg = jsonMessage.has("message") ? jsonMessage.get("message").getAsString() : "未知错误";
                    System.err.println("服务器错误: " + errorMsg);
                    break;
                    
                default:
                    // 忽略未知消息类型，不输出日志
                    break;
            }
            
            // 通知消息处理器
            if (messageHandler != null) {
                messageHandler.onMessage(type, jsonMessage);
            }
            
        } catch (Exception e) {
            System.err.println("处理消息时出错: " + e.getMessage());
            System.err.println("原始消息内容: " + message);
            e.printStackTrace();
        }
    }
    
    /**
     * 获取连接状态
     */
    public boolean isConnected() {
        return isConnected;
    }
    
    /**
     * 获取认证状态
     */
    public boolean isAuthenticated() {
        return isAuthenticated;
    }
    
    /**
     * 获取是否正在认证状态
     */
    public boolean isAuthenticating() {
        return isAuthenticating;
    }
    
    /**
     * 获取服务器地址
     */
    public String getServerAddress() {
        return SERVER_URL;
    }
}