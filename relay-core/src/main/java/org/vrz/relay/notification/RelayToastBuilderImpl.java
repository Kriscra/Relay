package org.vrz.relay.notification;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.vrz.relay.api.notification.ToastBuilder;
import org.vrz.relay.api.notification.ToastFrame;
import org.vrz.relay.api.notification.ToastNotification;
import org.vrz.relay.util.RelaySchedulerBridge;

import java.util.Objects;
import java.util.UUID;

public final class RelayToastBuilderImpl implements ToastBuilder {

    private final RelayNotificationServiceImpl service;
    private final Player player;

    private Component title = Component.empty();
    private Component description = Component.empty();
    private Material icon = Material.KNOWLEDGE_BOOK;
    private ToastFrame frame = ToastFrame.TASK;
    private Sound sound;

    public RelayToastBuilderImpl(@NotNull RelayNotificationServiceImpl service, @NotNull Player player) {
        this.service = Objects.requireNonNull(service, "service cannot be null");
        this.player = Objects.requireNonNull(player, "player cannot be null");
    }

    @Override
    @NotNull
    public ToastBuilder title(@NotNull Component title) {
        this.title = Objects.requireNonNull(title, "title cannot be null");
        return this;
    }

    @Override
    @NotNull
    public ToastBuilder titleMiniMessage(@NotNull String miniMessage) {
        this.title = MiniMessage.miniMessage().deserialize(Objects.requireNonNull(miniMessage, "miniMessage cannot be null"));
        return this;
    }

    @Override
    @NotNull
    public ToastBuilder description(@NotNull Component description) {
        this.description = Objects.requireNonNull(description, "description cannot be null");
        return this;
    }

    @Override
    @NotNull
    public ToastBuilder descriptionMiniMessage(@NotNull String miniMessage) {
        this.description = MiniMessage.miniMessage().deserialize(Objects.requireNonNull(miniMessage, "miniMessage cannot be null"));
        return this;
    }

    @Override
    @NotNull
    public ToastBuilder icon(@NotNull Material icon) {
        this.icon = Objects.requireNonNull(icon, "icon cannot be null");
        return this;
    }

    @Override
    @NotNull
    public ToastBuilder frame(@NotNull ToastFrame frame) {
        this.frame = Objects.requireNonNull(frame, "frame cannot be null");
        return this;
    }

    @Override
    @NotNull
    public ToastBuilder sound(@NotNull Sound sound) {
        this.sound = Objects.requireNonNull(sound, "sound cannot be null");
        return this;
    }

    @Override
    @NotNull
    public ToastBuilder sound(@NotNull org.bukkit.Sound sound, float volume, float pitch) {
        Objects.requireNonNull(sound, "sound cannot be null");
        this.sound = Sound.sound(sound.key(), Sound.Source.PLAYER, volume, pitch);
        return this;
    }

    @Override
    @NotNull
    public ToastNotification send(@NotNull Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        RelayToastNotificationImpl notification = new RelayToastNotificationImpl(
                plugin,
                player,
                title,
                description,
                icon,
                frame,
                sound
        );

        if (sound != null) {
            try {
                player.playSound(sound);
            } catch (Throwable ignored) {}
        }

        displayVirtualAdvancement(plugin, notification);
        return notification;
    }

    @SuppressWarnings("deprecation")
    private void displayVirtualAdvancement(@NotNull Plugin plugin, @NotNull RelayToastNotificationImpl toast) {
        if (Bukkit.getServer() == null) {
            return;
        }

        try {
            String keyStr = "toast_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            NamespacedKey key = new NamespacedKey(plugin, keyStr);

            String titleJson = GsonComponentSerializer.gson().serialize(toast.getTitle());
            String descJson = GsonComponentSerializer.gson().serialize(toast.getDescription());
            String iconKey = toast.getIcon().getKey().asString();

            String advancementJson = "{\n" +
                    "  \"display\": {\n" +
                    "    \"icon\": {\n" +
                    "      \"id\": \"" + iconKey + "\"\n" +
                    "    },\n" +
                    "    \"title\": " + titleJson + ",\n" +
                    "    \"description\": " + descJson + ",\n" +
                    "    \"frame\": \"" + toast.getFrame().getKey() + "\",\n" +
                    "    \"show_toast\": true,\n" +
                    "    \"announce_to_chat\": false,\n" +
                    "    \"hidden\": true\n" +
                    "  },\n" +
                    "  \"criteria\": {\n" +
                    "    \"trigger\": {\n" +
                    "      \"trigger\": \"minecraft:impossible\"\n" +
                    "    }\n" +
                    "  }\n" +
                    "}";

            Advancement adv = Bukkit.getUnsafe().loadAdvancement(key, advancementJson);
            if (adv != null) {
                RelaySchedulerBridge.runOnPlayer(plugin, player, () -> {
                    try {
                        AdvancementProgress progress = player.getAdvancementProgress(adv);
                        progress.awardCriteria("trigger");

                        // Revoke immediately and remove from registry so player's files stay pristine
                        RelaySchedulerBridge.runOnPlayer(plugin, player, () -> {
                            try {
                                progress.revokeCriteria("trigger");
                                Bukkit.getUnsafe().removeAdvancement(key);
                            } catch (Throwable ignored) {}
                        });
                    } catch (Throwable ignored) {}
                });
            }
        } catch (Throwable ignored) {
            // Graceful fallback for non-Paper environments or security managers
        }
    }
}
