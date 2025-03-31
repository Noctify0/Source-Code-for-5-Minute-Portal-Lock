package com.noctify.FiveMinutePortalLock;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCreatePortalEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import java.util.HashSet;
import java.util.Set;

public class FiveMinutePortalLock extends JavaPlugin implements Listener {
    private static final int LOCK_DURATION = 5 * 60; // 5 minutes in seconds
    private BossBar bossBar;
    private final Set<Location> portalLocations = new HashSet<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        initializePortalLocations();
    }

    private void initializePortalLocations() {
        World endWorld = Bukkit.getWorld("world_the_end");
        if (endWorld != null) {
            int[][] coordinates = {
                    {2, 60, -1}, {2, 60, 0}, {2, 60, 1}, {1, 60, 2}, {1, 60, 1}, {1, 60, 0},
                    {1, 60, -1}, {1, 60, -2}, {0, 60, -2}, {0, 60, -1}, {0, 60, 1}, {0, 60, 2},
                    {-1, 60, -2}, {-1, 60, -1}, {-1, 60, 0}, {-1, 60, 1}, {-1, 60, 2},
                    {-2, 60, -1}, {-2, 60, 0}, {-2, 60, 1}
            };
            for (int[] coord : coordinates) {
                portalLocations.add(new Location(endWorld, coord[0], coord[1], coord[2]));
            }
        } else {
            Bukkit.getLogger().severe("Could not find world_the_end! Portal locking may not work correctly.");
        }
    }

    @EventHandler
    public void onDragonDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof EnderDragon) {
            Bukkit.getLogger().info("Ender Dragon killed! Preventing portal spawn.");
            preventPortalSpawn();
            sendTitleToEndPlayers();
            startBossBarCountdown();
        }
    }

    @EventHandler
    public void onPortalCreate(EntityCreatePortalEvent event) {
        if (event.getEntity() instanceof EnderDragon) {
            event.setCancelled(true);
        }
    }

    private void preventPortalSpawn() {
        new BukkitRunnable() {
            int timeLeft = LOCK_DURATION;

            @Override
            public void run() {
                if (timeLeft <= 0) {
                    restorePortal();
                    cancel();
                    return;
                }

                for (Location loc : portalLocations) {
                    if (loc.getBlock().getType() == Material.END_PORTAL || loc.getBlock().getType() == Material.AIR || loc.getBlock().getType() == Material.RED_STAINED_GLASS) {
                        loc.getBlock().setType(Material.RED_STAINED_GLASS);
                    }
                }

                timeLeft--;
            }
        }.runTaskTimer(this, 0L, 5L); // Run every 5 ticks (0.25 seconds)
    }

    private void restorePortal() {
        for (Location loc : portalLocations) {
            loc.getBlock().setType(Material.END_PORTAL);
        }
        if (bossBar != null) bossBar.removeAll();
        Bukkit.getLogger().info("Portal restored!");
    }

    private void sendTitleToEndPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getName().equals("world_the_end")) {
                player.sendTitle("§c§lDragon Killed", "§fThe portal is locked for 5 Minutes", 10, 100, 20);
            }
        }
    }

    private void startBossBarCountdown() {
        bossBar = Bukkit.createBossBar("§fPortal Unlocks In: §c§l5:00", BarColor.WHITE, BarStyle.SEGMENTED_12);
        bossBar.setProgress(1.0);

        new BukkitRunnable() {
            int timeLeft = LOCK_DURATION;

            @Override
            public void run() {
                if (timeLeft <= 0) {
                    bossBar.removeAll();
                    cancel();
                    return;
                }

                double progress = (double) timeLeft / LOCK_DURATION;
                int minutes = timeLeft / 60;
                int seconds = timeLeft % 60;
                String timeString = String.format("§fPortal Unlocks In: §c§l%d:%02d", minutes, seconds);
                bossBar.setTitle(timeString);
                bossBar.setProgress(progress);

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getWorld().getName().equals("world_the_end")) {
                        bossBar.addPlayer(player);
                    } else {
                        bossBar.removePlayer(player);
                    }
                }

                timeLeft--;
            }
        }.runTaskTimer(this, 0L, 20L);
    }
}