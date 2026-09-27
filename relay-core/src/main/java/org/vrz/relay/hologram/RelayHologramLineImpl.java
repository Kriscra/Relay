package org.vrz.relay.hologram;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.TextDisplay;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vrz.relay.api.hologram.HologramLine;

import java.util.Objects;

/**
 * Thread-safe implementation of a single {@link HologramLine}.
 */
public class RelayHologramLineImpl implements HologramLine {

    private final RelayHologramImpl parent;
    private volatile int index;
    private volatile Component text;
    private volatile double customOffset = Double.NaN;
    private volatile TextDisplay entity;

    public RelayHologramLineImpl(@NotNull RelayHologramImpl parent, int index, @NotNull Component text) {
        this.parent = Objects.requireNonNull(parent, "parent hologram cannot be null");
        this.index = index;
        this.text = Objects.requireNonNull(text, "text cannot be null");
    }

    @Override
    public int getIndex() {
        return index;
    }

    void setIndex(int index) {
        this.index = index;
    }

    @Override
    @NotNull
    public Component getText() {
        return text;
    }

    @Override
    public void setText(@NotNull Component text) {
        this.text = Objects.requireNonNull(text, "text component cannot be null");
        updateEntityText();
    }

    @Override
    public void setText(@NotNull String miniMessage) {
        Objects.requireNonNull(miniMessage, "miniMessage string cannot be null");
        this.text = MiniMessage.miniMessage().deserialize(miniMessage);
        updateEntityText();
    }

    private void updateEntityText() {
        TextDisplay current = this.entity;
        if (current != null && current.isValid()) {
            parent.runSync(() -> {
                if (current.isValid()) {
                    current.text(this.text);
                }
            });
        }
    }

    @Override
    public double getVerticalOffset() {
        if (!Double.isNaN(customOffset)) {
            return customOffset;
        }
        int total = parent.getLineCount();
        return (total - 1 - index) * parent.getLineSpacing();
    }

    @Override
    public void setVerticalOffset(double offset) {
        this.customOffset = offset;
        parent.updateLinePositions();
    }

    @Nullable
    public TextDisplay getEntity() {
        return entity;
    }

    public void setEntity(@Nullable TextDisplay entity) {
        this.entity = entity;
    }
}
