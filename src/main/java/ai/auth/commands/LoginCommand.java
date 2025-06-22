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

public class LoginCommand implements CommandExecutor {
    private final AuthPlugin plugin;
    private final isPremium premiumChecker;

    public LoginCommand(AuthPlugin plugin) {
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


        if (command.getName().equalsIgnoreCase("login")) {
            if (isPremium) {
                player.sendMessage(ChatColor.RED + "Gracze premium są automatycznie logowani.");
                return true;
            }

            if (plugin.loggedIn.getOrDefault(uuid, false)) {
                player.sendMessage(ChatColor.YELLOW + "Już jesteś zalogowany.");
                return true;
            }

            if (args.length != 1) {
                player.sendMessage(ChatColor.RED + "Użycie: /login <hasło>");
                return true;
            }

            try (Connection conn = plugin.dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT password FROM users WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                if (!rs.next()) {
                    player.sendMessage(ChatColor.RED + "Nie znaleziono konta. Zarejestruj się!");
                    return true;
                }

                String hash = rs.getString("password");
                if (!BCrypt.checkpw(args[0], hash)) {
                    player.sendMessage(ChatColor.RED + "Błędne hasło!");
                    return true;
                }

                try (PreparedStatement update = conn.prepareStatement("UPDATE users SET last_ip = ? WHERE uuid = ?")) {
                    update.setString(1, player.getAddress().getAddress().getHostAddress());
                    update.setString(2, uuid.toString());
                    update.execute();
                }

                plugin.loggedIn.put(uuid, true);
                player.sendMessage(ChatColor.GREEN + "Zalogowano pomyślnie!");

            } catch (SQLException e) {
                getLogger().log(Level.SEVERE, "Błąd podczas logowania", e);
                player.sendMessage(ChatColor.RED + "Błąd logowania.");
            }
            return true;
        }
        return false;
    }
}
