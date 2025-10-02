package com.heypixel.heypixelmod.obsoverlay.IRCModules;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class IRCPlayerManager {
    private static final IRCPlayerManager INSTANCE = new IRCPlayerManager();
    private final Map<String, IRCPlayer> players = new ConcurrentHashMap<>();
    
    public static class IRCPlayer {
        private final String ircNick;
        private final String mcName;
        private final String rank;
        
        public IRCPlayer(String ircNick, String mcName, String rank) {
            this.ircNick = ircNick;
            this.mcName = mcName;
            this.rank = rank != null ? rank : "User";
        }
        
        public String getIrcNick() {
            return ircNick;
        }
        
        public String getMcName() {
            return mcName;
        }
        
        public String getRank() {
            return rank;
        }
        
        public String getFormattedRank() {
            String rankUpper = rank.toUpperCase();
            switch (rankUpper) {
                case "ADMIN":
                case "ADMINISTRATOR":
                    return "§c" + rankUpper;
                case "MOD":
                case "MODERATOR":
                    return "§9" + rankUpper;
                case "VIP":
                    return "§6" + rankUpper;
                case "PREMIUM":
                    return "§e" + rankUpper;
                default:
                    return "§7" + rankUpper;
            }
        }
        
        public String getTabPrefix() {
            return "§b[Naven-XD]§r[" + getFormattedRank() + "§r]§b[" + ircNick + "]§r";
        }
    }
    
    private IRCPlayerManager() {}
    
    public static IRCPlayerManager getInstance() {
        return INSTANCE;
    }
    
    public void addPlayer(String ircNick, String mcName, String rank) {
        if (mcName != null && !mcName.isEmpty()) {
            players.put(mcName, new IRCPlayer(ircNick, mcName, rank));
            System.out.println("IRC玩家已添加: IRC=" + ircNick + ", MC=" + mcName + ", Rank=" + rank);
        }
    }
    
    public void removePlayer(String mcName) {
        if (mcName != null) {
            IRCPlayer removed = players.remove(mcName);
            if (removed != null) {
                System.out.println("IRC玩家已移除: MC=" + mcName);
            }
        }
    }
    
    public IRCPlayer getPlayer(String mcName) {
        return mcName != null ? players.get(mcName) : null;
    }
    
    public boolean isIRCPlayer(String mcName) {
        return mcName != null && players.containsKey(mcName);
    }
    
    public Set<String> getAllMcNames() {
        return players.keySet();
    }
    
    public void clear() {
        players.clear();
        System.out.println("IRC玩家列表已清空");
    }
    
    public int getPlayerCount() {
        return players.size();
    }
    
    public void loadPlayersFromJson(JsonArray playersArray) {
        if (playersArray == null) return;
        
        for (int i = 0; i < playersArray.size(); i++) {
            JsonObject playerObj = playersArray.get(i).getAsJsonObject();
            String ircNick = playerObj.has("irc_nick") ? playerObj.get("irc_nick").getAsString() : "";
            String mcName = playerObj.has("mc_name") ? playerObj.get("mc_name").getAsString() : "";
            String rank = playerObj.has("rank") ? playerObj.get("rank").getAsString() : "User";
            
            if (!ircNick.isEmpty() && !mcName.isEmpty()) {
                addPlayer(ircNick, mcName, rank);
            }
        }
    }
}

