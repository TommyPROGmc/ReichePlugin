package de.kammer.reiche;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

public class ScoreboardManager {
    private final Reiche plugin;

    public ScoreboardManager(Reiche plugin) {
        this.plugin = plugin;
    }

    public void updateScoreboards() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective obj = board.registerNewObjective("reiche", "dummy",
                plugin.getConfig().getString("scoreboard-titel").replace("&", "§"));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);

            int line = 15;
            for (Reich reich : plugin.getReichManager().getReiche().values()) {
                String status = reich.isZerfallen()? "§cZerfallen" : "§aAktiv";
                String text = "§6" + reich.getName() + " §7[" + reich.getSpieler().size() + "] " + status;
                obj.getScore(text).setScore(line--);
            }

            String deinReich = plugin.getReichManager().getSpielerReich().get(player.getUniqueId());
            if (deinReich!= null) {
                obj.getScore("§7").setScore(line--);
                obj.getScore("§eDein Reich: §f" + deinReich).setScore(line--);
            }

            player.setScoreboard(board);
        }
    }
}
