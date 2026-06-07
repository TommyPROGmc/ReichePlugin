package de.kammer.reiche;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BananeManager implements Listener {
    private final Reiche plugin;
    private final Map<String, Item> bananen = new HashMap<>();
    private final Map<String, BukkitRunnable> respawnTasks = new HashMap<>();

    public BananeManager(Reiche plugin) {
        this.plugin = plugin;
    }

    public void spawnBanane(Reich reich) {
        Location loc = reich.getSpawn().add(0.5, 1, 0.5);

        ItemStack banane = new ItemStack(Material.BANANA);
        ItemMeta meta = banane.getItemMeta();
        meta.setDisplayName("§6Kronjuwel von " + reich.getName());
        banane.setItemMeta(meta);

        Item item = loc.getWorld().dropItem(loc, banane);
        item.setPickupDelay(0);
        bananen.put(reich.getName(), item);
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent event) {
        Item item = event.getItem();
        ItemStack stack = item.getItemStack();

        if (stack.getType()!= Material.BANANA || stack.getItemMeta() == null) return;
        String displayName = stack.getItemMeta().getDisplayName();

        for (String reichName : plugin.getReichManager().getReiche().keySet()) {
            if (displayName.contains(reichName)) {
                Reich reich = plugin.getReichManager().getReiche().get(reichName);
                UUID uuid = event.getPlayer().getUniqueId();

                String spielerReich = plugin.getReichManager().getSpielerReich().get(uuid);
                if (spielerReich == null || spielerReich.equals(reichName)) {
                    event.setCancelled(true);
                    return;
                }

                if (!reich.getLeader().equals(uuid)) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage("§cNur der gegnerische Leader kann klauen!");
                    return;
                }

                reich.setZerfallen(true);
                bananen.remove(reichName);
                Bukkit.broadcastMessage(
                    plugin.getConfig().getString("nachricht-banane-geklaut")
                      .replace("{reich}", reichName).replace("&", "§"));
                plugin.getScoreboardManager().updateScoreboards();

                int seconds = plugin.getConfig().getInt("banane-despawn-sekunden");
                BukkitRunnable task = new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (bananen.containsKey(reichName)) return;
                        spawnBanane(reich);
                        Bukkit.broadcastMessage("§7Banane von " + reichName + " respawnt.");
                    }
                };
                task.runTaskLater(plugin, seconds * 20L);
                respawnTasks.put(reichName, task);
                break;
            }
        }
    }

    @EventHandler
    public void onInventoryMove(InventoryMoveItemEvent event) {
        ItemStack item = event.getItem();
        if (item.getType()!= Material.BANANA || item.getItemMeta() == null) return;

        String displayName = item.getItemMeta().getDisplayName();
        for (String reichName : plugin.getReichManager().getReiche().keySet()) {
            if (displayName.contains(reichName)) {
                Location dest = event.getDestination().getLocation();
                Reich reich = plugin.getReichManager().getReiche().get(reichName);
                if (dest.distance(reich.getSpawn()) < 8) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        item.setAmount(0);
                        reich.setZerfallen(false);
                        if (respawnTasks.containsKey(reichName)) {
                            respawnTasks.get(reichName).cancel();
                            respawnTasks.remove(reichName);
                        }
                        Bukkit.broadcastMessage(
                            plugin.getConfig().getString("nachricht-banane-zurueck")
                              .replace("{reich}", reichName).replace("&", "§"));
                        plugin.getScoreboardManager().updateScoreboards();
                    }, 1L);
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.BANANA) {
            event.setCancelled(true);
        }
    }
}
