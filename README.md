# ⚡ Relay

> **Endüstri Standardı Reaktif Servis Kayıt & Pluginler Arası İletişim (Pub/Sub) Çatısı**  
> *Modern Minecraft sunucuları (Paper, Purpur, Folia) için Vault'un yerini alan yeni nesil mimari.*

---

## 🚀 Neden Relay? (Vault vs Relay)

| Özellik | Vault (2011) | Relay (Modern) |
| :--- | :--- | :--- |
| **İş Parçacığı (Threading)** | Senkron (Main thread'i dondurur) | **%100 Asenkron & Non-Blocking (`CompletableFuture`)** |
| **Sunucu Desteği** | Eski Bukkit/Spigot | **Paper, Purpur ve Çok Çekirdekli Folia Uyumlu** |
| **Para Birimleri** | Sadece tek bir para birimi | **Sınırsız Çoklu Para Birimi (`Currency` standartı)** |
| **Hassasiyet** | `double` (Kuruş yuvarlama hataları) | **`BigDecimal` ile %100 kayıpsız finansal işlemler** |
| **Pluginler Arası İletişim**| Yok (Doğrudan `.jar` bağımlılığı şart) | **Decoupled Pub/Sub EventBus (Bağımsız Kanal Mesajlaşması)** |
| **Yüklenme Sırası Hatası** | `NullPointerException` (Load order sorunu) | **Reaktif `awaitService` & `onServiceReady` Promise mimarisi** |
| **Öncelik & Fallback** | Bukkit ServicePriority (Sınırlı) | **Dinamik Fallback Zinciri (`HIGHEST` ➔ `FALLBACK`)** |

---

## 📦 Projeye Ekleme (Dependency)

Geliştiriciler yalnızca hafif **`relay-api`** modülünü bağımlılık olarak ekler.

### Gradle (Kotlin DSL)
```kotlin
repositories {
    mavenCentral()
    // Relay repository (veya JitPack)
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("org.vrz:relay-api:1.0.0")
}
```

### Maven (`pom.xml`)
```xml
<dependency>
    <groupId>org.vrz</groupId>
    <artifactId>relay-api</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>
```

---

## 🛠️ Hızlı Başlangıç (Quickstart)

### 1. Reaktif Servis Tüketimi (`awaitService`)
Diğer eklentinin sizden önce mi yoksa sonra mı yüklendiğini dert etmenize gerek yok. Servis kaydedildiği an geri çağrılır:

```java
import org.vrz.relay.api.RelayAPI;
import org.vrz.relay.api.economy.EconomyService;

// Ekonomi servisi hazır olduğunda tetiklenir:
RelayAPI.getServiceRegistry().onServiceReady(EconomyService.class, economy -> {
    getLogger().info("Ekonomi sistemi bağlandı! Para birimi: " + economy.getDefaultCurrency().displayName());
});
```

### 2. Kendi Servisinizi Kaydetme (Öncelikli Kayıt)
```java
import org.vrz.relay.api.RelayAPI;
import org.vrz.relay.api.service.ServicePriority;

// Kendi özel servisinizi öncelik belirterek kaydedin:
RelayAPI.getServiceRegistry().register(
    MyCustomService.class,
    new MyCustomServiceImpl(),
    ServicePriority.HIGH, // HIGHEST, HIGH, NORMAL, LOW, FALLBACK
    this
);
```

### 3. Pluginler Arası Bağımsız Mesajlaşma (Pub/Sub EventBus)
İki eklentinin birbirinin `.jar` dosyasına bağımlı kalmadan veri paylaşmasını sağlar:

#### Mesajı Yayınlama (Yayıncı Eklenti):
```java
// Klan seviye atladığında veri fırlatın:
RelayAPI.getMessenger().publish("clan:levelup", new ClanLevelPayload("Akıncılar", 10), this);
```

#### Mesajı Dinleme (Dinleyici Eklenti - Görev veya Skor Tablosu):
```java
// Klan eklentisinin JAR'ına ihtiyaç duymadan kanalı dinleyin:
RelayAPI.getMessenger().subscribe("clan:levelup", ClanLevelPayload.class, this, context -> {
    ClanLevelPayload data = context.getPayload();
    getLogger().info(data.clanName() + " klanı 10. seviyeye ulaştı! Görev puanı veriliyor.");
});
```

### 4. Soru-Cevap (RPC / Request-Response)
Bir eklentinin başka bir eklentiye asenkron soru sorup cevap almasını sağlar (REST / Microservice mantığı):

#### Cevap Verici (Provider Eklenti):
```java
// Klan eklentisi gelen sorguları yanıtlar:
RelayAPI.getMessenger().handleRequestSync("clan:get_name", UUID.class, this, (ctx, clanId) -> {
    return clanManager.getClan(clanId).getName();
});
```

#### Soru Soran (Tüketici Eklenti):
```java
// Başka bir eklenti klan adını asenkron sorar (zaman aşımı korumalı):
RelayAPI.getMessenger().request("clan:get_name", clanId, String.class, Duration.ofSeconds(2))
    .thenAccept(clanName -> player.sendMessage("Klanınız: " + clanName))
    .exceptionally(err -> {
        player.sendMessage("Klan bilgisi alınamadı (Zaman aşımı veya hata)");
        return null;
    });
```

### 5. Modern Asenkron Ekonomi İşlemleri
```java
RelayAPI.getEconomy().ifPresent(economy -> {
    UUID playerId = player.getUniqueId();
    BigDecimal amount = BigDecimal.valueOf(250.50);

    // Asenkron ve non-blocking para yatırma
    economy.deposit(playerId, amount, "Görev Ödülü").thenAccept(response -> {
        if (response.isSuccess()) {
            player.sendMessage("Hesabınıza " + economy.format(amount) + " yatırıldı!");
            player.sendMessage("Yeni Bakiyeniz: " + economy.format(response.balanceAfter()));
        } else {
            player.sendMessage("İşlem başarısız: " + response.errorMessage());
        }
    });
});
```

### 6. Evrensel Bekleme Süresi (Cooldown) Servisi
Eklentiler arası ortak, bellek sızıntısız ve otomatik temizlenen bekleme süresi yöneticisi:

```java
UUID playerId = player.getUniqueId();
String key = "ability:dash";

// 1. Cooldown kontrolü
if (RelayAPI.getCooldowns().hasCooldown(playerId, key)) {
    String remaining = RelayAPI.getCooldowns().formatRemaining(playerId, key);
    player.sendMessage("§cYetenek henüz hazır değil! Kalan süre: " + remaining);
    return;
}

// 2. Cooldown uygulama (10 saniye) ve bitiş bildirimi
RelayAPI.getCooldowns().setCooldown(playerId, key, Duration.ofSeconds(10), this, expiredEntry -> {
    player.sendMessage("§aDash yeteneğiniz tekrar hazır!");
});
```

### 7. Paylaşımlı Veri ve Metadata Havuzu (SharedDataService)
Eklentilerin oyunculara tip güvenli (`DataKey<T>`), otomatik süresi dolan (TTL) ve izlenebilir ortak veriler bağlamasını sağlar:

```java
// 1. Tip güvenli anahtar tanımlama
DataKey<Boolean> COMBAT_TAG = DataKey.of("combat", "tagged", Boolean.class, false);

// 2. Oyuncuya 15 saniyelik geçici (TTL) savaş etiketi koyma (Savaş eklentisi):
RelayAPI.getData().setTemporary(player.getUniqueId(), COMBAT_TAG, true, Duration.ofSeconds(15), this);

// 3. Başka bir eklentiden okuma (Işınlanma / Spawn eklentisi):
if (RelayAPI.getData().getOrKeyDefault(player.getUniqueId(), COMBAT_TAG)) {
    player.sendMessage("§cSavaş halindeyken ışınlanamazsınız!");
    return;
}

// 4. Değişiklikleri dinleme (Gözlemci / Observer):
RelayAPI.getData().observe(COMBAT_TAG, this, (targetId, key, oldVal, newVal) -> {
    getLogger().info("Oyuncu " + targetId + " savaş durumu değişti: " + newVal);
});
```

### 8. Asenkron Yetki ve Grup Servisi (PermissionService)
Vault'un senkron yetki sisteminin aksine, LuckPerms veya veritabanı sorgularını **TPS düşürmeden** asenkron yürüten yetki ve rütbe sistemi:

```java
// 1. Asenkron yetki kontrolü (Non-blocking):
RelayAPI.getPermissions().ifPresent(perms -> {
    UUID playerId = player.getUniqueId();

    perms.hasPermission(playerId, "vip.flight").thenAccept(canFly -> {
        if (canFly) {
            player.setAllowFlight(true);
        }
    });

    // 2. Oyuncunun rütbesini ve unvanını (prefix) alma:
    perms.getPrimaryGroup(playerId).thenAccept(group -> {
        perms.getPrefix(playerId).thenAccept(prefix -> {
            player.sendMessage("Rütbeniz: " + prefix + group);
        });
    });
});
```

### 9. Parti, Takım ve Dost Ateşi (PartyService)
Zindan, RPG, yetenek ve klan eklentileri için sıfır-gecikmeli (zero-allocation) senkron dost ateşi kontrolü ve asenkron parti yönetimi:

```java
// 1. Savaş Olaylarında Sıfır-Gecikmeli Dost Ateşi (Friendly Fire) Kontrolü (Senkron):
@EventHandler
public void onDamage(EntityDamageByEntityEvent event) {
    if (event.getDamager() instanceof Player damager && event.getEntity() instanceof Player victim) {
        RelayAPI.getParties().ifPresent(parties -> {
            // Takım arkadaşıysa ve dost ateşi kapalıysa hasarı engelle:
            if (parties.isFriendlySync(damager.getUniqueId(), victim.getUniqueId())) {
                event.setCancelled(true);
                damager.sendMessage("§cTakım arkadaşına saldıramazsın!");
            }
        });
    }
}

// 2. Asenkron Parti Oluşturma ve Üye Yönetimi:
RelayAPI.getParties().ifPresent(parties -> {
    parties.createParty(leader.getUniqueId(), "Zindan Takımı", 4).thenAccept(party -> {
        leader.sendMessage("§aParti kuruldu: " + party.getName());
        parties.addMember(party.getId(), friend.getUniqueId());
    });
});
```

---


## ⚙️ Komutlar & Yönetim

Sunucu yöneticileri için tanı ve izleme araçları:

* `/relay status`: Aktif servis sayısı, abone olunan kanal sayısı ve çalışma ortamı bilgilerini gösterir.
* `/relay services`: Kayıtlı tüm servisleri, hangi eklentinin sağladığını ve öncelik derecelerini listeler.
* `/relay topics`: Mesajlaşma sistemindeki aktif kanalları ve dinleyici sayılarını gösterir.

**Yetki:** `relay.admin` (Varsayılan: OP)

---

## 🏗️ Modül Mimarisi

* **`relay-api`**: Sıfır çalışma zamanı bağımlılığı içeren saf arayüzler, veri modelleri ve yaşam döngüsü olayları (19 KB).
* **`relay-core`**: STARTUP yaşam döngüsünde çalışan, Folia/Paper uyumlu, eşzamanlı veri yapılarıyla güçlendirilmiş sunucu eklentisi.
