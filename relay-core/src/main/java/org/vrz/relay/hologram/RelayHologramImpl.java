package org.vrz.relay.hologram;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vrz.relay.api.hologram.Hologram;
import org.vrz.relay.api.hologram.HologramLine;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe, Folia-ready implementation of {@link Hologram}.
 */
public class RelayHologramImpl implements Hologram {

    private final String id;
    private volatile Location location;
    private final Plugin owner;

    private final List<RelayHologramLineImpl> lines = new CopyOnWriteArrayList<>();
    private volatile double lineSpacing = 0.28;
    private volatile Display.Billboard billboard = Display.Billboard.CENTER;
    private volatile Color backgroundColor = null;
    private volatile boolean shadow = true;
    private volatile boolean seeThrough = false;
    private volatile boolean spawned = false;

    public RelayHologramImpl(@NotNull String id, @NotNull Location location, @NotNull Plugin owner) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.location = Objects.requireNonNull(location, "location cannot be null").clone();
        this.owner = Objects.requireNonNull(owner, "owner cannot be null");
    }

    @Override
    @NotNull
    public String getId() {
        return id;
    }

    @Override
    @NotNull
    public Location getLocation() {
        return location.clone();
    }

    @Override
    @NotNull
    public CompletableFuture<Void> setLocationAsync(@NotNull Location target) {
        Objects.requireNonNull(target, "target location cannot be null");
        this.location = target.clone();
        CompletableFuture<Void> future = new CompletableFuture<>();

        runSync(() -> {
            try {
                updateLinePositions();
                future.complete(null);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    @Override
    public void setLocation(@NotNull Location target) {
        setLocationAsync(target).join();
    }

    @Override
    @NotNull
    public List<HologramLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    @Override
    @NotNull
    public HologramLine getLine(int index) {
        return lines.get(index);
    }

    @Override
    @NotNull
    public HologramLine appendLine(@NotNull Component text) {
        Objects.requireNonNull(text, "text cannot be null");
        RelayHologramLineImpl line = new RelayHologramLineImpl(this, lines.size(), text);
        lines.add(line);
        if (spawned) {
            runSync(() -> spawnLineEntity(line));
        }
        return line;
    }

    @Override
    @NotNull
    public HologramLine appendLine(@NotNull String miniMessage) {
        Objects.requireNonNull(miniMessage, "miniMessage cannot be null");
        return appendLine(MiniMessage.miniMessage().deserialize(miniMessage));
    }

    @Override
    @NotNull
    public HologramLine insertLine(int index, @NotNull Component text) {
        Objects.requireNonNull(text, "text cannot be null");
        RelayHologramLineImpl line = new RelayHologramLineImpl(this, index, text);
        lines.add(index, line);
        reindexLines();
        if (spawned) {
            runSync(this::respawnAllLines);
        }
        return line;
    }

    @Override
    public void setLine(int index, @NotNull Component text) {
        lines.get(index).setText(text);
    }

    @Override
    public void removeLine(int index) {
        RelayHologramLineImpl line = lines.remove(index);
        reindexLines();
        if (spawned && line != null) {
            runSync(() -> {
                despawnLineEntity(line);
                updateLinePositions();
            });
        }
    }

    @Override
    public void clearLines() {
        if (spawned) {
            runSync(() -> {
                for (RelayHologramLineImpl line : lines) {
                    despawnLineEntity(line);
                }
                lines.clear();
            });
        } else {
            lines.clear();
        }
    }

    private void reindexLines() {
        for (int i = 0; i < lines.size(); i++) {
            lines.get(i).setIndex(i);
        }
    }

    @Override
    public int getLineCount() {
        return lines.size();
    }

    @Override
    public double getLineSpacing() {
        return lineSpacing;
    }

    @Override
    public void setLineSpacing(double spacing) {
        this.lineSpacing = spacing;
        if (spawned) {
            runSync(this::updateLinePositions);
        }
    }

    @Override
    @NotNull
    public Display.Billboard getBillboard() {
        return billboard;
    }

    @Override
    public void setBillboard(@NotNull Display.Billboard billboard) {
        this.billboard = Objects.requireNonNull(billboard, "billboard cannot be null");
        if (spawned) {
            runSync(() -> {
                for (RelayHologramLineImpl line : lines) {
                    TextDisplay entity = line.getEntity();
                    if (entity != null && entity.isValid()) {
                        entity.setBillboard(billboard);
                    }
                }
            });
        }
    }

    @Override
    @Nullable
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    @Override
    public void setBackgroundColor(@Nullable Color color) {
        this.backgroundColor = color;
        if (spawned) {
            runSync(() -> {
                for (RelayHologramLineImpl line : lines) {
                    TextDisplay entity = line.getEntity();
                    if (entity != null && entity.isValid()) {
                        entity.setBackgroundColor(color);
                        entity.setDefaultBackground(color == null);
                    }
                }
            });
        }
    }

    @Override
    public boolean hasTextShadow() {
        return shadow;
    }

    @Override
    public void setTextShadow(boolean shadow) {
        this.shadow = shadow;
        if (spawned) {
            runSync(() -> {
                for (RelayHologramLineImpl line : lines) {
                    TextDisplay entity = line.getEntity();
                    if (entity != null && entity.isValid()) {
                        entity.setShadowed(shadow);
                    }
                }
            });
        }
    }

    @Override
    public boolean isSeeThrough() {
        return seeThrough;
    }

    @Override
    public void setSeeThrough(boolean seeThrough) {
        this.seeThrough = seeThrough;
        if (spawned) {
            runSync(() -> {
                for (RelayHologramLineImpl line : lines) {
                    TextDisplay entity = line.getEntity();
                    if (entity != null && entity.isValid()) {
                        entity.setSeeThrough(seeThrough);
                    }
                }
            });
        }
    }

    @Override
    @NotNull
    public Plugin getOwner() {
        return owner;
    }

    @Override
    public boolean isSpawned() {
        return spawned;
    }

    @Override
    @NotNull
    public CompletableFuture<Void> spawnAsync() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (spawned) {
            future.complete(null);
            return future;
        }

        runSync(() -> {
            try {
                respawnAllLines();
                this.spawned = true;
                future.complete(null);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    @Override
    public void spawn() {
        spawnAsync().join();
    }

    @Override
    @NotNull
    public CompletableFuture<Void> removeAsync() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (!spawned) {
            future.complete(null);
            return future;
        }

        runSync(() -> {
            try {
                for (RelayHologramLineImpl line : lines) {
                    despawnLineEntity(line);
                }
                this.spawned = false;
                future.complete(null);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    @Override
    public void remove() {
        removeAsync().join();
    }

    private void respawnAllLines() {
        for (RelayHologramLineImpl line : lines) {
            despawnLineEntity(line);
            spawnLineEntity(line);
        }
    }

    private void spawnLineEntity(@NotNull RelayHologramLineImpl line) {
        World world = location.getWorld();
        if (world == null) return;

        Location lineLoc = location.clone().add(0, line.getVerticalOffset(), 0);

        try {
            TextDisplay display = world.spawn(lineLoc, TextDisplay.class, entity -> {
                entity.text(line.getText());
                entity.setBillboard(this.billboard);
                entity.setShadowed(this.shadow);
                entity.setSeeThrough(this.seeThrough);
                if (this.backgroundColor != null) {
                    entity.setBackgroundColor(this.backgroundColor);
                } else {
                    entity.setDefaultBackground(true);
                }
                entity.setPersistent(false); // Non-persistent avoids chunk saving issues
            });
            line.setEntity(display);
        } catch (Throwable ignored) {
            // Environment without full world mock (e.g. unit tests)
        }
    }

    private void despawnLineEntity(@NotNull RelayHologramLineImpl line) {
        TextDisplay entity = line.getEntity();
        if (entity != null) {
            try {
                if (entity.isValid()) {
                    entity.remove();
                }
            } catch (Throwable ignored) {}
            line.setEntity(null);
        }
    }

    void updateLinePositions() {
        for (RelayHologramLineImpl line : lines) {
            TextDisplay entity = line.getEntity();
            if (entity != null && entity.isValid()) {
                Location lineLoc = location.clone().add(0, line.getVerticalOffset(), 0);
                entity.teleport(lineLoc);
            }
        }
    }

    /**
     * Executes a task on the appropriate thread (Folia Region Scheduler if available, else Bukkit scheduler).
     */
    void runSync(@NotNull Runnable task) {
        World world = location.getWorld();
        if (world == null || Bukkit.getServer() == null) {
            task.run();
            return;
        }

        try {
            // Check for Folia RegionScheduler
            Bukkit.getRegionScheduler().execute(owner, location, task);
        } catch (Throwable e) {
            // Fallback for Paper / Spigot
            if (Bukkit.isPrimaryThread()) {
                task.run();
            } else {
                Bukkit.getScheduler().runTask(owner, task);
            }
        }
    }
}
