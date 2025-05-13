package dev.felnull.mihariin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import dev.felnull.mihariin.listener.CommonListener;
import dev.felnull.mihariin.listener.GlowingCobbleListener;
import net.minecraft.server.v1_12_R1.EntitySlime;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftSlime;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

import java.util.List;

import static dev.felnull.mihariin.listener.GlowingCobbleListener.protocolManager;

public final class Mihariin extends JavaPlugin {
    public static Mihariin INSTANCE;

    @Override
    public void onEnable() {
        initListener();
        Bukkit.getLogger().info("見張り員が配置につきました! [Loaded.Mihariin]");
        INSTANCE = this;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public void initListener() {
        Bukkit.getPluginManager().registerEvents(new CommonListener(), this);
        Bukkit.getPluginManager().registerEvents(new GlowingCobbleListener(), this);
    }
    public static Mihariin getInstance() {
        return INSTANCE;
    }

    public void spawnGlowingItem(Player player, Player glowViewer, Material material) {
        ItemStack item = new ItemStack(material);
        Item dropped = player.getWorld().dropItem(player.getLocation().add(0, 1, 0), item);
        dropped.setPickupDelay(Integer.MAX_VALUE);

        // 5秒後に削除
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!dropped.isDead()) {
                    dropped.remove();
                }
            }
        }.runTaskLater(this, 100L);

        sendTeamGlow(dropped, glowViewer);
    }

    public void sendTeamGlow(Item entity, Player owner) {
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();
        // DataWatcher 初期化
        WrappedDataWatcher watcher = new WrappedDataWatcher(entity);
        WrappedDataWatcher.WrappedDataWatcherObject obj =
                new WrappedDataWatcher.WrappedDataWatcherObject(0, WrappedDataWatcher.Registry.get(Byte.class));
        watcher.setObject(obj, (byte) 0x40); // 0x40 = Glowing

        // パケット構築
        PacketContainer packet = manager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        packet.getIntegers().write(0, entity.getEntityId());
        packet.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());

        // 送信先フィルタ
        Team ownerTeam = owner.getScoreboard().getEntryTeam(owner.getName());
        ChatColor ownerColor = ownerTeam != null ? ownerTeam.getColor() : null;

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Team viewerTeam = viewer.getScoreboard().getEntryTeam(viewer.getName());
            if (viewerTeam != null && viewerTeam.getColor() == ownerColor) {
                try {
                    manager.sendServerPacket(viewer, packet);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void spawnCustomSlime(Player player, Team viewTeam) {
        Slime slime = player.getWorld().spawn(player.getLocation(), Slime.class);
        slime.setSize(1);
        slime.setSilent(true);
        slime.setInvulnerable(true); // ダメージ無効化（プレイヤー攻撃から）

        showGlowToTeamOnly(slime, viewTeam);

        // NMSでAIを無効化
        EntitySlime nmsSlime = ((CraftSlime) slime).getHandle();
        NBTTagCompound tag = new NBTTagCompound();
        nmsSlime.c(tag);
        tag.setByte("NoAI", (byte) 1);
        tag.setByte("Invisible", (byte) 1);
        nmsSlime.f(tag);

        // 5秒後に削除
        new BukkitRunnable() {
            @Override
            public void run() {
                slime.remove();
            }
        }.runTaskLater(this, 20L * 5);
    }

    public void showGlowToTeamOnly(Entity target, Team team) {
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();

        // 発光データを作成
        WrappedDataWatcher watcher = new WrappedDataWatcher();
        watcher.setObject(
                new WrappedDataWatcher.WrappedDataWatcherObject(0, WrappedDataWatcher.Registry.get(Byte.class)),
                (byte) 0x20 // 発光フラグ
        );

        // パケット作成
        PacketContainer packet = manager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        packet.getIntegers().write(0, target.getEntityId());
        packet.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());

        // チームメンバーにのみ送信
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Team viewerTeam = viewer.getScoreboard().getEntryTeam(viewer.getName());
            if (viewerTeam != null && viewerTeam.getName().equals(team.getName())) {
                try {
                    manager.sendServerPacket(viewer, packet);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // 5秒後に非表示にする
        Bukkit.getScheduler().runTaskLater(Mihariin.getInstance(), () -> {
            try {
                // 発光をオフ（flag 0x00）にする
                WrappedDataWatcher clearWatcher = new WrappedDataWatcher();
                clearWatcher.setObject(
                        new WrappedDataWatcher.WrappedDataWatcherObject(0, WrappedDataWatcher.Registry.get(Byte.class)),
                        (byte) 0x00
                );

                PacketContainer clearPacket = manager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
                clearPacket.getIntegers().write(0, target.getEntityId());
                clearPacket.getWatchableCollectionModifier().write(0, clearWatcher.getWatchableObjects());

                for (Player viewer : Bukkit.getOnlinePlayers()) {
                    Team viewerTeam = viewer.getScoreboard().getEntryTeam(viewer.getName());
                    if (viewerTeam != null && viewerTeam.getName().equals(team.getName())) {
                        manager.sendServerPacket(viewer, clearPacket);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, 100L); // 5秒後（20L = 1秒）
    }
}
