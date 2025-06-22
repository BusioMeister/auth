package ai.auth.commands;

import ai.auth.AuthPlugin;
import ai.auth.utils.isPremium;
import org.bukkit.Bukkit;
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

public class ResetpasswordCommand implements CommandExecutor {

    private final AuthPlugin plugin;
    private final isPremium premiumChecker;

    public ResetpasswordCommand(AuthPlugin plugin) {
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


        if (command.getName().equalsIgnoreCase("resetpassword")) {
            if (!sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "Nie masz uprawnień do tej komendy.");
                return true;
            }

            if (args.length != 2) {
                sender.sendMessage(ChatColor.RED + "Użycie: /resetpassword <gracz> <nowe_haslo>");
                return true;
            }

            String targetName = args[0];
            String newPass = args[1];

            try (Connection conn = plugin.dataSource.getConnection();
                 PreparedStatement select = conn.prepareStatement("SELECT uuid FROM users WHERE LOWER(uuid) IN (SELECT uuid FROM premium_status WHERE LOWER(username) = LOWER(?))")) {
                // Pobranie UUID gracza po nicku
                PreparedStatement getUUID = conn.prepareStatement("SELECT uuid FROM premium_status WHERE LOWER(username) = LOWER(?)");
                getUUID.setString(1, targetName.toLowerCase());
                ResultSet rsUUID = getUUID.executeQuery();
                if (!rsUUID.next()) {
                    sender.sendMessage(ChatColor.RED + "Nie znaleziono gracza o nicku " + targetName);
                    return true;
                }
                String targetUUID = rsUUID.getString("uuid");

                // Hashowanie nowego hasła
                String newHash = BCrypt.hashpw(newPass, BCrypt.gensalt());

                // Aktualizacja hasła w bazie
                PreparedStatement update = conn.prepareStatement("UPDATE users SET password = ? WHERE uuid = ?");
                update.setString(1, newHash);
                update.setString(2, targetUUID);
                int updated = update.executeUpdate();

                if (updated > 0) {
                    sender.sendMessage(ChatColor.GREEN + "Hasło gracza " + targetName + " zostało zresetowane.");
                    Player targetPlayer = Bukkit.getPlayer(targetUUID);
                    if (targetPlayer != null && targetPlayer.isOnline()) {
                        targetPlayer.sendMessage(ChatColor.YELLOW + "Twoje hasło zostało zresetowane przez administratora.");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Gracz nie ma konta z hasłem lub wystąpił błąd.");
                }
            } catch (SQLException e) {
                getLogger().log(Level.SEVERE, "Błąd podczas resetowania hasła", e);
                sender.sendMessage(ChatColor.RED + "Błąd podczas resetowania hasła.");
            }
            return true;
        }
        return false;
    }
}