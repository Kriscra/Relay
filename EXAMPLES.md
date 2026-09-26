# 📚 Relay API - Geliştirici Örnekleri (Code Examples)

Bu dökümanda Relay API'nin farklı senaryolarda nasıl kullanılacağına dair uçtan uca gerçek hayat örnekleri yer almaktadır.

---

## Örnek 1: Çoklu Para Birimli Özel Bir Ekonomi Eklentisi Yazmak

Aşağıdaki örnek, kendi veritabanını kullanan ve Relay üzerinden **Altın** ile **Kripto Token** para birimlerini sunan bir eklentiyi gösterir:

```java
package com.example.myeco;

import org.bukkit.plugin.java.JavaPlugin;
import org.vrz.relay.api.RelayAPI;
import org.vrz.relay.api.economy.Currency;
import org.vrz.relay.api.economy.EconomyResponse;
import org.vrz.relay.api.economy.EconomyService;
import org.vrz.relay.api.service.ServicePriority;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class MyCustomEconomyPlugin extends JavaPlugin implements EconomyService {

    private final Currency gold = Currency.decimal("gold", "Altın", "Altın", "⛃");
    private final Currency token = Currency.integer("token", "Token", "Token", "✦");
    private final Map<UUID, Map<String, BigDecimal>> accounts = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        // Relay'e ekonomi servisi olarak kaydol:
        RelayAPI.getServiceRegistry().register(
            EconomyService.class,
            this,
            ServicePriority.HIGHEST, // Sunucunun ana ekonomisi olarak kaydet
            this
        );
        getLogger().info("Özel ekonomi servisi Relay'e bağlandı!");
    }

    @Override
    public Currency getDefaultCurrency() {
        return gold;
    }

    @Override
    public Optional<Currency> getCurrency(String currencyId) {
        if ("gold".equalsIgnoreCase(currencyId)) return Optional.of(gold);
        if ("token".equalsIgnoreCase(currencyId)) return Optional.of(token);
        return Optional.empty();
    }

    @Override
    public Set<Currency> getSupportedCurrencies() {
        return Set.of(gold, token);
    }

    @Override
    public CompletableFuture<Boolean> hasAccount(UUID accountId) {
        return CompletableFuture.completedFuture(accounts.containsKey(accountId));
    }

    @Override
    public CompletableFuture<Boolean> createAccount(UUID accountId) {
        accounts.computeIfAbsent(accountId, k -> new ConcurrentHashMap<>());
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<BigDecimal> getBalance(UUID accountId, Currency currency) {
        Map<String, BigDecimal> userBalances = accounts.getOrDefault(accountId, Collections.emptyMap());
        return CompletableFuture.completedFuture(userBalances.getOrDefault(currency.id(), BigDecimal.ZERO));
    }

    @Override
    public CompletableFuture<Boolean> has(UUID accountId, BigDecimal amount, Currency currency) {
        return getBalance(accountId, currency).thenApply(bal -> bal.compareTo(amount) >= 0);
    }

    @Override
    public CompletableFuture<EconomyResponse> deposit(UUID accountId, BigDecimal amount, Currency currency, String reason) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return CompletableFuture.completedFuture(
                EconomyResponse.failure(EconomyResponse.Status.INVALID_AMOUNT, amount, BigDecimal.ZERO, currency, "Yatırılacak miktar pozitif olmalıdır.")
            );
        }

        Map<String, BigDecimal> userBalances = accounts.computeIfAbsent(accountId, k -> new ConcurrentHashMap<>());
        BigDecimal newBal = userBalances.merge(currency.id(), amount, BigDecimal::add);

        return CompletableFuture.completedFuture(EconomyResponse.success(amount, newBal, currency));
    }

    @Override
    public CompletableFuture<EconomyResponse> withdraw(UUID accountId, BigDecimal amount, Currency currency, String reason) {
        Map<String, BigDecimal> userBalances = accounts.computeIfAbsent(accountId, k -> new ConcurrentHashMap<>());
        BigDecimal current = userBalances.getOrDefault(currency.id(), BigDecimal.ZERO);

        if (current.compareTo(amount) < 0) {
            return CompletableFuture.completedFuture(
                EconomyResponse.failure(EconomyResponse.Status.INSUFFICIENT_FUNDS, amount, current, currency, "Yetersiz bakiye.")
            );
        }

        BigDecimal newBal = current.subtract(amount);
        userBalances.put(currency.id(), newBal);
        return CompletableFuture.completedFuture(EconomyResponse.success(amount, newBal, currency));
    }

    @Override
    public CompletableFuture<EconomyResponse> transfer(UUID from, UUID to, BigDecimal amount, Currency currency, String reason) {
        return withdraw(from, amount, currency, reason).thenCompose(withdrawResp -> {
            if (!withdrawResp.isSuccess()) {
                return CompletableFuture.completedFuture(withdrawResp);
            }
            return deposit(to, amount, currency, reason);
        });
    }
}
```

---

## Örnek 2: Dükkan Eklentisinden Güvenli Asenkron Eşya Satın Alma

Sunucuda TPS düşürmeden (MySQL/Redis çağrısını bekleterek donma yaratmadan) işlem yapma:

```java
public void buyShopItem(Player player, String itemId, BigDecimal price) {
    RelayAPI.getEconomy().ifPresentOrElse(economy -> {
        UUID playerId = player.getUniqueId();

        // 1. Asenkron bakiye kontrolü ve çekme
        economy.withdraw(playerId, price, "Dükkan: " + itemId).thenAccept(response -> {
            if (response.isSuccess()) {
                // Oyuncuya eşyayı güvenle teslim et
                player.sendMessage("§aBaşarıyla satın alındı! Kalan Bakiye: " + economy.format(response.balanceAfter()));
                giveItem(player, itemId);
            } else {
                player.sendMessage("§cSatın alım başarısız: " + response.errorMessage());
            }
        });
    }, () -> {
        player.sendMessage("§cSunucuda aktif bir ekonomi eklentisi bulunamadı!");
    });
}
```

---

## Örnek 3: Pluginler Arası Bağımsız Haberleşme (Klan & Görev Sistemi)

Bir klan eklentisi ile görev eklentisinin birbirinin kodunu bilmeden çalışması:

### Ortak Veri Modeli (Payload):
```java
public record ClanLevelPayload(String clanId, String clanName, int newLevel) {}
```

### Yayıncı (UltimateClans):
```java
public void onClanLevelUp(String clanId, String name, int level) {
    // Sadece kanala mesaj gönder, dinleyicinin kim olduğu önemli değil:
    RelayAPI.getMessenger().publish("clan:level_up", new ClanLevelPayload(clanId, name, level), this);
}
```

### Dinleyici (QuestSystem):
```java
@Override
public void onEnable() {
    // UltimateClans'a hard dependency OLMADAN kanala abone ol:
    RelayAPI.getMessenger().subscribe("clan:level_up", ClanLevelPayload.class, this, context -> {
        ClanLevelPayload payload = context.getPayload();
        
        if (payload.newLevel() >= 5) {
            getLogger().info(payload.clanName() + " klan üyelerinin 5. seviye klan görevi tamamlandı!");
        }
    });
}
```

---

## Örnek 4: Soru-Cevap (RPC) ile Eklentiler Arası Veri Çekme

Bir Skor Tablosu (Scoreboard) eklentisinin, Klan eklentisinden oyuncunun klan adını asenkron sorgulaması:

### 1. Ortak Veri Modelleri:
```java
public record ClanQueryRequest(UUID playerId) {}
public record ClanQueryResponse(boolean hasClan, String clanName, int memberCount) {}
```

### 2. Cevap Veren (Klan Eklentisi):
```java
@Override
public void onEnable() {
    // Skor tablosu veya diğer eklentilerden gelecek "clan:query" isteklerini karşıla:
    RelayAPI.getMessenger().handleRequestSync(
        "clan:query",
        ClanQueryRequest.class,
        this,
        (context, request) -> {
            Clan clan = clanManager.getByPlayer(request.playerId());
            if (clan == null) {
                return new ClanQueryResponse(false, "Yok", 0);
            }
            return new ClanQueryResponse(true, clan.getName(), clan.getSize());
        }
    );
}
```

### 3. Soru Soran (Scoreboard / Profil Eklentisi):
```java
public void updateScoreboard(Player player) {
    // 2 saniye zaman aşımı ile sorgula, ana iş parçacığını (TPS) ASLA dondurma:
    RelayAPI.getMessenger().request(
        "clan:query",
        new ClanQueryRequest(player.getUniqueId()),
        ClanQueryResponse.class,
        Duration.ofSeconds(2)
    ).thenAccept(response -> {
        // Gelen yanıt ile oyuncunun tablosunu güncelle:
        if (response.hasClan()) {
            player.sendMessage("§eKlanınız: §b" + response.clanName() + " §7(Üye: " + response.memberCount() + ")");
        } else {
            player.sendMessage("§7Herhangi bir klana üye değilsiniz.");
        }
    }).exceptionally(throwable -> {
        // Zaman aşımı veya eklenti kapalıysa hata yönetimi:
        getLogger().warning("Klan sorgusu zaman aşımına uğradı: " + throwable.getMessage());
        return null;
    });
}
```

---

## Örnek 5: Cooldown Servisi ile Yetenek & Bitiş Bildirimi

Bir Özel Yetenek eklentisinde Cooldown kontrolü, ActionBar ilerleme çubuğu ve süre bitişi bildirimi:

```java
public void castLightningStrike(Player player) {
    UUID uuid = player.getUniqueId();
    String key = "skill:lightning";

    // 1. Cooldown kontrolü
    if (RelayAPI.getCooldowns().hasCooldown(uuid, key)) {
        RelayAPI.getCooldowns().getCooldown(uuid, key).ifPresent(entry -> {
            String remaining = RelayAPI.getCooldowns().formatRemaining(uuid, key);
            int percent = (int) (entry.getProgress() * 100);

            // ActionBar üzerinde kalan süreyi ve dolum yüzdesini göster:
            player.sendActionBar(Component.text("§eYıldırım Yeteneği Beklemede: §c" + remaining + " §7(%" + percent + ")"));
        });
        return;
    }

    // 2. Yeteneği uygula
    player.getWorld().strikeLightning(player.getTargetBlockExact(30).getLocation());
    player.sendMessage("§6⚡ Yıldırım fırlatıldı!");

    // 3. 15 saniyelik Cooldown başlat ve süre dolduğunda oyuncuya haber ver:
    RelayAPI.getCooldowns().setCooldown(uuid, key, Duration.ofSeconds(15), this, expired -> {
        if (player.isOnline()) {
            player.sendMessage("§a⚡ Yıldırım yeteneğiniz tekrar kullanıma hazır!");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        }
    });
}
```

---

## Örnek 6: Paylaşımlı Veri ile Savaş Durumu (Combat Tag) ve Işınlanma Engeli

İki farklı eklentinin (Savaş eklentisi ve Spawn/Işınlanma eklentisi) oyuncunun savaş durumunu paylaşması:

### Ortak Anahtar Tanımı:
```java
public class SharedKeys {
    public static final DataKey<Boolean> COMBAT_TAG = DataKey.of("combat", "tagged", Boolean.class, false);
}
```

### 1. Savaş Eklentisi (Hasar alındığında 15 saniyelik otomatik silinen etiket koyar):
```java
@EventHandler
public void onEntityDamage(EntityDamageByEntityEvent event) {
    if (event.getEntity() instanceof Player victim && event.getDamager() instanceof Player attacker) {
        // 15 saniyelik geçici (TTL) savaş etiketi koy (Süre dolunca otomatik silinir!):
        RelayAPI.getData().setTemporary(victim.getUniqueId(), SharedKeys.COMBAT_TAG, true, Duration.ofSeconds(15), this);
        RelayAPI.getData().setTemporary(attacker.getUniqueId(), SharedKeys.COMBAT_TAG, true, Duration.ofSeconds(15), this);

        victim.sendMessage("§cSavaşa girdiniz! 15 saniye boyunca ışınlanamazsınız.");
        attacker.sendMessage("§cSavaşa girdiniz! 15 saniye boyunca ışınlanamazsınız.");
    }
}
```

### 2. Işınlanma Eklentisi (/spawn komutu çalıştırıldığında kontrol eder):
```java
public boolean handleSpawnTeleport(Player player) {
    // Savaş eklentisine BAĞIMLI OLMADAN oyuncunun savaş durumunu oku:
    boolean inCombat = RelayAPI.getData().getOrKeyDefault(player.getUniqueId(), SharedKeys.COMBAT_TAG);

    if (inCombat) {
        player.sendMessage("§cIşınlanma engellendi: Hâlâ savaş halindesiniz!");
        return false;
    }

    player.teleport(spawnLocation);
    player.sendMessage("§aBaşarıyla başlangıç noktasına ışınlandınız.");
    return true;
}
```

---

## Örnek 7: Asenkron Rütbe Kontrolü ve Yetki Yönetimi

Bir VIP veya Mini-Oyun eklentisinde oyuncunun rütbesini sorgulama ve ödül yetkisi verme:

```java
public void claimDailyVipReward(Player player) {
    RelayAPI.getPermissions().ifPresentOrElse(perms -> {
        UUID uuid = player.getUniqueId();

        // 1. Asenkron olarak oyuncunun bir gruba ait olup olmadığını sorgula:
        perms.inGroup(uuid, "vip").thenAccept(isVip -> {
            if (!isVip) {
                player.sendMessage("§cBu ödülü sadece VIP üyeler alabilir!");
                return;
            }

            // 2. Oyuncunun rütbe ön ekini alıp mesaj gönder:
            perms.getPrefix(uuid).thenAccept(prefix -> {
                player.sendMessage(prefix + "§aGünlük VIP ödülünüz teslim edildi!");
                giveRewardItems(player);
            });
        });
    }, () -> {
        player.sendMessage("§cSunucuda yetki servisi aktif değil.");
    });
}
```

---

## Örnek 8: Parti Yönetimi, Zindan Grupları ve Dost Ateşi (Friendly Fire) Koruması

Bir RPG/Savaş eklentisinde veya Zindan (Dungeon) yöneticisinde partileri yönetme ve PVP hasarını engelleme:

### 1. Sıfır-Gecikmeli (Lock-Free) Savaş Hasar Dinleyicisi
```java
package com.example.combat;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.vrz.relay.api.RelayAPI;

public class FriendlyFireListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager) || !(event.getEntity() instanceof Player victim)) {
            return;
        }

        RelayAPI.getParties().ifPresent(parties -> {
            // isFriendlySync: O(1) sabit sürede, main/region thread'i dondurmadan kontrol eder.
            // Eğer aynı partideyseler ve parti içi hasar kapalıysa true döner.
            if (parties.isFriendlySync(damager.getUniqueId(), victim.getUniqueId())) {
                event.setCancelled(true);
                damager.sendMessage("§c" + victim.getName() + " senin takım arkadaşın! Ona saldıramazsın.");
            }
        });
    }
}
```

### 2. Zindan Eklentisi: Parti Oluşturma ve Üye Kontrolü
```java
package com.example.dungeon;

import org.bukkit.entity.Player;
import org.vrz.relay.api.RelayAPI;
import org.vrz.relay.api.party.Party;

import java.util.UUID;

public class DungeonManager {

    /**
     * Zindana girmek isteyen lider için parti oluşturur.
     */
    public void createDungeonParty(Player leader, String teamName) {
        RelayAPI.getParties().ifPresent(parties -> {
            parties.createParty(leader.getUniqueId(), teamName, 4) // Maksimum 4 kişilik grup
                .thenAccept(party -> {
                    leader.sendMessage("§aZindan partisi oluşturuldu! Kod: " + party.getId());
                })
                .exceptionally(err -> {
                    leader.sendMessage("§cZaten bir partidesiniz!");
                    return null;
                });
        });
    }

    /**
     * Tüm parti üyelerini zindana ışınlar.
     */
    public void enterDungeon(Player player) {
        RelayAPI.getParties().ifPresent(parties -> {
            parties.getParty(player.getUniqueId()).thenAccept(optParty -> {
                if (optParty.isEmpty()) {
                    player.sendMessage("§cZindana girmek için bir partide olmalısınız!");
                    return;
                }

                Party party = optParty.get();
                if (!party.isLeader(player.getUniqueId())) {
                    player.sendMessage("§cSadece parti lideri zindanı başlatabilir!");
                    return;
                }

                player.sendMessage("§aZindan açılıyor! Grup üye sayısı: " + party.getSize());
                for (UUID memberId : party.getMembers()) {
                    teleportToDungeon(memberId);
                }
            });
        });
    }

    private void teleportToDungeon(UUID playerId) {
        // Işınlanma mantığı...
    }
}
```

