package org.vrz.relay.scoreboard;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vrz.relay.api.scheduler.SchedulerService;
import org.vrz.relay.api.scoreboard.Sidebar;
import org.vrz.relay.api.scoreboard.SidebarBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Fluent builder implementation for {@link Sidebar}.
 */
public class RelaySidebarBuilderImpl implements SidebarBuilder {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final Plugin plugin;
    private final SchedulerService scheduler;
    private final Consumer<RelaySidebarImpl> registrationCallback;
    private final Consumer<RelaySidebarImpl> unregisterCallback;

    private Component title = Component.text("Scoreboard");
    private final List<Component> animatedTitleFrames = new ArrayList<>();
    private long titleIntervalTicks = 20L;

    private final Map<Integer, Component> staticLines = new HashMap<>();
    private final Map<Integer, Supplier<Component>> dynamicSuppliers = new HashMap<>();

    private long updateIntervalTicks = 0L;
    private int priority = 0;

    public RelaySidebarBuilderImpl(
            @NotNull Plugin plugin,
            @Nullable SchedulerService scheduler,
            @Nullable Consumer<RelaySidebarImpl> registrationCallback,
            @Nullable Consumer<RelaySidebarImpl> unregisterCallback
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.scheduler = scheduler;
        this.registrationCallback = registrationCallback;
        this.unregisterCallback = unregisterCallback;
    }

    @Override
    @NotNull
    public SidebarBuilder title(@NotNull Component title) {
        this.title = Objects.requireNonNull(title, "title cannot be null");
        return this;
    }

    @Override
    @NotNull
    public SidebarBuilder titleMiniMessage(@NotNull String miniMessage) {
        Objects.requireNonNull(miniMessage, "miniMessage cannot be null");
        return title(MINI_MESSAGE.deserialize(miniMessage));
    }

    @Override
    @NotNull
    public SidebarBuilder animatedTitle(@NotNull List<Component> frames, long intervalTicks) {
        Objects.requireNonNull(frames, "frames cannot be null");
        this.animatedTitleFrames.clear();
        this.animatedTitleFrames.addAll(frames);
        this.titleIntervalTicks = Math.max(1L, intervalTicks);
        if (!frames.isEmpty()) {
            this.title = frames.get(0);
        }
        return this;
    }

    @Override
    @NotNull
    public SidebarBuilder line(int lineIndex, @NotNull Component text) {
        Objects.requireNonNull(text, "text cannot be null");
        if (lineIndex < 1 || lineIndex > 15) {
            throw new IllegalArgumentException("lineIndex must be between 1 and 15");
        }
        this.staticLines.put(lineIndex, text);
        this.dynamicSuppliers.remove(lineIndex);
        return this;
    }

    @Override
    @NotNull
    public SidebarBuilder lineMiniMessage(int lineIndex, @NotNull String miniMessage) {
        Objects.requireNonNull(miniMessage, "miniMessage cannot be null");
        return line(lineIndex, MINI_MESSAGE.deserialize(miniMessage));
    }

    @Override
    @NotNull
    public SidebarBuilder line(int lineIndex, @NotNull Supplier<Component> supplier) {
        Objects.requireNonNull(supplier, "supplier cannot be null");
        if (lineIndex < 1 || lineIndex > 15) {
            throw new IllegalArgumentException("lineIndex must be between 1 and 15");
        }
        this.dynamicSuppliers.put(lineIndex, supplier);
        Component initial = supplier.get();
        if (initial != null) {
            this.staticLines.put(lineIndex, initial);
        }
        return this;
    }

    @Override
    @NotNull
    public SidebarBuilder lines(@NotNull List<Component> lines) {
        Objects.requireNonNull(lines, "lines cannot be null");
        this.staticLines.clear();
        this.dynamicSuppliers.clear();
        int count = Math.min(lines.size(), 15);
        for (int i = 0; i < count; i++) {
            this.staticLines.put(i + 1, lines.get(i));
        }
        return this;
    }

    @Override
    @NotNull
    public SidebarBuilder linesMiniMessage(@NotNull List<String> lines) {
        Objects.requireNonNull(lines, "lines cannot be null");
        List<Component> comps = new ArrayList<>(lines.size());
        for (String line : lines) {
            comps.add(MINI_MESSAGE.deserialize(line));
        }
        return lines(comps);
    }

    @Override
    @NotNull
    public SidebarBuilder updateInterval(long intervalTicks) {
        this.updateIntervalTicks = Math.max(0L, intervalTicks);
        return this;
    }

    @Override
    @NotNull
    public SidebarBuilder priority(int priority) {
        this.priority = priority;
        return this;
    }

    @Override
    @NotNull
    public Sidebar buildAndShow(@NotNull Player player) {
        Objects.requireNonNull(player, "player cannot be null");
        RelaySidebarImpl sidebar = new RelaySidebarImpl(
                plugin,
                player,
                title,
                staticLines,
                dynamicSuppliers,
                animatedTitleFrames,
                titleIntervalTicks,
                updateIntervalTicks,
                priority,
                scheduler,
                unregisterCallback
        );

        if (registrationCallback != null) {
            registrationCallback.accept(sidebar);
        }

        sidebar.show(player);
        return sidebar;
    }

    @Override
    @NotNull
    public Sidebar build() {
        RelaySidebarImpl sidebar = new RelaySidebarImpl(
                plugin,
                null,
                title,
                staticLines,
                dynamicSuppliers,
                animatedTitleFrames,
                titleIntervalTicks,
                updateIntervalTicks,
                priority,
                scheduler,
                unregisterCallback
        );

        if (registrationCallback != null) {
            registrationCallback.accept(sidebar);
        }

        return sidebar;
    }
}
