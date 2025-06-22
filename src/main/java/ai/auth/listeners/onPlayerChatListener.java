package ai.auth.listeners;

import ai.auth.AuthPlugin;
import ai.auth.utils.getPremiumUUIDFromDB;
import ai.auth.utils.isPremium;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class onPlayerChatListener implements Listener {

    private final AuthPlugin plugin;
    private final isPremium premiumChecker;
    private final getPremiumUUIDFromDB getPremiumUUID;

    public onPlayerChatListener(AuthPlugin plugin) {
        this.plugin = plugin;
        this.premiumChecker = new isPremium(plugin);
        this.getPremiumUUID = new getPremiumUUIDFromDB(plugin);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.loggedIn.getOrDefault(player.getUniqueId(), false)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Nie możesz pisać przed logowaniem!");
        }
    }
}
