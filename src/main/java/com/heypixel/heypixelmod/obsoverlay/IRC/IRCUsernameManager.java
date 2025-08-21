package com.heypixel.heypixelmod.obsoverlay.IRC;

public class IRCUsernameManager {
    private static IRCUsernameManager instance;
    private String ircUsername;
    
    private IRCUsernameManager() {
        // 私有构造函数，防止外部实例化
    }
    
    public static IRCUsernameManager getInstance() {
        if (instance == null) {
            instance = new IRCUsernameManager();
        }
        return instance;
    }
    
    public void setIRCUsername(String username) {
        this.ircUsername = username;
    }
    
    public String getIRCUsername() {
        return ircUsername;
    }
    
    public boolean hasUsername() {
        return ircUsername != null && !ircUsername.isEmpty();
    }
}