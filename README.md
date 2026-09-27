# ⚡ Relay

<p align="center">
  <img src="https://raw.githubusercontent.com/Kriscra/Relay/main/docs/favicon.svg" width="96" height="96" alt="Relay Logo" />
</p>

<p align="center">
  <strong>Industry-Standard Reactive Service Registry, Inter-Plugin Pub/Sub & Ecosystem Framework</strong><br>
  <em>Next-generation server infrastructure engineered for modern Paper, Purpur, and multi-threaded Folia servers.</em>
</p>

<p align="center">
  <a href="https://github.com/Kriscra/Relay/releases"><img src="https://img.shields.io/badge/version-v1.3.0-cyan?style=for-the-badge&logo=git" alt="Version" /></a>
  <a href="https://papermc.io"><img src="https://img.shields.io/badge/Paper%20%2F%20Folia-1.20%20--%201.21-00f2fe?style=for-the-badge&logo=buffer" alt="Paper" /></a>
  <a href="https://www.oracle.com/java/"><img src="https://img.shields.io/badge/Java-21%20LTS-f59e0b?style=for-the-badge&logo=openjdk" alt="Java 21" /></a>
  <a href="https://jitpack.io/#Kriscra/Relay"><img src="https://img.shields.io/badge/JitPack-v1.3.0-10b981?style=for-the-badge&logo=gradle" alt="JitPack" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-6366f1?style=for-the-badge" alt="License" /></a>
</p>

<p align="center">
  <strong>English</strong> • <a href="README_TR.md">Türkçe</a>
</p>

---

## 🌟 What is Relay?

**Relay** is an enterprise-grade, high-performance infrastructure framework and service backbone designed for modern Minecraft servers running on **Paper**, **Purpur**, and multi-threaded **Folia**.

Instead of fragile load-order dependencies, synchronous Bukkit service lookups that stutter the main thread, and tight compile-time couplings between plugins, Relay delivers:
* **100% Non-Blocking & Asynchronous Architecture:** Built on Java 21 `CompletableFuture` promises and lock-free concurrent primitives.
* **Microservice-Style Pub/Sub & RPC:** Decouple your plugins completely with typed asynchronous messaging and request-response channels.
* **Folia Multi-Thread Native (Region-Safe):** All entity, hologram, and inventory interactions are automatically routed to their owner region thread with zero deadlock risk.
* **Zero-Memory Leak Lifecycles:** Automatic cleanup hooks unregister listeners, remove display entities, close active virtual menus, and evict stale cooldowns upon plugin disable.

---

## 🚀 Built-in Services (Overview)

Relay unifies 10 essential server subsystems into a clean, lightweight API:

| Service / Subsystem | Description | Key Features |
| :--- | :--- | :--- |
| **ServiceRegistry** | Reactive service discovery & provider registry | Priority tiers (`HIGHEST` ➔ `FALLBACK`), load-order independent `onServiceReady` Promise callbacks. |
| **RelayMessenger** | Decoupled Pub/Sub event bus & async RPC | Inter-plugin messaging without jar dependencies; publish topics, subscribe listeners, and await async responses. |
| **EconomyService** | Multi-currency financial ledger engine | IEEE 754 float-safe `BigDecimal` arithmetic, arbitrary currency registration, atomic transfers. |
| **CooldownService** | Universal time-to-live cooldown manager | Thread-safe cooldowns for spells, skills, and commands; action bar completion percentage (`getProgress()`), auto-TTL eviction. |
| **SharedDataService** | Type-safe shared reactive data store | Strongly-typed `DataKey<T>` validation, temporary TTL key expiration, live change observers (`observe`). |
| **PermissionService** | Asynchronous permission & group checking | Non-blocking group and permission queries to safeguard server tick performance, LuckPerms/Vault bridge, weight ranking. |
| **PartyService** | Team, party & friendly-fire manager | Allocation-free, lockless `isFriendlySync` verification for combat engines, dungeon parties, and shared loot. |
| **MetricsService** | Performance telemetry & lag spike alerts | Nanosecond-resolution timers, P50/P95/P99 latency percentiles, automatic `LagSpikeEvent` dispatch upon tick drops. |
| **HologramService** | Display Entity based holographic billboard engine | Minecraft 1.20+ native `TextDisplay` entities, zero ArmorStand lag, Folia region-safe `spawnAsync`, MiniMessage text and shadows. |
| **MenuService** | Virtual chest inventory & GUI engine | Folia thread-safe opening, strict anti-duplication cancellation, 150ms anti-macro debounce, paginated menus (`PaginatedMenu<T>`). |

---

## 🛡️ Folia Multi-Threading & Zero-Leak Architecture

Relay was architected from the ground up for multi-threaded servers:
* **Region-Safe Scheduling:** World entity mutations (such as `TextDisplay` holograms) and inventory views are dispatched via Folia's `RegionScheduler` or `EntityScheduler`.
* **Lock-Free Concurrency:** All internal registries utilize `ConcurrentHashMap`, `CopyOnWriteArrayList`, and `AtomicReference` constructs to guarantee deterministic execution without thread contention or deadlocks.
* **Persistent Auto-Cleanup:** All spawned entities have `setPersistent(false)` enabled. When a consumer plugin unloads, Relay automatically purges its associated subscribers, holograms, and GUI sessions.

---

## 📦 Adding to Your Project (Dependency)

Plugin developers only need to shade or compile against the featherweight **`relay-api`** (~40 KB, zero transitive runtime bloat):

### Gradle (Kotlin DSL)
```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.Kriscra.Relay:relay-api:v1.3.0")
}
```

### Gradle (Groovy DSL)
```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.Kriscra.Relay:relay-api:v1.3.0'
}
```

### Maven (`pom.xml`)
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.Kriscra.Relay</groupId>
        <artifactId>relay-api</artifactId>
        <version>v1.3.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

---

## 🛠️ Quick Code Examples

### 1. Reactive Service Discovery (`onServiceReady`)
Never worry about `loadbefore` or `depend` ordering again. Receive the service instance the moment it gets registered:
```java
RelayAPI.getServiceRegistry().onServiceReady(EconomyService.class, economy -> {
    getLogger().info("Economy hooked! Default currency: " + economy.getDefaultCurrency().displayName());
});
```

### 2. Inter-Plugin Pub/Sub Messaging
Enable seamless cross-plugin communication without direct `.jar` dependencies:
```java
// Publisher Plugin:
RelayAPI.getMessenger().publish("guild:levelup", new GuildLevelPayload("Valkyrie", 10), this);

// Subscriber Plugin:
RelayAPI.getMessenger().subscribe("guild:levelup", GuildLevelPayload.class, this, ctx -> {
    getLogger().info(ctx.getPayload().guildName() + " has reached level " + ctx.getPayload().level());
});
```

### 3. TextDisplay Hologram (Folia Region-Safe)
Create lag-free text displays using Minecraft 1.20+ Display Entities:
```java
Hologram hologram = RelayAPI.getHolograms().createBuilder(plugin, location)
        .appendLine("<gradient:#4facfe:#00f2fe><bold>✦ RELAY NETWORK ✦</bold></gradient>")
        .appendLine("<gray>Powered by TextDisplay & Folia</gray>")
        .setBillboard(Billboard.CENTER)
        .setTextShadow(true)
        .build();

hologram.spawnAsync();
```

### 4. Folia-Safe Chest GUI (Anti-Dupe & Debounce)
```java
Menu menu = RelayAPI.getMenus().createBuilder(plugin)
        .titleMiniMessage("<green>Teleportation Portal</green>")
        .pattern(
            "#########",
            "#   W   #",
            "#########"
        )
        .bindKey('#', MenuButton.of(new ItemStack(Material.BLACK_STAINED_GLASS_PANE)))
        .bindKey('W', MenuButton.builder()
                .item(new ItemStack(Material.COMPASS))
                .sound(Sound.ENTITY_ENDERMAN_TELEPORT)
                .debounce(250L) // 250ms anti-macro debounce
                .onClick(ctx -> ctx.getPlayer().teleportAsync(spawnLocation))
                .build())
        .build();

menu.open(player);
```

---

## ⚙️ Administration & Diagnostics

Monitor active services, topics, telemetry, and open sessions in real time via the in-game command engine:

* `/relay status`: Overview of loaded services, topics, active holograms, menus, and server runtime environment.
* `/relay services`: Lists all registered services, provider plugins, and priority tiers.
* `/relay topics`: Inspects active pub/sub topics and subscriber counts.
* `/relay metrics`: Live execution averages, P95 latencies, and transaction counters.
* `/relay metrics reset`: Resets all active telemetry samples and metrics counters.
* `/relay holograms`: Inspects currently tracked TextDisplay holograms and their world locations.
* `/relay menus`: Lists active virtual inventory sessions and viewing players.

**Permission:** `relay.admin` (Default: OP)

---

## 🔗 Links & Resources

* 🌐 **Live Documentation:** https://kriscra.github.io/Relay/
* 💻 **GitHub Repository:** https://github.com/Kriscra/Relay
* 📦 **JitPack SDK:** https://jitpack.io/#Kriscra/Relay
* 📜 **License:** [MIT License](LICENSE)
