package ai.auth.utils;

import ai.auth.AuthPlugin;
import org.bukkit.event.Listener;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

public class isPremium implements Listener {

    private final AuthPlugin plugin;
    public isPremium(AuthPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isPremium(UUID uuid) {
        try (Connection conn = plugin.dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT is_premium FROM premium_status WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("is_premium");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Błąd sprawdzania premium", e);
        }
        return false;
    }
}

