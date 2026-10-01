package org.vrz.relay.scoreboard;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vrz.relay.api.scheduler.RelayTask;
import org.vrz.relay.api.scheduler.SchedulerService;
import org.vrz.relay.api.scoreboard.Sidebar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Enterprise-grade, 100% flicker-free implementation of {@link Sidebar}.
 * <p>
 * Uses per-player isolated {@link Scoreboard} instances with team prefixes for zero-flicker line updates,
 * supports dynamic lambda suppliers, animated title sequences, and Folia thread-safe operations.
 */
public class RelaySidebarImpl implements Sidebar {

    private static final int MAX_LINES = 15;
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final UUID id;
    private final Plugin plugin;
    private final Player player;
    private final int priority;
    private final SchedulerService scheduler;
    private final Consumer<RelaySidebarImpl> onDestroyCallback;

    private volatile Component title;
    private final List<Component> animatedTitleFrames = new ArrayList<>();
    private volatile long titleIntervalTicks;
    private volatile int currentTitleFrameIndex = 0;

    private final Map<Integer, Component> renderedLines = new ConcurrentHashMap<>();
    private final Map<Integer, Supplier<Component>> lineSuppliers = new ConcurrentHashMap<>();

    private Scoreboard scoreboard;
    private Objective objective;
    private final Map<Integer, Team> lineTeams = new ConcurrentHashMap<>();
    private final Map<Integer, String> lineEntries = new ConcurrentHashMap<>();

    private RelayTask updateTask;
    private RelayTask titleAnimationTask;
    private volatile boolean destroyed = false;

    public RelaySidebarImpl(
            @NotNull Plugin plugin,
            @Nullable Player player,
            @NotNull Component title,
            @NotNull Map<Integer, Component> initialLines,
            @NotNull Map<Integer, Supplier<Component>> initialSuppliers,
            @Nullable List<Component> animatedTitleFrames,
            long titleIntervalTicks,
            long updateIntervalTicks,
            int priority,
            @Nullable SchedulerService scheduler,
            @Nullable Consumer<RelaySidebarImpl> onDestroyCallback
    ) {
        this.id = UUID.randomUUID();
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.player = player;
        this.title = Objects.requireNonNull(title, "title cannot be null");
        this.priority = priority;
        this.scheduler = scheduler;
        this.onDestroyCallback = onDestroyCallback;

        this.renderedLines.putAll(initialLines);
        this.lineSuppliers.putAll(initialSuppliers);

        if (animatedTitleFrames != null && !animatedTitleFrames.isEmpty()) {
            this.animatedTitleFrames.addAll(animatedTitleFrames);
            this.titleIntervalTicks = titleIntervalTicks;
        }

        initializeScoreboard();

        // Initial line rendering
        for (Map.Entry<Integer, Component> entry : initialLines.entrySet()) {
            applyLineToTeam(entry.getKey(), entry.getValue());
        }

        // Setup background update loop if interval specified
        if (updateIntervalTicks > 0 && scheduler != null && player != null) {
            this.updateTask = scheduler.runForRepeating(
                    plugin,
                    player,
                    task -> this.update(),
                    updateIntervalTicks,
                    updateIntervalTicks
            );
        }

        // Setup animated title loop if frames specified
        if (!this.animatedTitleFrames.isEmpty() && this.titleIntervalTicks > 0 && scheduler != null && player != null) {
            this.titleAnimationTask = scheduler.runForRepeating(
                    plugin,
                    player,
                    task -> this.tickTitleAnimation(),
                    this.titleIntervalTicks,
                    this.titleIntervalTicks
            );
        }
    }

    private void initializeScoreboard() {
        if (Bukkit.getServer() == null || Bukkit.getScoreboardManager() == null) {
            return; // Test environment without live Bukkit server
        }

        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.objective = this.scoreboard.registerNewObjective("relay_sb", Criteria.DUMMY, this.title);
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Try blanking score numbers if Paper 1.20.4+ blank number format is available
        tryBlankNumberFormat(this.objective);

        // Pre-create 15 dedicated teams with unique non-colliding color entries
        for (int i = 1; i <= MAX_LINES; i++) {
            String teamName = "rl_line_" + i;
            Team team = this.scoreboard.getTeam(teamName);
            if (team == null) {
                team = this.scoreboard.registerNewTeam(teamName);
            }

            // Create invisible unique entry using ChatColor codes: e.g. §1§r, §2§r...
            String entry = ChatColor.COLOR_CHAR + Integer.toHexString(i) + ChatColor.RESET;
            team.addEntry(entry);

            this.lineTeams.put(i, team);
            this.lineEntries.put(i, entry);
        }
    }

    private void tryBlankNumberFormat(Objective obj) {
        try {
            Class<?> numberFormatClass = Class.forName("io.papermc.paper.scoreboard.numbers.NumberFormat");
            java.lang.reflect.Method blankMethod = numberFormatClass.getMethod("blank");
            Object blankFormat = blankMethod.invoke(null);

            java.lang.reflect.Method numberFormatMethod = obj.getClass().getMethod("numberFormat", numberFormatClass);
            numberFormatMethod.invoke(obj, blankFormat);
        } catch (Throwable ignored) {
            // Older Bukkit/Spigot versions do not support blank number formatting
        }
    }

    public int getPriority() {
        return priority;
    }

    @Override
    @NotNull
    public UUID getId() {
        return id;
    }

    @Override
    @NotNull
    public Plugin getPlugin() {
        return plugin;
    }

    @Override
    @Nullable
    public Player getPlayer() {
        return player;
    }

    @Override
    @NotNull
    public Collection<Player> getViewers() {
        if (player != null && player.isOnline()) {
            return Collections.singletonList(player);
        }
        return Collections.emptyList();
    }

    @Override
    @NotNull
    public Component getTitle() {
        return title;
    }

    @Override
    public void setTitle(@NotNull Component title) {
        Objects.requireNonNull(title, "title cannot be null");
        this.title = title;
        runOnViewerThread(() -> {
            if (this.objective != null) {
                this.objective.displayName(title);
            }
        });
    }

    @Override
    public void setTitleMiniMessage(@NotNull String miniMessage) {
        Objects.requireNonNull(miniMessage, "miniMessage cannot be null");
        setTitle(MINI_MESSAGE.deserialize(miniMessage));
    }

    @Override
    public void setAnimatedTitle(@NotNull List<Component> frames, long intervalTicks) {
        Objects.requireNonNull(frames, "frames cannot be null");
        if (frames.isEmpty()) {
            return;
        }

        this.animatedTitleFrames.clear();
        this.animatedTitleFrames.addAll(frames);
        this.titleIntervalTicks = Math.max(1L, intervalTicks);
        this.currentTitleFrameIndex = 0;

        if (titleAnimationTask != null) {
            titleAnimationTask.cancel();
            titleAnimationTask = null;
        }

        if (scheduler != null && player != null) {
            this.titleAnimationTask = scheduler.runForRepeating(
                    plugin,
                    player,
                    task -> this.tickTitleAnimation(),
                    this.titleIntervalTicks,
                    this.titleIntervalTicks
            );
        }
    }

    private void tickTitleAnimation() {
        if (destroyed || animatedTitleFrames.isEmpty()) {
            return;
        }
        Component nextFrame = animatedTitleFrames.get(currentTitleFrameIndex % animatedTitleFrames.size());
        currentTitleFrameIndex++;
        setTitle(nextFrame);
    }

    @Override
    @NotNull
    public Map<Integer, Component> getLines() {
        return Collections.unmodifiableMap(renderedLines);
    }

    @Override
    @Nullable
    public Component getLine(int lineIndex) {
        return renderedLines.get(lineIndex);
    }

    @Override
    public void setLine(int lineIndex, @NotNull Component text) {
        Objects.requireNonNull(text, "text cannot be null");
        if (lineIndex < 1 || lineIndex > MAX_LINES) {
            throw new IllegalArgumentException("lineIndex must be between 1 and " + MAX_LINES + " (got: " + lineIndex + ")");
        }
        this.renderedLines.put(lineIndex, text);
        this.lineSuppliers.remove(lineIndex);

        runOnViewerThread(() -> applyLineToTeam(lineIndex, text));
    }

    @Override
    public void setLineMiniMessage(int lineIndex, @NotNull String miniMessage) {
        Objects.requireNonNull(miniMessage, "miniMessage cannot be null");
        setLine(lineIndex, MINI_MESSAGE.deserialize(miniMessage));
    }

    @Override
    public void setLineSupplier(int lineIndex, @NotNull Supplier<Component> supplier) {
        Objects.requireNonNull(supplier, "supplier cannot be null");
        if (lineIndex < 1 || lineIndex > MAX_LINES) {
            throw new IllegalArgumentException("lineIndex must be between 1 and " + MAX_LINES + " (got: " + lineIndex + ")");
        }
        this.lineSuppliers.put(lineIndex, supplier);

        Component initial = supplier.get();
        if (initial != null) {
            this.renderedLines.put(lineIndex, initial);
            runOnViewerThread(() -> applyLineToTeam(lineIndex, initial));
        }
    }

    @Override
    public void setLines(@NotNull List<Component> lines) {
        Objects.requireNonNull(lines, "lines cannot be null");
        clearLines();
        int count = Math.min(lines.size(), MAX_LINES);
        for (int i = 0; i < count; i++) {
            setLine(i + 1, lines.get(i));
        }
    }

    @Override
    public void setLinesMiniMessage(@NotNull List<String> lines) {
        Objects.requireNonNull(lines, "lines cannot be null");
        List<Component> comps = new ArrayList<>(lines.size());
        for (String line : lines) {
            comps.add(MINI_MESSAGE.deserialize(line));
        }
        setLines(comps);
    }

    @Override
    public void removeLine(int lineIndex) {
        renderedLines.remove(lineIndex);
        lineSuppliers.remove(lineIndex);

        runOnViewerThread(() -> {
            String entry = lineEntries.get(lineIndex);
            if (entry != null && scoreboard != null) {
                scoreboard.resetScores(entry);
            }
        });
    }

    @Override
    public void clearLines() {
        renderedLines.clear();
        lineSuppliers.clear();

        runOnViewerThread(() -> {
            if (scoreboard != null) {
                for (String entry : lineEntries.values()) {
                    scoreboard.resetScores(entry);
                }
            }
        });
    }

    private void applyLineToTeam(int lineIndex, @NotNull Component text) {
        Team team = lineTeams.get(lineIndex);
        String entry = lineEntries.get(lineIndex);

        if (team != null && entry != null && objective != null) {
            team.prefix(text);
            // Higher lines have higher score numbers so line 1 renders at the top
            int score = (MAX_LINES + 1) - lineIndex;
            objective.getScore(entry).setScore(score);
        }
    }

    @Override
    public void update() {
        if (destroyed) {
            return;
        }

        // Re-evaluate dynamic line suppliers
        boolean changed = false;
        for (Map.Entry<Integer, Supplier<Component>> entry : lineSuppliers.entrySet()) {
            int lineIndex = entry.getKey();
            try {
                Component comp = entry.getValue().get();
                if (comp != null) {
                    renderedLines.put(lineIndex, comp);
                    changed = true;
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("Error evaluating sidebar line supplier at index " + lineIndex + ": " + t.getMessage());
            }
        }

        if (changed || !renderedLines.isEmpty()) {
            runOnViewerThread(() -> {
                for (Map.Entry<Integer, Component> entry : renderedLines.entrySet()) {
                    applyLineToTeam(entry.getKey(), entry.getValue());
                }
            });
        }
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    @Override
    public void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;

        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        if (titleAnimationTask != null) {
            titleAnimationTask.cancel();
            titleAnimationTask = null;
        }

        runOnViewerThread(() -> {
            if (player != null && player.isOnline() && scoreboard != null) {
                // If player is still viewing this scoreboard, restore main scoreboard
                if (player.getScoreboard().equals(this.scoreboard)) {
                    if (Bukkit.getScoreboardManager() != null) {
                        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                    }
                }
            }

            if (objective != null) {
                try {
                    objective.unregister();
                } catch (IllegalStateException ignored) {}
                objective = null;
            }

            for (Team team : lineTeams.values()) {
                try {
                    team.unregister();
                } catch (IllegalStateException ignored) {}
            }
            lineTeams.clear();
            lineEntries.clear();
            renderedLines.clear();
            lineSuppliers.clear();
        });

        if (onDestroyCallback != null) {
            onDestroyCallback.accept(this);
        }
    }

    @Override
    public void show(@NotNull Player targetPlayer) {
        Objects.requireNonNull(targetPlayer, "targetPlayer cannot be null");
        if (destroyed || scoreboard == null) {
            return;
        }

        runOnPlayerThread(targetPlayer, () -> targetPlayer.setScoreboard(this.scoreboard));
    }

    @Override
    public void hide(@NotNull Player targetPlayer) {
        Objects.requireNonNull(targetPlayer, "targetPlayer cannot be null");
        if (scoreboard == null) {
            return;
        }

        runOnPlayerThread(targetPlayer, () -> {
            if (targetPlayer.getScoreboard().equals(this.scoreboard)) {
                if (Bukkit.getScoreboardManager() != null) {
                    targetPlayer.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                }
            }
        });
    }

    private void runOnViewerThread(@NotNull Runnable action) {
        if (player != null && player.isOnline()) {
            runOnPlayerThread(player, action);
        } else {
            action.run();
        }
    }

    private void runOnPlayerThread(@NotNull Player target, @NotNull Runnable action) {
        if (scheduler != null && Bukkit.getServer() != null) {
            scheduler.runFor(plugin, target, action);
        } else {
            action.run();
        }
    }
}
