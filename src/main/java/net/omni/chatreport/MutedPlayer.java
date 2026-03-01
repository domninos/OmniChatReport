package net.omni.chatreport;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public record MutedPlayer(String issuer, String playerName, long expiry, String reason, String fromServer) {

    public String getFromServer() {
        return fromServer;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getPlayerName() {
        return playerName;
    }

    public long getExpiry() {
        return expiry;
    }

    public String getReason() {
        return reason;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(playerName);
    }
}
