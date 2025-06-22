package ai.auth.commands;

import ai.auth.AuthPlugin;
import ai.auth.utils.isPremium;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getLogger;

public class RegisterCommand implements CommandExecutor {

    private final AuthPlugin plugin;
    private final isPremium premiumChecker;


    public RegisterCommand(AuthPlugin plugin) {
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

        if (command.getName().equalsIgnoreCase("register")) {
            if (isPremium) {
                player.sendMessage(ChatColor.RED + "Gracze premium nie mogą się rejestrować.");
                return true;
            }

            if (args.length != 1) {
                player.sendMessage(ChatColor.RED + "Użycie: /register <hasło>");
                return true;
            }

            try (Connection conn = plugin.dataSource.getConnection();
                 PreparedStatement check = conn.prepareStatement("SELECT uuid FROM users WHERE uuid = ?")) {
                check.setString(1, uuid.toString());
                ResultSet rs = check.executeQuery();
                if (rs.next()) {
                    player.sendMessage(ChatColor.RED + "Masz już konto! Użyj /login");
                    return true;
                }

                String hash = BCrypt.hashpw(args[0], BCrypt.gensalt());
                try (PreparedStatement insert = conn.prepareStatement("INSERT INTO users (uuid, password, last_ip, ip_login_enabled) VALUES (?, ?, ?, FALSE)")) {
                    insert.setString(1, uuid.toString());
                    insert.setString(2, hash);
                    insert.setString(3, player.getAddress().getAddress().getHostAddress());
                    insert.execute();
                }

                plugin.loggedIn.put(uuid, true);
                player.sendMessage(ChatColor.GREEN + "Zarejestrowano pomyślnie!");

            } catch (SQLException e) {
                getLogger().log(Level.SEVERE, "Błąd przy rejestracji", e);
                player.sendMessage(ChatColor.RED + "Błąd rejestracji.");
            }
            return true;
        }
        return false;
    }
}