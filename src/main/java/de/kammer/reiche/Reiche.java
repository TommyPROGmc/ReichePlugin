package de.kammer.reiche;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class Reiche extends JavaPlugin {
    private static Reiche instance;
    private ReichManager reichManager;
    private BananeManager bananeManager;
    private ScoreboardManager scoreboardManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        reichManager = new ReichManager(this);
        bananeManager = new BananeManager(this);
        scoreboardManager = new ScoreboardManager(this);

        getCommand("reich").setExecutor(reichManager);
        getCommand("reich").setTabCompleter(reichManager);
        getServer().getPluginManager().registerEvents(reichManager, this);
        getServer().getPluginManager().registerEvents(bananeManager, this);

        new BukkitRunnable() {
            @Override
            public void run() {
                reichManager.updateActionbars();
                scoreboardManager.updateScoreboards();
            }
        }.runTaskTimer(this, 0L, 20L);

        getLogger().info("Reiche v1.1 aktiviert!");
    }

    public static Reiche getInstance() { return instance; }
    public ReichManager getReichManager() { return reichManager; }
    public BananeManager getBananeManager() { return bananeManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
}
