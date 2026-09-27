package org.vrz.relay.api.hologram;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a single line of text within an active {@link Hologram}.
 */
public interface HologramLine {

    /**
     * Gets the 0-based index of this line in the parent hologram.
     *
     * @return 0-based line index
     */
    int getIndex();

    /**
     * Gets the Adventure {@link Component} content of this line.
     *
     * @return text component
     */
    @NotNull
    Component getText();

    /**
     * Updates the text of this line using an Adventure {@link Component}.
     *
     * @param text new text component
     */
    void setText(@NotNull Component text);

    /**
     * Updates the text of this line using MiniMessage format string (e.g. {@code "<rainbow>Text</rainbow>"}).
     *
     * @param miniMessage formatted text string
     */
    void setText(@NotNull String miniMessage);

    /**
     * Gets the vertical offset (height spacing) of this line relative to the base hologram anchor.
     *
     * @return vertical offset in blocks
     */
    double getVerticalOffset();

    /**
     * Sets a custom vertical offset for this specific line.
     *
     * @param offset offset in blocks
     */
    void setVerticalOffset(double offset);
}
