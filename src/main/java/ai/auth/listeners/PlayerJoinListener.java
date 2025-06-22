package ai.auth.listeners;

import ai.auth.AuthPlugin;
import ai.auth.utils.getPremiumUUIDFromDB;
import ai.auth.utils.isPremium;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getLogger;

public class PlayerJoinListener implements Listener {
    private final AuthPlugin plugin;
    private final isPremium premiumChecker;
    private final getPremiumUUIDFromDB getPremiumUUID;

    public PlayerJoinListener(AuthPlugin plugin) {
        this.plugin = plugin;
        this.premiumChecker = new isPremium(plugin);
        this.getPremiumUUID = new getPremiumUUIDFromDB(plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String ip = player.getAddress().getAddress().getHostAddress();
        String nick = player.getName();
        UUID expectedUUID = getPremiumUUID.getPremiumUUIDFromDB(nick);

        if (expectedUUID != null && !expectedUUID.equals(uuid)) {
            player.kickPlayer(ChatColor.RED + "To konto jest premium. Użyj launchera Mojang.");
            return;
        }
        try (Connection conn =plugin.dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT uuid FROM premium_status WHERE is_premium = TRUE AND LOWER(username) = LOWER(?)"
             )) {
            stmt.setString(1, nick.toLowerCase());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                UUID premiumUuid = UUID.fromString(rs.getString("uuid"));
                if (!premiumUuid.equals(uuid)) {
                    player.kickPlayer(ChatColor.RED + "Ten nick należy do konta premium. Wejdź przez konto Mojang.");
                    return;
                }
            }
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Błąd przy sprawdzaniu podszycia", e);
            player.kickPlayer(ChatColor.RED + "Błąd weryfikacji premium.");
            return;
        }

        if (premiumChecker.isPremium(uuid)) {
            plugin.premiumStatus.put(uuid, true);
            plugin.loggedIn.put(uuid, true);
            player.sendMessage(ChatColor.GREEN + "Witamy premium gracza!");
        } else {
            plugin.premiumStatus.put(uuid, false);
            try (Connection conn = plugin.dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT password, last_ip, ip_login_enabled FROM users WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    boolean ipLogin = rs.getBoolean("ip_login_enabled");
                    String lastIp = rs.getString("last_ip");
                    plugin. ipLoginEnabled.put(uuid, ipLogin);
                    if (ipLogin && lastIp != null && lastIp.equals(ip)) {
                        plugin.loggedIn.put(uuid, true);
                        player.sendMessage(ChatColor.GREEN + "Zalogowano automatycznie (IP zgodne).");
                    } else {
                        plugin.loggedIn.put(uuid, false);
                        player.sendMessage(ChatColor.YELLOW + "Zaloguj się: /login <hasło>");
                    }
                } else {
                    plugin.loggedIn.put(uuid, false);
                    player.sendMessage(ChatColor.RED + "Zarejestruj się: /register <hasło>");
                }
            } catch (SQLException e) {
                getLogger().log(Level.SEVERE, "Błąd podczas sprawdzania konta", e);
                player.sendMessage(ChatColor.RED + "Błąd bazy danych.");
            }
        }
    }
}
