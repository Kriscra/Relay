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

        sender.sendMessage(Component.text("Unknown subcommand. Usage: /relay <status|services|topics|metrics|holograms>", NamedTextColor.RED));
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
            return Arrays.asList("status", "services", "topics", "metrics", "holograms").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("metrics")) {
            return Collections.singletonList("reset");
        }
        return Collections.emptyList();
    }
}
