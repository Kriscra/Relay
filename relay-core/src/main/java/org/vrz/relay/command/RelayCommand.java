package org.vrz.relay.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vrz.relay.Relay;
import org.vrz.relay.api.service.ServiceRegistration;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Administrative and diagnostic command for Relay.
 */
public class RelayCommand implements CommandExecutor, TabCompleter {

    private final Relay plugin;

    public RelayCommand(@NotNull Relay plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        if (!sender.hasPermission("relay.admin")) {
            sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            sendHeader(sender, "System Status");
            sender.sendMessage(Component.text("» Version: ", NamedTextColor.GRAY)
                    .append(Component.text(plugin.getPluginMeta().getVersion(), NamedTextColor.AQUA)));
            sender.sendMessage(Component.text("» Active Services: ", NamedTextColor.GRAY)
                    .append(Component.text(plugin.getServiceRegistry().getKnownServices().size(), NamedTextColor.GREEN)));
            sender.sendMessage(Component.text("» Active Topics: ", NamedTextColor.GRAY)
                    .append(Component.text(plugin.getMessenger().getActiveTopics().size(), NamedTextColor.GREEN)));
            sender.sendMessage(Component.text("» Active Holograms: ", NamedTextColor.GRAY)
                    .append(Component.text(plugin.getHologramService().getActiveHologramCount(), NamedTextColor.GREEN)));
            sender.sendMessage(Component.text("» Active Menus: ", NamedTextColor.GRAY)
                    .append(Component.text(plugin.getMenuService().getActiveMenus().size(), NamedTextColor.GREEN)));
            sender.sendMessage(Component.text("» Active Tasks: ", NamedTextColor.GRAY)
                    .append(Component.text(plugin.getSchedulerService().getActiveTaskCount(), NamedTextColor.GREEN)));
            sender.sendMessage(Component.text("» Runtime: ", NamedTextColor.GRAY)
                    .append(Component.text(Runtime.version().toString(), NamedTextColor.WHITE)));
            return true;
        }

        if (args[0].equalsIgnoreCase("services")) {
            sendHeader(sender, "Registered Services");
            Set<Class<?>> services = plugin.getServiceRegistry().getKnownServices();
            if (services.isEmpty()) {
                sender.sendMessage(Component.text("No services registered yet.", NamedTextColor.DARK_GRAY));
                return true;
            }

            for (Class<?> svc : services) {
                Collection<?> regs = plugin.getServiceRegistry().getRegistrations((Class) svc);
                sender.sendMessage(Component.text("• " + svc.getSimpleName(), NamedTextColor.GOLD)
                        .append(Component.text(" (" + regs.size() + " providers)", NamedTextColor.DARK_GRAY)));

                for (Object obj : regs) {
                    ServiceRegistration<?> reg = (ServiceRegistration<?>) obj;
                    sender.sendMessage(Component.text("    ↳ ", NamedTextColor.DARK_GRAY)
                            .append(Component.text(reg.getPlugin().getName(), NamedTextColor.AQUA))
                            .append(Component.text(" [Priority: " + reg.getPriority().name() + "]", NamedTextColor.YELLOW))
                            .append(Component.text(" -> " + reg.getProvider().getClass().getSimpleName(), NamedTextColor.GRAY)));
                }
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("topics")) {
            sendHeader(sender, "Active Pub/Sub Topics");
            Set<String> topics = plugin.getMessenger().getActiveTopics();
            if (topics.isEmpty()) {
                sender.sendMessage(Component.text("No active messenger topics.", NamedTextColor.DARK_GRAY));
                return true;
            }

            for (String topic : topics) {
                int count = plugin.getMessenger().getSubscriberCount(topic);
                sender.sendMessage(Component.text("• ", NamedTextColor.DARK_AQUA)
                        .append(Component.text(topic, NamedTextColor.WHITE))
                        .append(Component.text(" (" + count + " subscribers)", NamedTextColor.GRAY)));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("metrics")) {
            if (args.length > 1 && args[1].equalsIgnoreCase("reset")) {
                plugin.getMetricsService().reset();
                sender.sendMessage(Component.text("✔ All Relay metrics and performance counters have been reset.", NamedTextColor.GREEN));
                return true;
            }

            sendHeader(sender, "Performance Metrics & Telemetry");
            Map<String, org.vrz.relay.api.metric.MetricSnapshot> snapshots = plugin.getMetricsService().getAllSnapshots();
            Map<String, Long> counters = plugin.getMetricsService().getAllCounters();
            Map<String, Double> gauges = plugin.getMetricsService().getAllGauges();

            if (snapshots.isEmpty() && counters.isEmpty() && gauges.isEmpty()) {
                sender.sendMessage(Component.text("No metrics recorded yet.", NamedTextColor.DARK_GRAY));
                return true;
            }

            if (!snapshots.isEmpty()) {
                sender.sendMessage(Component.text("» Execution Timers:", NamedTextColor.GOLD));
                for (org.vrz.relay.api.metric.MetricSnapshot snap : snapshots.values()) {
                    sender.sendMessage(Component.text("  • " + snap.name() + ": ", NamedTextColor.AQUA)
                            .append(Component.text(String.format("avg: %.2fms", snap.averageMillis()), NamedTextColor.GREEN))
                            .append(Component.text(String.format(" | p95: %.2fms", snap.p95Millis()), NamedTextColor.YELLOW))
                            .append(Component.text(String.format(" | max: %.2fms", snap.maxMillis()), NamedTextColor.RED))
                            .append(Component.text(" (" + snap.count() + " calls)", NamedTextColor.DARK_GRAY)));
                }
            }

            if (!counters.isEmpty()) {
                sender.sendMessage(Component.text("» Counters:", NamedTextColor.LIGHT_PURPLE));
                for (Map.Entry<String, Long> entry : counters.entrySet()) {
                    sender.sendMessage(Component.text("  • " + entry.getKey() + ": ", NamedTextColor.WHITE)
                            .append(Component.text(entry.getValue(), NamedTextColor.GREEN)));
                }
            }

            if (!gauges.isEmpty()) {
                sender.sendMessage(Component.text("» Gauges:", NamedTextColor.BLUE));
                for (Map.Entry<String, Double> entry : gauges.entrySet()) {
                    sender.sendMessage(Component.text("  • " + entry.getKey() + ": ", NamedTextColor.WHITE)
                            .append(Component.text(String.format("%.2f", entry.getValue()), NamedTextColor.AQUA)));
                }
            }

            return true;
        }

        if (args[0].equalsIgnoreCase("holograms")) {
            sendHeader(sender, "Registered Holograms");
            Collection<org.vrz.relay.api.hologram.Hologram> holos = plugin.getHologramService().getAllHolograms();
            if (holos.isEmpty()) {
                sender.sendMessage(Component.text("No active holograms registered.", NamedTextColor.DARK_GRAY));
                return true;
            }

            for (org.vrz.relay.api.hologram.Hologram holo : holos) {
                org.bukkit.Location loc = holo.getLocation();
                String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "unknown";
                sender.sendMessage(Component.text("• " + holo.getId(), NamedTextColor.AQUA)
                        .append(Component.text(" [" + holo.getOwner().getName() + "]", NamedTextColor.YELLOW))
                        .append(Component.text(String.format(" (%s: %.1f, %.1f, %.1f)", worldName, loc.getX(), loc.getY(), loc.getZ()), NamedTextColor.GRAY))
                        .append(Component.text(" (" + holo.getLineCount() + " lines)", NamedTextColor.DARK_GRAY)));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("menus")) {
            sendHeader(sender, "Active Virtual Menus");
            Set<org.vrz.relay.api.menu.Menu> menus = plugin.getMenuService().getActiveMenus();
            if (menus.isEmpty()) {
                sender.sendMessage(Component.text("No active virtual menus registered.", NamedTextColor.DARK_GRAY));
                return true;
            }

            for (org.vrz.relay.api.menu.Menu menu : menus) {
                sender.sendMessage(Component.text("• " + menu.getId(), NamedTextColor.LIGHT_PURPLE)
                        .append(Component.text(" [" + menu.getPlugin().getName() + "]", NamedTextColor.YELLOW))
                        .append(Component.text(" (" + menu.getRows() + " rows, " + menu.getViewers().size() + " viewers)", NamedTextColor.GRAY)));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("notify")) {
            if (args.length < 3) {
                sender.sendMessage(Component.text("Usage: /relay notify <player> <actionbar|bossbar|toast|title> [message...]", NamedTextColor.RED));
                return true;
            }

            org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayerExact(args[1]);
            if (target == null || !target.isOnline()) {
                sender.sendMessage(Component.text("Player not found or offline: " + args[1], NamedTextColor.RED));
                return true;
            }

            String type = args[2].toLowerCase(Locale.ROOT);
            String rawMessage = args.length > 3
                    ? String.join(" ", Arrays.copyOfRange(args, 3, args.length))
                    : "<gradient:#4facfe:#00f2fe>Relay Notification Test</gradient>";

            switch (type) {
                case "actionbar":
                    plugin.getNotificationService().actionBar(target)
                            .messageMiniMessage(rawMessage)
                            .priority(org.vrz.relay.api.notification.NotificationPriority.HIGH)
                            .duration(java.time.Duration.ofSeconds(4))
                            .send(plugin);
                    sender.sendMessage(Component.text("✔ Sent ActionBar to " + target.getName(), NamedTextColor.GREEN));
                    break;
                case "bossbar":
                    plugin.getNotificationService().bossBar(target)
                            .titleMiniMessage(rawMessage + " <gray>(%time%s)</gray>")
                            .countdown(java.time.Duration.ofSeconds(10))
                            .color(net.kyori.adventure.bossbar.BossBar.Color.BLUE)
                            .send(plugin);
                    sender.sendMessage(Component.text("✔ Sent 10s Countdown BossBar to " + target.getName(), NamedTextColor.GREEN));
                    break;
                case "toast":
                    plugin.getNotificationService().toast(target)
                            .titleMiniMessage(rawMessage)
                            .descriptionMiniMessage("<gray>Relay Virtual Advancement</gray>")
                            .icon(org.bukkit.Material.DIAMOND)
                            .frame(org.vrz.relay.api.notification.ToastFrame.CHALLENGE)
                            .send(plugin);
                    sender.sendMessage(Component.text("✔ Dispatched Virtual Toast to " + target.getName(), NamedTextColor.GREEN));
                    break;
                case "title":
                    plugin.getNotificationService().title(target)
                            .titleMiniMessage(rawMessage)
                            .subtitleMiniMessage("<gray>Powered by Relay Notification Engine</gray>")
                            .times(java.time.Duration.ofMillis(500), java.time.Duration.ofSeconds(3), java.time.Duration.ofMillis(500))
                            .send(plugin);
                    sender.sendMessage(Component.text("✔ Sent Title to " + target.getName(), NamedTextColor.GREEN));
                    break;
                default:
                    sender.sendMessage(Component.text("Unknown notification type: " + type + ". Choose: actionbar, bossbar, toast, title", NamedTextColor.RED));
                    break;
            }
            return true;
        }

        sender.sendMessage(Component.text("Unknown subcommand. Usage: /relay <status|services|topics|metrics|holograms|menus|notify>", NamedTextColor.RED));
        return true;
    }

    private void sendHeader(@NotNull CommandSender sender, @NotNull String subtitle) {
        sender.sendMessage(Component.text("------- [ ", NamedTextColor.DARK_GRAY)
                .append(Component.text("Relay", NamedTextColor.AQUA, TextDecoration.BOLD))
                .append(Component.text(" - " + subtitle + " ] -------", NamedTextColor.DARK_GRAY)));
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      @NotNull String[] args) {
        if (!sender.hasPermission("relay.admin")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return Arrays.asList("status", "services", "topics", "metrics", "holograms", "menus", "notify").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("metrics")) {
            return Collections.singletonList("reset");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("notify")) {
            return org.bukkit.Bukkit.getOnlinePlayers().stream()
                    .map(org.bukkit.entity.Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("notify")) {
            return Arrays.asList("actionbar", "bossbar", "toast", "title").stream()
                    .filter(s -> s.startsWith(args[2].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
