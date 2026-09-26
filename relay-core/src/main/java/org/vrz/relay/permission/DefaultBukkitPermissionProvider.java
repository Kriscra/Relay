package org.vrz.relay.permission;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vrz.relay.api.permission.GroupInfo;
import org.vrz.relay.api.permission.PermissionResult;
import org.vrz.relay.api.permission.PermissionService;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Built-in fallback implementation of {@link PermissionService} backed by standard Bukkit permissions.
 * <p>
 * Registered with {@link org.vrz.relay.api.service.ServicePriority#FALLBACK} so that advanced permission
 * plugins (such as LuckPerms) automatically supersede it when available.
 */
public class DefaultBukkitPermissionProvider implements PermissionService {

    private static final String DEFAULT_GROUP = "default";
    private static final String OP_GROUP = "operator";

    @Override
    @NotNull
    public CompletableFuture<Boolean> hasPermission(@NotNull UUID playerId, @NotNull String permission) {
        return CompletableFuture.completedFuture(hasPermissionSync(playerId, permission));
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> hasPermission(@NotNull UUID playerId,
                                                    @NotNull String permission,
                                                    @Nullable String world) {
        // Bukkit's standard permissible has no native per-world query for offline players
        return hasPermission(playerId, permission);
    }

    @Override
    public boolean hasPermissionSync(@NotNull UUID playerId, @NotNull String permission) {
        if (Bukkit.getServer() == null) {
            return false;
        }
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            return player.hasPermission(permission);
        }
        return Bukkit.getOfflinePlayer(playerId).isOp();
    }

    @Override
    @NotNull
    public CompletableFuture<String> getPrimaryGroup(@NotNull UUID playerId) {
        if (Bukkit.getServer() != null) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOp()) {
                return CompletableFuture.completedFuture(OP_GROUP);
            }
        }
        return CompletableFuture.completedFuture(DEFAULT_GROUP);
    }

    @Override
    @NotNull
    public CompletableFuture<Set<String>> getGroups(@NotNull UUID playerId) {
        Set<String> groups = new HashSet<>();
        groups.add(DEFAULT_GROUP);
        if (Bukkit.getServer() != null) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOp()) {
                groups.add(OP_GROUP);
            }
        }
        return CompletableFuture.completedFuture(groups);
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> inGroup(@NotNull UUID playerId, @NotNull String groupName) {
        return getGroups(playerId).thenApply(groups ->
                groups.stream().anyMatch(g -> g.equalsIgnoreCase(groupName))
        );
    }

    @Override
    @NotNull
    public CompletableFuture<PermissionResult> addGroup(@NotNull UUID playerId, @NotNull String groupName) {
        if (OP_GROUP.equalsIgnoreCase(groupName)) {
            if (Bukkit.getServer() != null) {
                Bukkit.getOfflinePlayer(playerId).setOp(true);
                return CompletableFuture.completedFuture(PermissionResult.success("Player granted operator status"));
            }
        }
        return CompletableFuture.completedFuture(PermissionResult.failure(
                PermissionResult.Status.NOT_SUPPORTED,
                "Dynamic group assignment requires a dedicated permission manager like LuckPerms."
        ));
    }

    @Override
    @NotNull
    public CompletableFuture<PermissionResult> removeGroup(@NotNull UUID playerId, @NotNull String groupName) {
        if (OP_GROUP.equalsIgnoreCase(groupName)) {
            if (Bukkit.getServer() != null) {
                Bukkit.getOfflinePlayer(playerId).setOp(false);
                return CompletableFuture.completedFuture(PermissionResult.success("Player revoked operator status"));
            }
        }
        return CompletableFuture.completedFuture(PermissionResult.failure(
                PermissionResult.Status.NOT_SUPPORTED,
                "Dynamic group removal requires a dedicated permission manager like LuckPerms."
        ));
    }

    @Override
    @NotNull
    public CompletableFuture<PermissionResult> setPermission(@NotNull UUID playerId,
                                                            @NotNull String permission,
                                                            boolean value) {
        return CompletableFuture.completedFuture(PermissionResult.failure(
                PermissionResult.Status.NOT_SUPPORTED,
                "Direct permission modification requires a dedicated permission manager like LuckPerms."
        ));
    }

    @Override
    @NotNull
    public CompletableFuture<PermissionResult> unsetPermission(@NotNull UUID playerId,
                                                              @NotNull String permission) {
        return CompletableFuture.completedFuture(PermissionResult.failure(
                PermissionResult.Status.NOT_SUPPORTED,
                "Direct permission modification requires a dedicated permission manager like LuckPerms."
        ));
    }

    @Override
    @NotNull
    public CompletableFuture<PermissionResult> setTemporaryPermission(@NotNull UUID playerId,
                                                                     @NotNull String permission,
                                                                     boolean value,
                                                                     @NotNull Duration duration) {
        return CompletableFuture.completedFuture(PermissionResult.failure(
                PermissionResult.Status.NOT_SUPPORTED,
                "Temporary permissions require a dedicated permission manager like LuckPerms."
        ));
    }

    @Override
    @NotNull
    public CompletableFuture<String> getPrefix(@NotNull UUID playerId) {
        if (Bukkit.getServer() != null) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOp()) {
                return CompletableFuture.completedFuture("[OP] ");
            }
        }
        return CompletableFuture.completedFuture("");
    }

    @Override
    @NotNull
    public CompletableFuture<String> getSuffix(@NotNull UUID playerId) {
        return CompletableFuture.completedFuture("");
    }

    @Override
    @NotNull
    public CompletableFuture<Integer> getWeight(@NotNull UUID playerId) {
        if (Bukkit.getServer() != null) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOp()) {
                return CompletableFuture.completedFuture(100);
            }
        }
        return CompletableFuture.completedFuture(0);
    }

    @Override
    @NotNull
    public CompletableFuture<Set<String>> getKnownGroups() {
        return CompletableFuture.completedFuture(Set.of(DEFAULT_GROUP, OP_GROUP));
    }

    @Override
    @NotNull
    public CompletableFuture<Optional<GroupInfo>> getGroupInfo(@NotNull String groupName) {
        if (OP_GROUP.equalsIgnoreCase(groupName)) {
            return CompletableFuture.completedFuture(Optional.of(new GroupInfo(OP_GROUP, "Operator", 100, "[OP] ", "")));
        }
        if (DEFAULT_GROUP.equalsIgnoreCase(groupName)) {
            return CompletableFuture.completedFuture(Optional.of(new GroupInfo(DEFAULT_GROUP, "Default", 0, "", "")));
        }
        return CompletableFuture.completedFuture(Optional.empty());
    }
}
