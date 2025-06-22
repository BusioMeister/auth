package ai.auth.commands;

import ai.auth.AuthPlugin;
import ai.auth.utils.isPremium;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getLogger;

public class ToggleiploginCommand implements CommandExecutor {
    private final AuthPlugin plugin;
    private final isPremium premiumChecker;

    public ToggleiploginCommand(AuthPlugin plugin) {
        this.plugin = plugin;
        this.premiumChecker = new isPremium(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;

        Player player = (Player) sender;

        UUID uuid = player.getUniqueId();

        boolean isPremium = premiumChecker.isPremium(uuid);

        boolean isLogged = plugin.loggedIn.getOrDefault(uuid, false);


        if (command.getName().equalsIgnoreCase("toggleiplogin")) {
            if (isPremium) {
                player.sendMessage(ChatColor.RED + "Gracze premium nie potrzebują tej funkcji.");
                return true;
            }

            if (!isLogged) {
                player.sendMessage(ChatColor.RED + "Musisz być zalogowany, aby użyć tej komendy.");
                return true;
            }

            try (Connection conn = plugin.dataSource.getConnection();
                 PreparedStatement update = conn.prepareStatement("UPDATE users SET ip_login_enabled = NOT ip_login_enabled WHERE uuid = ?")) {
                update.setString(1, uuid.toString());
                update.execute();

                boolean now = !plugin.ipLoginEnabled.getOrDefault(uuid, false);
                plugin.ipLoginEnabled.put(uuid, now);
                player.sendMessage(ChatColor.GREEN + "Auto-logowanie po IP " + (now ? "włączone." : "wyłączone."));

            } catch (SQLException e) {
                getLogger().log(Level.SEVERE, "Błąd podczas zmiany ustawień IP", e);
                player.sendMessage(ChatColor.RED + "Błąd zmiany ustawień.");
            }
            return true;
        }
        return false;
    }
}



