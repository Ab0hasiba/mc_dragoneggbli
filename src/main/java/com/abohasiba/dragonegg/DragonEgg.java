package com.abohasiba.dragonegg;

import com.abohasiba.dragonegg.listener.ContainerProtectionListener;
import com.abohasiba.dragonegg.listener.DragonEggCarrierListener;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

/**
 * Main plugin class for DragonEgg (Paper 26.1, 26.2, 26.3).
 */
public class DragonEgg extends JavaPlugin {

    private DragonEggConfig configuration;
    private HealthManager healthManager;
    private EffectManager effectManager;
    private DragonEggManager eggManager;

    private BukkitTask scheduledCheckTask;

    @Override
    public void onEnable() {
        // Save default config.yml if not present
        saveDefaultConfig();

        // Load configuration
        this.configuration = new DragonEggConfig(getConfig());

        // Initialize managers
        this.healthManager = new HealthManager(this);
        this.effectManager = new EffectManager();
        this.eggManager = new DragonEggManager(this, healthManager, effectManager, configuration);

        // Register listeners
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new DragonEggCarrierListener(this, eggManager), this);
        pm.registerEvents(new ContainerProtectionListener(this), this);

        // Register command
        PluginCommand cmd = getCommand("dragonegg");
        if (cmd != null) {
            DragonEggCommand executor = new DragonEggCommand(this);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        // Start scheduled check task
        startScheduledTask();

        getLogger().info("DragonEgg 1.0.0 enabled successfully for Paper 26.1, 26.2, and 26.3!");
    }

    @Override
    public void onDisable() {
        // Cancel repeating task
        if (scheduledCheckTask != null) {
            scheduledCheckTask.cancel();
            scheduledCheckTask = null;
        }

        // Clean up active carrier powers safely
        if (eggManager != null) {
            UUID carrierUuid = eggManager.getCurrentCarrierUuid();
            if (carrierUuid != null) {
                Player carrier = Bukkit.getPlayer(carrierUuid);
                if (carrier != null && carrier.isOnline()) {
                    healthManager.removeHealthBonus(carrier);
                    effectManager.removeEffects(carrier);
                }
            }
        }

        getLogger().info("DragonEgg disabled successfully.");
    }

    /**
     * Starts or restarts the periodic safety scan task.
     */
    private void startScheduledTask() {
        if (scheduledCheckTask != null) {
            scheduledCheckTask.cancel();
            scheduledCheckTask = null;
        }

        long interval = configuration.getCheckIntervalTicks();
        this.scheduledCheckTask = Bukkit.getScheduler().runTaskTimer(
                this,
                () -> eggManager.scanAllOnlinePlayers(),
                interval,
                interval
        );
    }

    /**
     * Reloads configuration and refreshes running systems.
     */
    public void reloadPluginConfig() {
        reloadConfig();
        long oldInterval = configuration.getCheckIntervalTicks();

        this.configuration = new DragonEggConfig(getConfig());
        eggManager.setConfig(configuration);

        // Restart task if interval changed
        if (oldInterval != configuration.getCheckIntervalTicks()) {
            startScheduledTask();
        }

        getLogger().info("DragonEgg configuration reloaded.");
    }

    public DragonEggConfig getConfiguration() {
        return configuration;
    }

    public HealthManager getHealthManager() {
        return healthManager;
    }

    public EffectManager getEffectManager() {
        return effectManager;
    }

    public DragonEggManager getEggManager() {
        return eggManager;
    }
}
