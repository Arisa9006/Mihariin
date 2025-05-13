package dev.felnull.mihariin.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import dev.felnull.mihariin.Mihariin;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CommonListener implements Listener {
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    @EventHandler
    public void onClickCarrotStick(PlayerInteractEvent e) {

        if(e.getItem() != null && e.getItem().getType() == Material.CARROT_STICK && e.getAction() == Action.RIGHT_CLICK_AIR) {
            Player targetPlayer = getTargetPlayerNoWall(e.getPlayer(), 100);
            if(targetPlayer != null){
                tryUse(e.getPlayer(), targetPlayer);
            }
        }
    }

    public Player getTargetPlayerNoWall(Player viewer, double maxDistance) {
        Location eye = viewer.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        World world = viewer.getWorld();

        for (double i = 0; i <= maxDistance; i += 0.3) {
            Location checkLoc = eye.clone().add(direction.clone().multiply(i));

            // 壁があるか確認（遮蔽ブロックがあれば中断）
            if (checkLoc.getBlock().getType().isSolid()) {
                return null;
            }

            // 周囲のエンティティを確認
            Collection<Entity> nearby = world.getNearbyEntities(checkLoc, 0.5, 0.5, 0.5);
            Team team = viewer.getScoreboard().getEntryTeam(viewer.getName());
            for (Entity entity : nearby) {
                if (entity instanceof Player && entity != viewer && !team.getName().equals(((Player) entity).getScoreboard().getEntryTeam(entity.getName()).getName())) {
                    return (Player) entity; // 視線上で壁越しでないプレイヤー
                }
            }
        }

        return null; // プレイヤーはいなかった or 壁があった
    }

    public void tryUse(Player player, Player targetPlayer) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (cooldowns.containsKey(uuid)) {
            long lastUse = cooldowns.get(uuid);
            long elapsed = now - lastUse;

            if (elapsed < 10_000) {
                long remaining = (10_000 - elapsed) / 1000;
                player.sendMessage("クールダウン中です" + remaining + " 秒後に再度使用可能です");
                return;
            }
        }


        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BASS, 1f, 1f);
        cooldowns.put(uuid, now);

        Team team = player.getScoreboard().getEntryTeam(player.getName());
        if (team != null) {
            //showGlowToTeamOnly(targetPlayer, team);
            switch(targetPlayer.getScoreboard().getEntryTeam(targetPlayer.getName()).getName()) {
                case "大日本帝国陸戦隊":
                    targetPlayer.sendMessage("何か視線を感じる...?");
                    break;
                case "U.S.Marines":
                    targetPlayer.sendMessage("Jesus is watching you");
                    break;
            }
            //Mihariin.INSTANCE.spawnGlowingItem(targetPlayer, player, Material.BARRIER);
            Mihariin.INSTANCE.spawnCustomSlime(targetPlayer, team);
        }
    }


}
