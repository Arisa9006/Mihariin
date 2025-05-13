package dev.felnull.mihariin.listener;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import dev.felnull.mihariin.Mihariin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GlowingCobbleListener implements Listener {

    public static final ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
    private final Map<UUID, Integer> sneakTasks = new HashMap<>();

    public GlowingCobbleListener() {
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (event.isSneaking()) {
            // 6秒後に実行するタスクを登録
            int taskId = Bukkit.getScheduler().runTaskLater(Mihariin.getInstance(), () -> {
                if (player.isSneaking()) {
                    Mihariin.INSTANCE.spawnGlowingItem(player, player, Material.CHORUS_FLOWER); // まだ押してたら実行
                }
                sneakTasks.remove(uuid); // タスク消去
            }, 60L).getTaskId(); // 100L = 5秒

            sneakTasks.put(uuid, taskId);
        } else {
            if (sneakTasks.containsKey(uuid)) {
                Bukkit.getScheduler().cancelTask(sneakTasks.get(uuid));
                sneakTasks.remove(uuid);
            }
        }
    }
}
