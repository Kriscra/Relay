package org.vrz.relay;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.vrz.relay.api.RelayAPI;
import org.vrz.relay.api.cooldown.CooldownService;
import org.vrz.relay.api.data.SharedDataService;
import org.vrz.relay.api.service.ServicePriority;
import org.vrz.relay.command.RelayCommand;
import org.vrz.relay.cooldown.RelayCooldownServiceImpl;
import org.vrz.relay.data.RelaySharedDataServiceImpl;
import org.vrz.relay.messenger.RelayMessengerImpl;
import org.vrz.relay.service.RelayServiceRegistryImpl;

import java.util.Objects;

/**
 * Main Paper/Spigot plugin entry point for Relay.
 */
public final class Relay extends JavaPlugin implements Listener {

    private RelayServiceRegistryImpl serviceRegistry;
    private RelayMessengerImpl messenger;
    private RelayCooldownServiceImpl cooldownService;
    private RelaySharedDataServiceImpl dataService;
    private org.vrz.relay.party.DefaultPartyServiceImpl partyService;
    private org.vrz.relay.metric.RelayMetricsServiceImpl metricsService;
    private org.vrz.relay.hologram.RelayHologramServiceImpl hologramService;
    private org.vrz.relay.menu.RelayMenuServiceImpl menuService;
    private org.vrz.relay.notification.RelayNotificationServiceImpl notificationService;
    private org.vrz.relay.scheduler.RelaySchedulerServiceImpl schedulerService;

    @Override
    public void onLoad() {
        // Initialize early in onLoad (load: STARTUP) so dependants have immediate access
        this.serviceRegistry = new RelayServiceRegistryImpl();
        this.messenger = new RelayMessengerImpl();
        this.cooldownService = new RelayCooldownServiceImpl();
        this.dataService = new RelaySharedDataServiceImpl();
        this.partyService = new org.vrz.relay.party.DefaultPartyServiceImpl();
        this.metricsService = new org.vrz.relay.metric.RelayMetricsServiceImpl();
        this.hologramService = new org.vrz.relay.hologram.RelayHologramServiceImpl();
        this.menuService = new org.vrz.relay.menu.RelayMenuServiceImpl();
        this.notificationService = new org.vrz.relay.notification.RelayNotificationServiceImpl(this);
        this.schedulerService = new org.vrz.relay.scheduler.RelaySchedulerServiceImpl();

        // Register core framework services
        this.serviceRegistry.register(CooldownService.class, this.cooldownService, ServicePriority.HIGHEST, this);
        this.serviceRegistry.register(SharedDataService.class, this.dataService, ServicePriority.HIGHEST, this);
        this.serviceRegistry.register(org.vrz.relay.api.metric.MetricsService.class, this.metricsService, ServicePriority.HIGHEST, this);
        this.serviceRegistry.register(org.vrz.relay.api.hologram.HologramService.class, this.hologramService, ServicePriority.HIGHEST, this);
        this.serviceRegistry.register(org.vrz.relay.api.menu.MenuService.class, this.menuService, ServicePriority.HIGHEST, this);
        this.serviceRegistry.register(org.vrz.relay.api.notification.NotificationService.class, this.notificationService, ServicePriority.HIGHEST, this);
        this.serviceRegistry.register(org.vrz.relay.api.scheduler.SchedulerService.class, this.schedulerService, ServicePriority.HIGHEST, this);
        this.serviceRegistry.register(org.vrz.relay.api.party.PartyService.class, this.partyService, ServicePriority.FALLBACK, this);
        this.serviceRegistry.register(org.vrz.relay.api.permission.PermissionService.class, new org.vrz.relay.permission.DefaultBukkitPermissionProvider(), ServicePriority.FALLBACK, this);

        RelayAPI.setInstance(new RelayProviderImpl(this.serviceRegistry, this.messenger, this.cooldownService, this.dataService, this.metricsService, this.hologramService, this.menuService, this.notificationService, this.schedulerService));
        getLogger().info("Relay API runtime bound successfully.");
    }


    @Override
    public void onEnable() {
        // Register listeners
        getServer().getPluginManager().registerEvents(this, this);
        if (this.menuService != null) {
            getServer().getPluginManager().registerEvents(new org.vrz.relay.menu.RelayMenuListener(this.menuService), this);
        }
        if (this.notificationService != null) {
            getServer().getPluginManager().registerEvents(new org.vrz.relay.notification.RelayNotificationListener(this.notificationService), this);
        }

        // Register diagnostic commands
        RelayCommand commandHandler = new RelayCommand(this);
        Objects.requireNonNull(getCommand("relay"), "Command /relay not defined in plugin.yml")
                .setExecutor(commandHandler);
        Objects.requireNonNull(getCommand("relay"))
                .setTabCompleter(commandHandler);

        getLogger().info("========================================");
        getLogger().info(" Relay Service & Communication Framework ");
        getLogger().info(" Version: " + getPluginMeta().getVersion());
        getLogger().info(" Folia & Paper Thread-Safe API active.  ");
        getLogger().info(" Cooldown, SharedData & RPC active.     ");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down Relay framework...");

        if (this.messenger != null) {
            this.messenger.shutdown();
        }

        if (this.cooldownService != null) {
            this.cooldownService.shutdown();
        }

        if (this.dataService != null) {
            this.dataService.shutdown();
        }

        if (this.partyService != null) {
            this.partyService.clearAll();
        }

        if (this.metricsService != null) {
            this.metricsService.reset();
        }

        if (this.hologramService != null) {
            this.hologramService.deleteAll();
        }

        if (this.menuService != null) {
            this.menuService.closeAll();
        }

        if (this.notificationService != null) {
            this.notificationService.shutdown();
        }

        if (this.schedulerService != null) {
            this.schedulerService.shutdown();
        }

        RelayAPI.clearInstance();
        getLogger().info("Relay successfully unhooked and terminated.");
    }

    /**
     * Automatic lifecycle cleanup: When any plugin disables, remove its registered services,
     * messenger subscriptions, active cooldowns, shared data, holograms and menus to prevent memory leaks.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin().equals(this)) {
            return;
        }

        if (this.serviceRegistry != null) {
            this.serviceRegistry.unregisterAll(event.getPlugin());
        }

        if (this.messenger != null) {
            this.messenger.unsubscribeAll(event.getPlugin());
        }

        if (this.cooldownService != null) {
            this.cooldownService.clearAllCooldowns(event.getPlugin());
        }

        if (this.dataService != null) {
            this.dataService.clearAll(event.getPlugin());
        }

        if (this.hologramService != null) {
            this.hologramService.deleteAll(event.getPlugin());
        }

        if (this.menuService != null) {
            this.menuService.closeAll(event.getPlugin());
        }

        if (this.notificationService != null) {
            this.notificationService.clearAll(event.getPlugin());
        }

        if (this.schedulerService != null) {
            this.schedulerService.cancelAll(event.getPlugin());
        }
    }

    @NotNull
    public RelayServiceRegistryImpl getServiceRegistry() {
        return serviceRegistry;
    }

    @NotNull
    public RelayMessengerImpl getMessenger() {
        return messenger;
    }

    @NotNull
    public RelayCooldownServiceImpl getCooldownService() {
        return cooldownService;
    }

    @NotNull
    public RelaySharedDataServiceImpl getDataService() {
        return dataService;
    }

    @NotNull
    public org.vrz.relay.party.DefaultPartyServiceImpl getPartyService() {
        return partyService;
    }

    @NotNull
    public org.vrz.relay.metric.RelayMetricsServiceImpl getMetricsService() {
        return metricsService;
    }

    @NotNull
    public org.vrz.relay.hologram.RelayHologramServiceImpl getHologramService() {
        return hologramService;
    }

    @NotNull
    public org.vrz.relay.menu.RelayMenuServiceImpl getMenuService() {
        return menuService;
    }

    @NotNull
    public org.vrz.relay.notification.RelayNotificationServiceImpl getNotificationService() {
        return notificationService;
    }

    @NotNull
    public org.vrz.relay.scheduler.RelaySchedulerServiceImpl getSchedulerService() {
        return schedulerService;
    }
}

