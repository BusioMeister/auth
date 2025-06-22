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

public class ChangepasswordCommand implements CommandExecutor {


    private final AuthPlugin plugin;
    private final isPremium premiumChecker;

    public ChangepasswordCommand(AuthPlugin plugin) {
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


        if (command.getName().equalsIgnoreCase("changepassword")) {
            if (isPremium) {
                player.sendMessage(ChatColor.RED + "Gracze premium nie mogą zmieniać hasła.");
                return true;
            }
            if (!isLogged) {
                player.sendMessage(ChatColor.RED + "Musisz być zalogowany, aby zmienić hasło.");
                return true;
            }

            if (args.length != 2) {
                player.sendMessage(ChatColor.RED + "Użycie: /changepassword <stare_haslo> <nowe_haslo>");
                return true;
            }

            String oldPass = args[0];
            String newPass = args[1];

            try (Connection conn = plugin.dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT password FROM users WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                if (!rs.next()) {
                    player.sendMessage(ChatColor.RED + "Nie znaleziono Twojego konta!");
                    return true;
                }

                String currentHash = rs.getString("password");
                if (!BCrypt.checkpw(oldPass, currentHash)) {
                    player.sendMessage(ChatColor.RED + "Stare hasło jest niepoprawne!");
                    return true;
                }

                String newHash = BCrypt.hashpw(newPass, BCrypt.gensalt());

                try (PreparedStatement update = conn.prepareStatement("UPDATE users SET password = ? WHERE uuid = ?")) {
                    update.setString(1, newHash);
                    update.setString(2, uuid.toString());
                    update.executeUpdate();
                }

                player.sendMessage(ChatColor.GREEN + "Hasło zostało pomyślnie zmienione.");

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Błąd podczas zmiany hasła", e);
                player.sendMessage(ChatColor.RED + "Wystąpił błąd podczas zmiany hasła.");
            }
            return true;
        }
        return false;
    }
}