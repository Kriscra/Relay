package org.vrz.relay.hologram;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vrz.relay.api.hologram.Hologram;
import org.vrz.relay.api.hologram.HologramBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Fluent builder implementation for {@link Hologram}.
 */
public class RelayHologramBuilderImpl implements HologramBuilder {

    private final RelayHologramServiceImpl service;
    private final String id;
    private Location location;
    private final List<Component> lines = new ArrayList<>();
    private Display.Billboard billboard = Display.Billboard.CENTER;
    private Color backgroundColor = null;
    private boolean shadow = true;
    private boolean seeThrough = false;
    private double lineSpacing = 0.28;
    private Plugin owner;

    public RelayHologramBuilderImpl(@NotNull RelayHologramServiceImpl service, @NotNull String id) {
        this.service = Objects.requireNonNull(service, "service cannot be null");
        this.id = Objects.requireNonNull(id, "id cannot be null");
    }

    @Override
    @NotNull
    public HologramBuilder location(@NotNull Location location) {
        this.location = Objects.requireNonNull(location, "location cannot be null");
        return this;
    }

    @Override
    @NotNull
    public HologramBuilder addLine(@NotNull Component text) {
        this.lines.add(Objects.requireNonNull(text, "text cannot be null"));
        return this;
    }

    @Override
    @NotNull
    public HologramBuilder addLine(@NotNull String miniMessage) {
        Objects.requireNonNull(miniMessage, "miniMessage cannot be null");
        this.lines.add(MiniMessage.miniMessage().deserialize(miniMessage));
        return this;
    }

    @Override
    @NotNull
    public HologramBuilder billboard(@NotNull Display.Billboard billboard) {
        this.billboard = Objects.requireNonNull(billboard, "billboard cannot be null");
        return this;
    }

    @Override
    @NotNull
    public HologramBuilder backgroundColor(@Nullable Color color) {
        this.backgroundColor = color;
        return this;
    }

    @Override
    @NotNull
    public HologramBuilder shadow(boolean shadow) {
        this.shadow = shadow;
        return this;
    }

    @Override
    @NotNull
    public HologramBuilder seeThrough(boolean seeThrough) {
        this.seeThrough = seeThrough;
        return this;
    }

    @Override
    @NotNull
    public HologramBuilder lineSpacing(double spacing) {
        this.lineSpacing = spacing;
        return this;
    }

    @Override
    @NotNull
    public HologramBuilder owner(@NotNull Plugin plugin) {
        this.owner = Objects.requireNonNull(plugin, "owner plugin cannot be null");
        return this;
    }

    @Override
    @NotNull
    public Hologram build() {
        if (location == null) {
            throw new IllegalStateException("Hologram location must be specified!");
        }
        if (owner == null) {
            throw new IllegalStateException("Hologram owner plugin must be specified!");
        }

        RelayHologramImpl hologram = new RelayHologramImpl(id, location, owner);
        hologram.setBillboard(billboard);
        hologram.setBackgroundColor(backgroundColor);
        hologram.setTextShadow(shadow);
        hologram.setSeeThrough(seeThrough);
        hologram.setLineSpacing(lineSpacing);

        for (Component line : lines) {
            hologram.appendLine(line);
        }

        service.registerHologram(hologram);
        return hologram;
    }

    @Override
    @NotNull
    public Hologram buildAndSpawn() {
        Hologram hologram = build();
        hologram.spawn();
        return hologram;
    }
}
