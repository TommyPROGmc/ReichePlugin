package de.kammer.reiche;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class ReichManager implements CommandExecutor, TabCompleter, Listener {
    private final Reiche plugin;
    private final Map<String, Reich> reiche = new HashMap<>();
    private final Map<UUID, String> spielerReich = new HashMap<>();
    private final Map<UUID, Integer> leben = new HashMap<>();

    public ReichManager(Reiche plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) return false;
        if (args.length == 0) {
            player.sendMessage("§7/reich erstellen <name> | join <name> | leave | start | info");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "erstellen" -> {
                if (args.length < 2) return false;
                if (spielerReich.containsKey(player.getUniqueId())) {
                    player.sendMessage("§cDu bist schon in einem Reich!");
                    return true;
                }
                String name = args[1];
                if (reiche.containsKey(name)) {
                    player.sendMessage("§cReich existiert schon!");
                    return true;
                }
                Reich reich = new Reich(name, player.getUniqueId(), player.getLocation());
                reiche.put(name, reich);
                spielerReich.put(player.getUniqueId(), name);
                plugin.getBananeManager().spawnBanane(reich);
                player.sendMessage("§aReich §6" + name + " §aerstellt! Banane spawnt.");
            }
            case "join" -> {
                if (args.length < 2) return false;
                Reich reich = reiche.get(args[1]);
                if (reich == null) {
                    player.sendMessage("§cReich nicht gefunden!");
                    return true;
                }
                int max = plugin.getConfig().getInt("max-spieler-pro-reich");
                if (reich.getSpieler().size() >= max) {
                    player.sendMessage("§cReich ist voll! Max: " + max);
                    return true;
                }
                reich.addSpieler(player.getUniqueId());
                spielerReich.put(player.getUniqueId(), reich.getName());
                player.sendMessage("§aDu bist §6" + reich.getName() + " §abeigetreten!");
            }
            case "leave" -> {
                String reichName = spielerReich.get(player.getUniqueId());
                if (reichName == null) {
                    player.sendMessage("§cDu bist in keinem Reich!");
                    return true;
                }
                reiche.get(reichName).removeSpieler(player.getUniqueId());
                spielerReich.remove(player.getUniqueId());
                leben.remove(player.getUniqueId());
                player.sendMessage("§7Du hast das Reich verlassen.");
            }
            case "start" -> {
                String reichName = spielerReich.get(player.getUniqueId());
                if (reichName == null) return true;
                Reich reich = reiche.get(reichName);
                if (!reich.getLeader().equals(player.getUniqueId())) {
                    player.sendMessage("§cNur der Leader kann starten!");
                    return true;
                }
                int lebenAnzahl = plugin.getConfig().getInt("leben-pro-spieler");
                for (UUID uuid : reich.getSpieler()) {
                    leben.put(uuid, lebenAnzahl);
                    Player p = Bukkit.getPlayer(uuid);
                    if (p!= null) p.setGameMode(org.bukkit.GameMode.SURVIVAL);
                }
                Bukkit.broadcastMessage("§6⚔ Runde gestartet! Reich: " + reichName);
            }
            case "info" -> {
                String reichName = spielerReich.get(player.getUniqueId());
                if (reichName == null) {
                    player.sendMessage("§cDu bist in keinem Reich!");
                    return true;
                }
                Reich reich = reiche.get(reichName);
                player.sendMessage("§6=== " + reich.getName() + " ===");
                player.sendMessage("§7Leader: " + Bukkit.getOfflinePlayer(reich.getLeader()).getName());
                player.sendMessage("§7Spieler: " + reich.getSpieler().size() + "/" +
                    plugin.getConfig().getInt("max-spieler-pro-reich"));
                player.sendMessage("§7Status: " + (reich.isZerfallen()? "§cZerfallen" : "§aAktiv"));
            }
        }
        plugin.getScoreboardManager().updateScoreboards();
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("erstellen", "join", "leave", "start", "info");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            return new ArrayList<>(reiche.keySet());
        }
        return Collections.emptyList();
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();
        if (!leben.containsKey(uuid)) return;

        int lebenJetzt = leben.get(uuid) - 1;
        leben.put(uuid, lebenJetzt);

        if (lebenJetzt <= 0) {
            leben.remove(uuid);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.setGameMode(org.bukkit.GameMode.SPECTATOR);
                player.sendMessage("§c☠ Du bist tot für diese Runde!");
            }, 1L);
        } else {
            player.sendMessage("§c♥ Noch " + lebenJetzt + " Leben übrig!");
        }
        plugin.getScoreboardManager().updateScoreboards();
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        String reichName = spielerReich.get(uuid);
        if (reichName == null) return;

        Reich reich = reiche.get(reichName);
        if (reich.isZerfallen() &&!leben.containsKey(uuid)) {
            event.setRespawnLocation(reich.getSpawn());
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                event.getPlayer().setGameMode(org.bukkit.GameMode.SPECTATOR), 1L);
        }
    }

    public void updateActionbars() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if (leben.containsKey(uuid) && spielerReich.containsKey(uuid)) {
                String msg = "§c♥ " + leben.get(uuid) + " Leben | §6" + spielerReich.get(uuid);
                player.sendActionBar(Component.text(msg));
            }

            String reichName = spielerReich.get(uuid);
            if (reichName!= null) {
                Reich reich = reiche.get(reichName);
                if (reich.isZerfallen()) {
                    PotionEffectType effect = PotionEffectType.getByName(
                        plugin.getConfig().getString("zerfall-effekt"));
                    if (effect!= null) {
                        player.addPotionEffect(new PotionEffect(effect, 40, 1, true, false));
                    }
                }
            }
        }
    }

    public Map<String, Reich> getReiche() { return reiche; }
    public Map<UUID, String> getSpielerReich() { return spielerReich; }
    public Map<UUID, Integer> getLeben() { return leben; }
}

class Reich {
    private final String name;
    private final UUID leader;
    private final Location spawn;
    private final Set<UUID> spieler = new HashSet<>();
    private boolean zerfallen = false;

    public Reich(String name, UUID leader, Location spawn) {
        this.name = name;
        this.leader = leader;
        this.spawn = spawn.clone();
        this.spieler.add(leader);
    }

    public void addSpieler(UUID uuid) { spieler.add(uuid); }
    public void removeSpieler(UUID uuid) { spieler.remove(uuid); }
    public String getName() { return name; }
    public UUID getLeader() { return leader; }
    public Location getSpawn() { return spawn; }
    public Set<UUID> getSpieler() { return spieler; }
    public boolean isZerfallen() { return zerfallen; }
    public void setZerfallen(boolean z) { this.zerfallen = z; }
}
