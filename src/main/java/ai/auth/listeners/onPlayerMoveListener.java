package ai.auth.listeners;

import ai.auth.AuthPlugin;
import ai.auth.utils.getPremiumUUIDFromDB;
import ai.auth.utils.isPremium;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class onPlayerMoveListener implements Listener {

    private final AuthPlugin plugin;
    private final isPremium premiumChecker;
    private final getPremiumUUIDFromDB getPremiumUUID;

    public onPlayerMoveListener(AuthPlugin plugin) {
        this.plugin = plugin;
        this.premiumChecker = new isPremium(plugin);
        this.getPremiumUUID = new getPremiumUUIDFromDB(plugin);
    }



    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String nick = player.getName();

        boolean isOfflineUUID = uuid.equals(UUID.nameUUIDFromBytes(("OfflinePlayer:" + nick).getBytes(StandardCharsets.UTF_8)));

        if (premiumChecker.isPremium(uuid) && isOfflineUUID) {
            player.kickPlayer(ChatColor.RED + "To konto jest premium. Wejdź przez launcher Mojang.");
            return;
        }
        if (!plugin.loggedIn.getOrDefault(player.getUniqueId(), false)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Zaloguj się najpierw!");
        }
    }
}
