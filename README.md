# ⚡ Relay

<p align="center">
  <img src="https://raw.githubusercontent.com/Kriscra/Relay/main/docs/favicon.svg" width="96" height="96" alt="Relay Logo" />
</p>

<p align="center">
  <strong>Industry-Standard Reactive Service Registry, Inter-Plugin Pub/Sub & Folia Multi-Thread Ecosystem Framework</strong><br>
  <em>Endüstri Standardı Reaktif Servis Kaydı, Eklentiler Arası İletişim (Pub/Sub) ve Çok Çekirdekli Folia Ekosistem Çatısı</em>
</p>

<p align="center">
  <a href="https://github.com/Kriscra/Relay/releases"><img src="https://img.shields.io/badge/version-v1.5.0-cyan?style=for-the-badge&logo=git" alt="Version" /></a>
  <a href="https://papermc.io"><img src="https://img.shields.io/badge/Paper%20%2F%20Folia-1.20%20--%201.21-00f2fe?style=for-the-badge&logo=buffer" alt="Paper" /></a>
  <a href="https://www.oracle.com/java/"><img src="https://img.shields.io/badge/Java-21%20LTS-f59e0b?style=for-the-badge&logo=openjdk" alt="Java 21" /></a>
  <a href="https://jitpack.io/#Kriscra/Relay"><img src="https://img.shields.io/badge/JitPack-v1.5.0-10b981?style=for-the-badge&logo=gradle" alt="JitPack" /></a>
  <a href="https://github.com/Kriscra/Relay/actions"><img src="https://img.shields.io/badge/tests-46%2F46%20passed-emerald?style=for-the-badge&logo=githubactions" alt="Tests" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-6366f1?style=for-the-badge" alt="License" /></a>
</p>

<p align="center">
  <a href="#-english"><strong>🇬🇧 English Documentation</strong></a>
  &nbsp;•&nbsp;
  <a href="#-türkçe"><strong>🇹🇷 Türkçe Dokümantasyon</strong></a>
</p>

---

## 🇬🇧 English

### 🌟 What is Relay?

**Relay** is an enterprise-grade, high-performance reactive service backbone, inter-plugin communication framework, and multi-threaded infrastructure engine designed for modern Minecraft servers running on **Paper**, **Purpur**, and multi-threaded **Folia**.

For years, Minecraft plugin architecture has remained shackled to single-threaded Bukkit conventions from 2012: synchronous main-thread stalls, fragile `depend` load-order chains in `plugin.yml`, incompatible Folia scheduler crashes, hacky ArmorStand holograms that destroy client FPS, and notorious memory leaks whenever a plugin reloads.

**Relay completely redesigns server internals around 5 modern architectural pillars:**

1. ⚡ **100% Non-Blocking & Asynchronous Architecture:** Built on Java 21 `CompletableFuture` promises, concurrent lock-free primitives, and nanosecond telemetry. Long database lookups, economy calculations, and permission queries never stall server ticks.
2. 🧵 **Unified Multi-Thread Scheduler (`SchedulerService`):** Write your task logic once. Relay transparently routes entity, region, global, and async tasks to Folia's `EntityScheduler`, `RegionScheduler`, `GlobalRegionScheduler`, and `AsyncScheduler`, while seamlessly degrading to `BukkitScheduler` on Paper/Spigot.
3. 📡 **Microservice-Style Pub/Sub & RPC Event Bus:** Decouple your plugins completely. Publish events across topics, subscribe typed payload consumers, or invoke time-bounded asynchronous request-response queries (`request`) without direct `.jar` dependencies.
4. 🛡️ **Zero-Memory Leak Lifecycles:** Automatic cleanup hooks unregister listeners, remove display entities, close active virtual menus, dismiss active bossbars, cancel running scheduled tasks, and evict stale cooldowns upon plugin disable (`setPersistent(false)`).
5. 🪶 **Ultra-Lightweight API (~80 KB):** Zero transitive runtime dependencies. Your consumer plugins stay clean, fast, and featherweight.

---

### 🚀 Built-in Services (Overview)

Relay unifies 12 essential server subsystems into a clean, lightweight API:

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
| **NotificationService** | Multi-channel player feedback & display engine | Priority-based queued ActionBars, animated countdown BossBars, virtual top-right Toasts, and smooth Title sequences. |
| **SchedulerService** | Unified Folia & Paper multi-thread scheduler | Universal entity, location/region, global, and async scheduling; `runAsyncPromise`, thread verification, zero-leak task auto-cancel. |

---

### 🛡️ Folia Multi-Threading & Zero-Leak Architecture

Relay was architected from the ground up for multi-threaded servers:
* **Region-Safe Scheduling:** World entity mutations (such as `TextDisplay` holograms), scheduled tasks, and inventory views are dispatched via Folia's `RegionScheduler` or `EntityScheduler`.
* **Lock-Free Concurrency:** All internal registries utilize `ConcurrentHashMap`, `CopyOnWriteArrayList`, and `AtomicReference` constructs to guarantee deterministic execution without thread contention or deadlocks.
* **Persistent Auto-Cleanup:** All spawned entities have `setPersistent(false)` enabled. When a consumer plugin unloads, Relay automatically purges its associated subscribers, holograms, bossbars, scheduled tasks, and GUI sessions.

---

### 📦 Adding to Your Project (Dependency)

Plugin developers only need to compile against the featherweight **`relay-api`** (~80 KB, zero transitive runtime bloat):

#### Gradle (Kotlin DSL)
```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.Kriscra.Relay:relay-api:v1.5.0")
}
```

#### Gradle (Groovy DSL)
```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.Kriscra.Relay:relay-api:v1.5.0'
}
```

#### Maven (`pom.xml`)
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
        <version>v1.5.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

---

### 🛠️ Quick Code Examples

#### 1. Reactive Service Discovery (`onServiceReady`)
Never worry about `loadbefore` or `depend` ordering again. Receive the service instance the moment it gets registered:
```java
RelayAPI.getServiceRegistry().onServiceReady(EconomyService.class, economy -> {
    getLogger().info("Economy hooked! Default currency: " + economy.getDefaultCurrency().displayName());
});
```

#### 2. Inter-Plugin Pub/Sub & RPC Messaging
Enable seamless cross-plugin communication without direct `.jar` dependencies:
```java
// Publisher Plugin:
RelayAPI.getMessenger().publish("guild:levelup", new GuildLevelPayload("Valkyrie", 10), this);

// Subscriber Plugin:
RelayAPI.getMessenger().subscribe("guild:levelup", GuildLevelPayload.class, this, ctx -> {
    getLogger().info(ctx.getPayload().guildName() + " has reached level " + ctx.getPayload().level());
});

// Non-blocking RPC Request/Response:
RelayAPI.getMessenger().request("guild:get_balance", guildId, BigDecimal.class, Duration.ofSeconds(2))
    .thenAccept(balance -> player.sendMessage("Guild Balance: " + balance));
```

#### 3. TextDisplay Holograms (Minecraft 1.20+ Native)
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

#### 4. Folia-Safe Chest GUI (Anti-Dupe & Debounce)
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

#### 5. Multi-Channel Notifications (Priority ActionBar, BossBar & Toast)
```java
// Priority-queued ActionBar with preemption:
RelayAPI.getNotifications().actionBar(player)
        .messageMiniMessage("<gradient:#f12711:#f5af19><bold>WARNING:</bold> Combat Zone Entered!</gradient>")
        .priority(NotificationPriority.HIGH)
        .duration(Duration.ofSeconds(3))
        .send(plugin);

// Animated Countdown BossBar with placeholders %time% and %progress%:
ActiveBossBar bar = RelayAPI.getNotifications().bossBar(player)
        .titleMiniMessage("<aqua>Dungeon Cleansing</aqua> <gray>(%time%s left)</gray>")
        .countdown(Duration.ofSeconds(30))
        .color(BossBar.Color.BLUE)
        .onComplete(() -> player.sendMessage("Dungeon sealed!"))
        .send(plugin);

// Virtual Top-Right Toast Popup:
RelayAPI.getNotifications().toast(player)
        .titleMiniMessage("<gold>Mythic Quest Complete!</gold>")
        .descriptionMiniMessage("<gray>Defeat the Ender Dragon</gray>")
        .icon(Material.DRAGON_HEAD)
        .frame(ToastFrame.CHALLENGE)
        .send(plugin);
```

#### 6. Unified Multi-Thread Scheduler (Folia & Paper)
Run tasks on entity region threads, global ticks, or async pools with identical code across Paper and Folia:
```java
SchedulerService scheduler = RelayAPI.getScheduler();

// Run safely on player's regional thread:
scheduler.runFor(plugin, player, () -> {
    player.giveExp(100);
});

// Run repeating particle task at world coordinates:
RelayTask task = scheduler.runAtRepeating(plugin, targetLoc, t -> {
    targetLoc.getWorld().spawnParticle(Particle.FLAME, targetLoc, 5);
}, 20L, 10L);

// Cancel anytime (or let Relay auto-cancel upon plugin disable):
task.cancel();

// Non-blocking async promise execution:
scheduler.supplyAsyncPromise(() -> database.loadPlayerData(uuid))
         .thenAccept(data -> scheduler.runFor(plugin, player, () -> player.sendMessage("Loaded!")));
```

---

### ⚙️ Administration & Diagnostics

Monitor active services, topics, telemetry, open sessions, scheduled tasks, and test notifications in real time:

* `/relay status`: Overview of loaded services, topics, active holograms, menus, scheduled tasks, and server runtime environment.
* `/relay services`: Lists all registered services, provider plugins, and priority tiers.
* `/relay topics`: Inspects active pub/sub topics and subscriber counts.
* `/relay metrics`: Live execution averages, P95 latencies, and transaction counters.
* `/relay metrics reset`: Resets all active telemetry samples and metrics counters.
* `/relay holograms`: Inspects currently tracked TextDisplay holograms and their world locations.
* `/relay menus`: Lists active virtual inventory sessions and viewing players.
* `/relay notify <player> <actionbar|bossbar|toast|title> [message]`: Dispatches real-time test notifications to an online player.

**Permission:** `relay.admin` (Default: OP)

---

## 🇹🇷 Türkçe

### 🌟 Relay Nedir?

**Relay**, modern Minecraft sunucularında (**Paper**, **Purpur** ve özellikle çok çekirdekli **Folia**) eklentiler arası entegrasyonu, veri iletişimini ve sunucu servislerini endüstri standartlarına taşıyan kurumsal düzeyde açık kaynaklı bir altyapı motorudur.

Yıllardır Minecraft eklenti mimarisi 2012'den kalma tek çekirdekli Bukkit alışkanlıklarına takılı kalmıştır: Ana iş parçacığını (Main Thread) kilitleyen senkron veritabanı/ekonomi sorguları, `plugin.yml` içerisindeki kırılgan `depend:` yüklenme sırası çakışmaları, Folia üzerinde çöken klasik zamanlayıcılar, sunucu performansını yok eden ArmorStand hologramları ve eklenti yeniden yüklendiğinde arkasında zombi nesneler bırakan bellek sızıntıları.

**Relay, sunucu mimarisini 5 temel sütun üzerinde yeniden inşa eder:**

1. ⚡ **%100 Non-Blocking & Asenkron Mimari:** Java 21 `CompletableFuture` promise zincirleri, kilit-içermeyen (lock-free) eşzamanlı veri yapıları ve nanosaniye hassasiyetli telemetri. Veritabanı sorguları ve ekonomi transferleri sunucu TPS'ini asla düşürmez.
2. 🧵 **Evrensel Çok Çekirdekli Zamanlayıcı (`SchedulerService`):** Görev kodunuzu tek sefer yazın. Relay; varlık, koordinat/bölge, küresel tick ve asenkron görevleri Folia'nın `EntityScheduler`, `RegionScheduler`, `GlobalRegionScheduler` ve `AsyncScheduler` motorlarına yönlendirirken, Paper üzerinde otomatik olarak `BukkitScheduler`'a şeffaf düşer.
3. 📡 **Mikroservis Tipi Pub/Sub & RPC İletişim Veri Yolu:** Eklentilerinizi birbirinden tamamen bağımsız hale getirin. Birbirlerinin `.jar` dosyasına ihtiyaç duymadan kanallara abone olun, mesaj yayınlayın veya zaman aşımı korumalı asenkron soru-cevap (`request`) yapın.
4. 🛡️ **Sıfır Bellek Sızıntısı (Zero-Leak) Garantisi:** Bir eklenti kapandığında veya sunucu yeniden yüklendiğinde; açılan hologramlar, sanal sandık menüleri, aktif BossBar'lar, zamanlanmış görevler ve bekleme süreleri sunucudan otomatik olarak tahliye edilir (`setPersistent(false)`).
5. 🪶 **Tüy Siklet API (~80 KB):** Harici bağımlılık veya şişkinlik içermez. Projenize eklediğinizde sadece hafif ve tip-güvenli arayüzler sunar.

---

### 🚀 Dahili Servisler (Genel Bakış)

Relay, bir sunucunun ve eklentilerin ihtiyaç duyduğu 12 temel alt sistemi tek bir çatı altında eksiksiz sunar:

| Modül / Servis | Açıklama | Öne Çıkan Özellikler |
| :--- | :--- | :--- |
| **ServiceRegistry** | Reaktif servis keşfi ve kayıt defteri | `HIGHEST` ➔ `FALLBACK` öncelik basamakları, yüklenme sırasından bağımsız `onServiceReady` Promise mimarisi. |
| **RelayMessenger** | Bağımsız Pub/Sub ve RPC iletişim veri yolu | Eklentiler birbirinin `.jar` dosyasına bağımlı olmadan kanallara abone olabilir veya asenkron istek-cevap (`request`) yapabilir. |
| **EconomyService** | Çoklu para birimi ve finans motoru | IEEE 754 kayan noktalı sayı hatalarını önleyen `BigDecimal` aritmetiği, sınırsız para birimi, atomik transferler. |
| **CooldownService** | Evrensel bekleme süresi yöneticisi | Yetenek, büyü ve komutlar için thread-safe bekleme süreleri, ActionBar dolum yüzdesi (`getProgress()`), otomatik TTL tahliyesi. |
| **SharedDataService** | Tip güvenli paylaşımlı veri havuzu | `DataKey<T>` ile veri doğrulama, süreli geçici veriler (`setTemporary`), reaktif canlı gözlemciler (`observe`). |
| **PermissionService** | Asenkron yetki ve rütbe kontrolü | TPS düşüşlerini engelleyen asenkron yetki sorgulama, LuckPerms/Vault uyumluluğu, ön ek ve rütbe ağırlığı. |
| **PartyService** | Takım ve dost ateşi yöneticisi | Combat anında thread dondurmayan sıfır-bellek tahsisli `isFriendlySync` kontrolleri, zindan grupları. |
| **MetricsService** | Performans telemetrisi & lag alarmı | Nanosaniye hassasiyetinde süre ölçümü, P50/P95/P99 yüzdelikleri, TPS düşüşlerinde otomatik `LagSpikeEvent` alarmı. |
| **HologramService** | Display Entity tabanlı hologram motoru | Minecraft 1.20+ `TextDisplay` native mimarisi, sıfır ArmorStand lag, Folia region-safe `spawnAsync`, MiniMessage ve gölge. |
| **MenuService** | Folia güvenli sanal sandık GUI motoru | Üst envanter tıklamalarını ve sürüklemelerini kilitleyen anti-dupe koruması, 150ms anti-macro debounce, sayfalama (pagination). |
| **NotificationService** | Çok kanallı oyuncu bildirim & geri bildirim motoru | Öncelik sıralı ActionBar kuyruğu, animasyonlu geri sayımlı BossBar, sağ üst sanal Toast kutucukları ve Title akışları. |
| **SchedulerService** | Evrensel Folia & Paper çok çekirdekli zamanlayıcı | Folia varlık, koordinat/bölge, global ve asenkron görev çalıştırma; `runAsyncPromise`, thread doğrulama, sıfır sızıntılı görev iptali. |

---

### 🛡️ Folia Paralel Çekirdek Güvenliği & Sıfır Sızıntı Mimarisi

Relay, çok çekirdekli Minecraft sunucu yazılımı **Folia** için sıfırdan tasarlanmıştır:
* **Region-Safe Scheduling:** Hologram, zamanlanmış görevler, menü ve bildirim işlemleri, hedef koordinatın veya oyuncunun ait olduğu bağımsız iş parçacığına otomatik iletilir.
* **Lock-Free Veri Yapıları:** Tüm dahili önbellekler `ConcurrentHashMap`, `CopyOnWrite` ve atomik sayaçlar ile donatılmıştır; kilitlenme (deadlock) yaşanmaz.
* **Zero-Leak Yaşam Döngüsü:** Bir eklenti kapandığında veya sunucu yeniden yüklendiğinde, o eklentiye ait tüm dinleyiciler, hologramlar, zamanlanmış görevler, menüler, BossBar'lar ve bekleme süreleri sunucudan otomatik tahliye edilir (`setPersistent(false)`).

---

### 📦 Projenize Dahil Edin (Bağımlılık)

Eklenti geliştiricileri yalnızca hafif, harici kütüphane içermeyen **`relay-api`** modülünü çeker (~80 KB):

#### Gradle (Kotlin DSL)
```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.Kriscra.Relay:relay-api:v1.5.0")
}
```

#### Gradle (Groovy DSL)
```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.Kriscra.Relay:relay-api:v1.5.0'
}
```

#### Maven (`pom.xml`)
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
        <version>v1.5.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

---

### 🛠️ Hızlı Kod Örnekleri

#### 1. Reaktif Servis Keşfi (`onServiceReady`)
Yüklenme sırasını dert etmeyin. İlgili eklenti sizden sonra yüklense bile servis kaydedildiği anda geri çağrılır:
```java
RelayAPI.getServiceRegistry().onServiceReady(EconomyService.class, economy -> {
    getLogger().info("Ekonomi sistemi bağlandı! Para birimi: " + economy.getDefaultCurrency().displayName());
});
```

#### 2. Eklentiler Arası Pub/Sub ve RPC İletişimi
Eklentiler birbirinin `.jar` dosyasına bağımlı olmadan güvenle haberleşir:
```java
// Mesaj Yayınlayan Eklenti:
RelayAPI.getMessenger().publish("klan:seviye_atlama", new KlanSeviyeVerisi("Valkyrie", 10), this);

// Dinleyen Eklenti:
RelayAPI.getMessenger().subscribe("klan:seviye_atlama", KlanSeviyeVerisi.class, this, ctx -> {
    getLogger().info(ctx.getPayload().klanAdi() + " yeni seviyeye ulaştı: " + ctx.getPayload().seviye());
});

// Non-blocking Asenkron RPC İstek-Cevap:
RelayAPI.getMessenger().request("klan:bakiye_sor", klanId, BigDecimal.class, Duration.ofSeconds(2))
    .thenAccept(bakiye -> player.sendMessage("Klan Bakiyesi: " + bakiye));
```

#### 3. TextDisplay Hologram Motoru (Minecraft 1.20+ Native)
ArmorStand kullanmadan, sıfır FPS kaybı ile Folia bölgesel iş parçacığında güvenli hologramlar oluşturun:
```java
Hologram hologram = RelayAPI.getHolograms().createBuilder(plugin, location)
        .appendLine("<gradient:#4facfe:#00f2fe><bold>✦ RELAY NETWORK ✦</bold></gradient>")
        .appendLine("<gray>TextDisplay & Folia Destekli</gray>")
        .setBillboard(Billboard.CENTER)
        .setTextShadow(true)
        .build();

hologram.spawnAsync();
```

#### 4. Folia Güvenli Sanal Sandık GUI (Anti-Dupe & Anti-Macro)
```java
Menu menu = RelayAPI.getMenus().createBuilder(plugin)
        .titleMiniMessage("<green>Işınlanma Kapısı</green>")
        .pattern(
            "#########",
            "#   W   #",
            "#########"
        )
        .bindKey('#', MenuButton.of(new ItemStack(Material.BLACK_STAINED_GLASS_PANE)))
        .bindKey('W', MenuButton.builder()
                .item(new ItemStack(Material.COMPASS))
                .sound(Sound.ENTITY_ENDERMAN_TELEPORT)
                .debounce(250L) // 250ms anti-macro debounce koruması
                .onClick(ctx -> ctx.getPlayer().teleportAsync(spawnLocation))
                .build())
        .build();

menu.open(player);
```

#### 5. Çok Kanallı Oyuncu Bildirimleri (Öncelikli ActionBar, BossBar & Toast)
```java
// Öncelik sıralı, diğer mesajları ezmeyen ActionBar kuyruğu:
RelayAPI.getNotifications().actionBar(player)
        .messageMiniMessage("<gradient:#f12711:#f5af19><bold>UYARI:</bold> Savaş Bölgesine Girildi!</gradient>")
        .priority(NotificationPriority.HIGH)
        .duration(Duration.ofSeconds(3))
        .send(plugin);

// Animasyonlu geri sayımlı ve %time% / %progress% etiketli BossBar:
ActiveBossBar bar = RelayAPI.getNotifications().bossBar(player)
        .titleMiniMessage("<aqua>Zindan Temizliği</aqua> <gray>(Kalan: %time%s)</gray>")
        .countdown(Duration.ofSeconds(30))
        .color(BossBar.Color.BLUE)
        .onComplete(() -> player.sendMessage("Zindan kapıları mühürlendi!"))
        .send(plugin);

// Sağ Üst Sanal Toast (Advancement) Bildirimi:
RelayAPI.getNotifications().toast(player)
        .titleMiniMessage("<gold>Efsanevi Görev!</gold>")
        .descriptionMiniMessage("<gray>Ender Ejderhasını alt et</gray>")
        .icon(Material.DRAGON_HEAD)
        .frame(ToastFrame.CHALLENGE)
        .send(plugin);
```

#### 6. Evrensel Çok Çekirdekli Zamanlayıcı (Folia & Paper)
Folia bölgesel iş parçacıkları (Region), global tick veya asenkron havuzlarda aynı kodla güvenle görev çalıştırın:
```java
SchedulerService scheduler = RelayAPI.getScheduler();

// Oyuncunun bulunduğu bölgesel (region) iş parçacığında güvenle çalıştır:
scheduler.runFor(plugin, player, () -> {
    player.giveExp(100);
});

// Belirli bir dünya koordinatında periyodik parçacık efekti görevi:
RelayTask task = scheduler.runAtRepeating(plugin, targetLoc, t -> {
    targetLoc.getWorld().spawnParticle(Particle.FLAME, targetLoc, 5);
}, 20L, 10L);

// İstediğiniz an iptal edin (veya eklenti kapandığında Relay'in otomatik iptal etmesine bırakın):
task.cancel();

// Non-blocking asenkron promise akışı:
scheduler.supplyAsyncPromise(() -> database.loadPlayerData(uuid))
         .thenAccept(data -> scheduler.runFor(plugin, player, () -> player.sendMessage("Yüklendi!")));
```

---

### ⚙️ Yönetici Komutları

Sunucudaki aktif servisleri, telemetriyi, zamanlanmış görevleri ve oturumları anlık izleyin:

* `/relay status`: Aktif servis, konu, hologram, menü, zamanlanmış görev ve çalışma ortamı bilgilerini gösterir.
* `/relay services`: Sunucuda kayıtlı tüm servisleri ve öncelik derecelerini listeler.
* `/relay topics`: Aktif Pub/Sub iletişim kanallarını ve dinleyici sayılarını listeler.
* `/relay metrics`: Ortalama işlem sürelerini, P95 gecikmelerini ve sayaçları gösterir.
* `/relay metrics reset`: Tüm telemetri ve performans sayaçlarını sıfırlar.
* `/relay holograms`: Aktif TextDisplay hologramlarını ve konumlarını listeler.
* `/relay menus`: Aktif sanal menü oturumlarını ve görüntüleyen oyuncu sayılarını listeler.
* `/relay notify <oyuncu> <actionbar|bossbar|toast|title> [mesaj]`: Oyuncuya test bildirimi gönderir.

**Yetki:** `relay.admin` (Varsayılan: OP)

---

## 🔗 Links & Resources / Bağlantılar

* 🌐 **Live Documentation / Canlı Dokümantasyon:** https://kriscra.github.io/Relay/
* 💻 **GitHub Repository:** https://github.com/Kriscra/Relay
* 📦 **JitPack SDK:** https://jitpack.io/#Kriscra/Relay
* 📜 **License / Lisans:** [MIT License](LICENSE)
