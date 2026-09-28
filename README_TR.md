# ⚡ Relay

<p align="center">
  <img src="https://raw.githubusercontent.com/Kriscra/Relay/main/docs/favicon.svg" width="96" height="96" alt="Relay Logo" />
</p>

<p align="center">
  <strong>Endüstri Standardı Reaktif Servis Kaydı, Eklentiler Arası İletişim (Pub/Sub) ve Ekosistem Çatısı</strong><br>
  <em>Modern Paper, Purpur ve Çok Çekirdekli Folia Sunucuları İçin Yeni Nesil Altyapı Motoru.</em>
</p>

<p align="center">
  <a href="https://github.com/Kriscra/Relay/releases"><img src="https://img.shields.io/badge/version-v1.4.1-cyan?style=for-the-badge&logo=git" alt="Version" /></a>
  <a href="https://papermc.io"><img src="https://img.shields.io/badge/Paper%20%2F%20Folia-1.20%20--%201.21-00f2fe?style=for-the-badge&logo=buffer" alt="Paper" /></a>
  <a href="https://www.oracle.com/java/"><img src="https://img.shields.io/badge/Java-21%20LTS-f59e0b?style=for-the-badge&logo=openjdk" alt="Java 21" /></a>
  <a href="https://jitpack.io/#Kriscra/Relay"><img src="https://img.shields.io/badge/JitPack-v1.4.1-10b981?style=for-the-badge&logo=gradle" alt="JitPack" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-6366f1?style=for-the-badge" alt="License" /></a>
</p>

<p align="center">
  <a href="README.md">English</a> • <strong>Türkçe</strong>
</p>

---

## 🌟 Relay Nedir?

**Relay**, modern Minecraft sunucularında (Paper, Purpur ve özellikle çok iş parçacıklı **Folia**) eklentiler arası entegrasyonu ve sunucu servislerini endüstri standartlarına taşıyan açık kaynaklı bir altyapı motorudur.

Eski Bukkit mimarisindeki ana iş parçacığını (Main Thread) donduran senkron çağrılar yerine; **%100 Non-Blocking asenkron mimari**, **mikroservis tipi Pub/Sub mesajlaşması**, **Folia bölgesel iş parçacığı güvenliği (Region-Safe)** ve **otomatik bellek temizliği (Zero-Leak)** sunar.

---

## 🚀 Dahili Servisler (Genel Bakış)

Relay, bir sunucunun ve eklentilerin ihtiyaç duyduğu 11 temel alt sistemi tek bir çatı altında eksiksiz sunar:

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

---

## 🛡️ Folia Multi-Threading & Zero-Leak Mimarisi

Relay, çok çekirdekli Minecraft sunucu yazılımı **Folia** için sıfırdan tasarlanmıştır:
* **Region-Safe Scheduling:** Hologram, menü ve bildirim işlemleri, hedef koordinatın veya oyuncunun ait olduğu bağımsız iş parçacığına otomatik iletilir.
* **Lock-Free Veri Yapıları:** Tüm dahili önbellekler `ConcurrentHashMap`, `CopyOnWrite` ve atomik sayaçlar ile donatılmıştır; kilitlenme (deadlock) yaşanmaz.
* **Zero-Leak Yaşam Döngüsü:** Bir eklenti kapandığında veya sunucu yeniden yüklendiğinde, o eklentiye ait tüm dinleyiciler, hologramlar, menüler, BossBar'lar ve bekleme süreleri sunucudan otomatik tahliye edilir (`setPersistent(false)`).

---

## 📦 Geliştiriciler İçin Projeye Ekleme (Dependency)

Eklenti geliştiricileri yalnızca hafif, harici kütüphane içermeyen **`relay-api`** modülünü çeker:

### Gradle (Kotlin DSL)
```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.Kriscra.Relay:relay-api:v1.4.1")
}
```

### Gradle (Groovy DSL)
```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.Kriscra.Relay:relay-api:v1.4.1'
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
        <version>v1.4.1</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

---

## 🛠️ Hızlı Kod Örnekleri

### 1. Reaktif Servis Keşfi (`onServiceReady`)
Yüklenme sırasını dert etmeyin. İlgili eklenti sizden sonra yüklense bile servis kaydedildiği anda geri çağrılır:
```java
RelayAPI.getServiceRegistry().onServiceReady(EconomyService.class, economy -> {
    getLogger().info("Ekonomi sistemi bağlandı! Para birimi: " + economy.getDefaultCurrency().displayName());
});
```

### 2. Eklentiler Arası Mesajlaşma (Pub/Sub)
Kendi aralarında doğrudan `.jar` bağımlılığı olmadan eklentilerin haberleşmesini sağlayın:
```java
// Yayıncı Eklenti:
RelayAPI.getMessenger().publish("clan:levelup", new ClanLevelPayload("Akıncılar", 10), this);

// Dinleyici Eklenti:
RelayAPI.getMessenger().subscribe("clan:levelup", ClanLevelPayload.class, this, ctx -> {
    getLogger().info(ctx.getPayload().clanName() + " seviye atladı!");
});
```

### 3. TextDisplay Hologram Oluşturma (Folia Region-Safe)
```java
Hologram hologram = RelayAPI.getHolograms().createBuilder(plugin, location)
        .appendLine("<gradient:#4facfe:#00f2fe><bold>✦ RELAY NETWORK ✦</bold></gradient>")
        .appendLine("<gray>Folia & Paper Uyumlu</gray>")
        .setBillboard(Billboard.CENTER)
        .setTextShadow(true)
        .build();

hologram.spawnAsync();
```

### 4. Folia Uyumlu Sandık Menüsü (Anti-Dupe & Debounce)
```java
Menu menu = RelayAPI.getMenus().createBuilder(plugin)
        .titleMiniMessage("<green>Işınlanma Menüsü</green>")
        .pattern(
            "#########",
            "#   W   #",
            "#########"
        )
        .bindKey('#', MenuButton.of(new ItemStack(Material.BLACK_STAINED_GLASS_PANE)))
        .bindKey('W', MenuButton.builder()
                .item(new ItemStack(Material.COMPASS))
                .sound(Sound.ENTITY_ENDERMAN_TELEPORT)
                .debounce(250L) // 250ms makro koruması
                .onClick(ctx -> ctx.getPlayer().teleport(spawnLocation))
                .build())
        .build();

menu.open(player);
```

### 5. Çok Kanallı Bildirimler (Öncelikli ActionBar, BossBar & Toast)
```java
// Öncelikli ve Kesintisiz ActionBar:
RelayAPI.getNotifications().actionBar(player)
        .messageMiniMessage("<gradient:#f12711:#f5af19><bold>UYARI:</bold> Güvenli bölgeden çıktınız!</gradient>")
        .priority(NotificationPriority.HIGH)
        .duration(Duration.ofSeconds(3))
        .send(plugin);

// Animasyonlu Geri Sayımlı BossBar (%time% ve %progress% destekli):
ActiveBossBar bar = RelayAPI.getNotifications().bossBar(player)
        .titleMiniMessage("<aqua>Zindan Başlıyor</aqua> <gray>(Kalan: %time%s)</gray>")
        .countdown(Duration.ofSeconds(30))
        .color(BossBar.Color.BLUE)
        .onComplete(() -> player.sendMessage("Zindan kapıları açıldı!"))
        .send(plugin);

// Sağ Üst Sanal Toast (Advancement) Bildirimi:
RelayAPI.getNotifications().toast(player)
        .titleMiniMessage("<gold>Efsanevi Görev!</gold>")
        .descriptionMiniMessage("<gray>Ender Ejderhasını alt et</gray>")
        .icon(Material.DRAGON_HEAD)
        .frame(ToastFrame.CHALLENGE)
        .send(plugin);
```

---

## ⚙️ Yönetici Komutları

Sunucudaki aktif servisleri, telemetriyi ve oturumları anlık izleyin:

* `/relay status`: Aktif servis, konu, hologram, menü ve çalışma ortamı bilgilerini gösterir.
* `/relay services`: Sunucuda kayıtlı tüm servisleri ve öncelik derecelerini listeler.
* `/relay topics`: Aktif Pub/Sub iletişim kanallarını ve dinleyici sayılarını listeler.
* `/relay metrics`: Ortalama işlem sürelerini, P95 gecikmelerini ve sayaçları gösterir.
* `/relay metrics reset`: Tüm telemetri ve performans sayaçlarını sıfırlar.
* `/relay holograms`: Aktif TextDisplay hologramlarını ve konumlarını listeler.
* `/relay menus`: Aktif sanal menü oturumlarını ve görüntüleyen oyuncu sayılarını listeler.
* `/relay notify <oyuncu> <actionbar|bossbar|toast|title> [mesaj]`: Oyuncuya test bildirimi gönderir.

**Yetki:** `relay.admin` (Varsayılan: OP)

---

## 🔗 Bağlantılar & Kaynaklar

* 🌐 **Canlı Dokümantasyon:** https://kriscra.github.io/Relay/
* 💻 **GitHub:** https://github.com/Kriscra/Relay
* 📦 **JitPack SDK:** https://jitpack.io/#Kriscra/Relay
* 📜 **Lisans:** [MIT License](LICENSE)
