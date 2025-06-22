package ai.auth;

import ai.auth.commands.*;
import ai.auth.listeners.PlayerJoinListener;
import ai.auth.listeners.onPlayerChatListener;
import ai.auth.listeners.onPlayerMoveListener;
import ai.auth.managers.setupDatabase;
import ai.auth.utils.getPremiumUUIDFromDB;
import ai.auth.utils.isPremium;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;

public final class AuthPlugin extends JavaPlugin {
    private static AuthPlugin instance;  // statyczna instancja pluginu (jeśli potrzebujesz)

    public HikariDataSource dataSource;
    public HashMap<UUID, Boolean> loggedIn = new HashMap<>();
    public HashMap<UUID, Boolean> premiumStatus = new HashMap<>();
    public HashMap<UUID, Boolean> ipLoginEnabled = new HashMap<>();

    @Override
    public void onEnable() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/minecraft");
        config.setUsername("root");
        config.setPassword("root");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(3000);
        config.setIdleTimeout(60000);

        this.dataSource = new HikariDataSource(config);

        instance = this;  // ustawiamy instancję na tę stworzoną przez Bukkit
        getServer().getPluginManager().registerEvents(new getPremiumUUIDFromDB(this), this);
        getServer().getPluginManager().registerEvents(new setupDatabase(this), this);
        getServer().getPluginManager().registerEvents(new onPlayerChatListener(this), this);
        getServer().getPluginManager().registerEvents(new onPlayerMoveListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new isPremium(this), this);
        getCommand("register").setExecutor(new RegisterCommand(this));
        getCommand("login").setExecutor(new LoginCommand(this));
        getCommand("toggleiplogin").setExecutor(new ToggleiploginCommand(this));
        getCommand("whois").setExecutor(new WhoisCommand(this));
        getCommand("changepassword").setExecutor(new ChangepasswordCommand(this));
        getCommand("resetpassword").setExecutor(new ResetpasswordCommand(this));
        getLogger().info("Auth plugin włączony.");
    }
    @Override
    public void onDisable() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public static AuthPlugin getInstance() {
        return instance;
    }
}
