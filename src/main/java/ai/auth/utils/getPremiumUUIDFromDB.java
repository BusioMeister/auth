package ai.auth.utils;

import ai.auth.AuthPlugin;
import org.bukkit.event.Listener;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getLogger;

public class getPremiumUUIDFromDB implements Listener {
    private final AuthPlugin plugin;
    private final isPremium premiumChecker;



    public getPremiumUUIDFromDB(AuthPlugin plugin) {
        this.plugin = plugin;
        this.premiumChecker = new isPremium(plugin);
    }

    public UUID getPremiumUUIDFromDB(String username) {
        try (Connection conn = plugin.dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT uuid FROM premium_status WHERE is_premium = TRUE AND LOWER(username) = LOWER(?)")) {
            stmt.setString(1, username.toLowerCase());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return UUID.fromString(rs.getString("uuid"));
            }
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Błąd sprawdzania UUID premium dla nicku: " + username, e);
        }
        return null;
    }
}
