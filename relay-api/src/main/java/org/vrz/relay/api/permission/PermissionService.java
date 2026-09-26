package org.vrz.relay.api.permission;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Universal, asynchronous, and thread-safe permissions and group hierarchy service.
 * <p>
 * Replaces Vault's 13-year-old blocking synchronous permissions model with
 * non-blocking {@link CompletableFuture}, group weights, prefixes/suffixes,
 * and temporary (TTL) permissions.
 *
 * <pre>{@code
 * // Checking permission asynchronously (non-blocking, 0 TPS drop)
 * permService.hasPermission(player.getUniqueId(), "essentials.fly").thenAccept(hasFly -> {
 *     if (hasFly) {
 *         player.setAllowFlight(true);
 *     }
 * });
 *
 * // Fast-path synchronous check for currently online players
 * if (permService.hasPermissionSync(player.getUniqueId(), "admin.panel")) {
 *     openAdminPanel(player);
 * }
 * }</pre>
 */
public interface PermissionService {

    /**
     * Checks asynchronously whether the given player holds the specified permission node.
     *
     * @param playerId   player UUID
     * @param permission permission string (e.g. "relay.admin")
     * @return future yielding true if permission is granted
     */
    @NotNull
    CompletableFuture<Boolean> hasPermission(@NotNull UUID playerId, @NotNull String permission);

    /**
     * Checks asynchronously whether the player holds a permission in a specific world context.
     *
     * @param playerId   player UUID
     * @param permission permission string
     * @param world      world name context (can be null for global context)
     * @return future yielding true if permission is granted
     */
    @NotNull
    CompletableFuture<Boolean> hasPermission(@NotNull UUID playerId,
                                            @NotNull String permission,
                                            @Nullable String world);

    /**
     * Fast-path synchronous permission check.
     * <p>
     * If the player is online, queries the player's active permission cache directly.
     * If the player is offline, returns false or key default without blocking the thread.
     *
     * @param playerId   player UUID
     * @param permission permission string
     * @return true if permission is currently active
     */
    boolean hasPermissionSync(@NotNull UUID playerId, @NotNull String permission);

    /**
     * Retrieves the primary rank/group name for the player (e.g. "admin", "vip", "default").
     *
     * @param playerId player UUID
     * @return future yielding the primary group name
     */
    @NotNull
    CompletableFuture<String> getPrimaryGroup(@NotNull UUID playerId);

    /**
     * Retrieves all group names the player belongs to (including inherited groups).
     *
     * @param playerId player UUID
     * @return future yielding set of group names
     */
    @NotNull
    CompletableFuture<Set<String>> getGroups(@NotNull UUID playerId);

    /**
     * Checks whether the player belongs to the specified group.
     *
     * @param playerId  player UUID
     * @param groupName target group name
     * @return future yielding true if the player is a member
     */
    @NotNull
    CompletableFuture<Boolean> inGroup(@NotNull UUID playerId, @NotNull String groupName);

    /**
     * Adds a player to a group.
     *
     * @param playerId  player UUID
     * @param groupName target group name
     * @return future yielding the mutation result
     */
    @NotNull
    CompletableFuture<PermissionResult> addGroup(@NotNull UUID playerId, @NotNull String groupName);

    /**
     * Removes a player from a group.
     *
     * @param playerId  player UUID
     * @param groupName target group name
     * @return future yielding the mutation result
     */
    @NotNull
    CompletableFuture<PermissionResult> removeGroup(@NotNull UUID playerId, @NotNull String groupName);

    /**
     * Grants or denies a specific permission node to a player.
     *
     * @param playerId   player UUID
     * @param permission permission node
     * @param value      true to grant, false to explicitly negate
     * @return future yielding the mutation result
     */
    @NotNull
    CompletableFuture<PermissionResult> setPermission(@NotNull UUID playerId,
                                                     @NotNull String permission,
                                                     boolean value);

    /**
     * Removes an explicit permission node from a player.
     *
     * @param playerId   player UUID
     * @param permission permission node to remove
     * @return future yielding the mutation result
     */
    @NotNull
    CompletableFuture<PermissionResult> unsetPermission(@NotNull UUID playerId,
                                                       @NotNull String permission);

    /**
     * Grants a temporary permission that expires automatically after the given duration.
     *
     * @param playerId   player UUID
     * @param permission permission node
     * @param value      true to grant, false to negate
     * @param duration   time-to-live before automatic expiration
     * @return future yielding the mutation result
     */
    @NotNull
    CompletableFuture<PermissionResult> setTemporaryPermission(@NotNull UUID playerId,
                                                              @NotNull String permission,
                                                              boolean value,
                                                              @NotNull Duration duration);

    /**
     * Retrieves the chat prefix for the player (based on primary group or player override).
     *
     * @param playerId player UUID
     * @return future yielding prefix string (e.g. "[Admin] ") or empty string
     */
    @NotNull
    CompletableFuture<String> getPrefix(@NotNull UUID playerId);

    /**
     * Retrieves the chat suffix for the player.
     *
     * @param playerId player UUID
     * @return future yielding suffix string or empty string
     */
    @NotNull
    CompletableFuture<String> getSuffix(@NotNull UUID playerId);

    /**
     * Retrieves the numerical weight/priority of the player's primary rank (higher = higher rank).
     *
     * @param playerId player UUID
     * @return future yielding integer weight
     */
    @NotNull
    CompletableFuture<Integer> getWeight(@NotNull UUID playerId);

    /**
     * Gets a set of all known permission groups configured on the server.
     *
     * @return future yielding set of all group names
     */
    @NotNull
    CompletableFuture<Set<String>> getKnownGroups();

    /**
     * Gets detailed descriptor information about a specific group.
     *
     * @param groupName group identifier
     * @return future yielding optional GroupInfo
     */
    @NotNull
    CompletableFuture<Optional<GroupInfo>> getGroupInfo(@NotNull String groupName);
}
