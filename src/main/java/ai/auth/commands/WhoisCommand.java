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

public class WhoisCommand implements CommandExecutor {

    private final AuthPlugin plugin;
    private final isPremium premiumChecker;

    public WhoisCommand(AuthPlugin plugin) {
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


        if (command.getName().equalsIgnoreCase("whois")) {
            if (!sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "Nie masz uprawnień do tej komendy.");
                return true;
            }

            if (args.length != 1) {
                sender.sendMessage(ChatColor.RED + "Użycie: /whois <nick>");
                return true;
            }

            String nick = args[0];

            try (Connection conn = plugin.dataSource.getConnection();
                 PreparedStatement premiumStmt = conn.prepareStatement("SELECT uuid, is_premium FROM premium_status WHERE LOWER(username) = LOWER(?)")) {

                premiumStmt.setString(1, nick.toLowerCase());
                ResultSet rs = premiumStmt.executeQuery();

                if (!rs.next()) {
                    sender.sendMessage(ChatColor.YELLOW + "Gracz " + nick + " nie istnieje w bazie.");
                    return true;
                }

                String targetUuid = rs.getString("uuid");
                boolean targetPremium = rs.getBoolean("is_premium");

                try (PreparedStatement userStmt = conn.prepareStatement("SELECT last_ip, password FROM users WHERE uuid = ?")) {
                    userStmt.setString(1, targetUuid);
                    ResultSet urs = userStmt.executeQuery();

                    String ip = "(brak)";
                    String pass = "(brak)";
                    if (urs.next()) {
                        ip = urs.getString("last_ip") != null ? urs.getString("last_ip") : "(brak)";
                        pass = urs.getString("password") != null ? urs.getString("password") : "(brak)";
                    }

                    sender.sendMessage(ChatColor.GOLD + "Informacje o graczu " + ChatColor.YELLOW + nick);
                    sender.sendMessage(ChatColor.GRAY + "UUID: " + ChatColor.WHITE + targetUuid);
                    sender.sendMessage(ChatColor.GRAY + "Status: " + (targetPremium ? ChatColor.GREEN + "Premium" : ChatColor.RED + "Non-premium"));
                    sender.sendMessage(ChatColor.GRAY + "Ostatnie IP: " + ChatColor.WHITE + ip);
                    sender.sendMessage(ChatColor.GRAY + "Hasło (hash): " + ChatColor.WHITE + pass);
                }

            } catch (SQLException e) {
                sender.sendMessage(ChatColor.RED + "Błąd podczas pobierania danych.");
                plugin.getLogger().log(Level.SEVERE, "Błąd podczas komendy /whois", e);
            }

            return true;
        }
        return false;
    }
}