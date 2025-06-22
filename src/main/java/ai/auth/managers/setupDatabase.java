package ai.auth.managers;

import ai.auth.AuthPlugin;
import ai.auth.utils.isPremium;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.event.Listener;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class setupDatabase implements Listener {

    private final AuthPlugin plugin;
    private final isPremium premiumChecker;

    public setupDatabase(AuthPlugin plugin) {
        this.plugin = plugin;
        this.premiumChecker = new isPremium(plugin);
    }

    private void setupDatabase() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(plugin.getConfig().getString("database.url"));
        config.setUsername(plugin.getConfig().getString("database.username"));
        config.setPassword(plugin.getConfig().getString("database.password"));
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(3000);
        config.setIdleTimeout(60000);

        plugin.dataSource = new HikariDataSource(config);

        try (Connection connection = plugin.dataSource.getConnection()) {
            Statement stmt = connection.createStatement();

            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "password VARCHAR(60), " +
                    "last_ip VARCHAR(45), " +
                    "ip_login_enabled BOOLEAN DEFAULT FALSE)");

            stmt.execute("CREATE TABLE IF NOT EXISTS premium_status (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "username VARCHAR(16), " +
                    "is_premium BOOLEAN DEFAULT FALSE)");

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Nie udało się połączyć z bazą danych!", e);
        }
    }
}
