# 🐉 DragonEgg

<p align="center">
  <img src="https://raw.githubusercontent.com/Ab0hasiba/mc_dragoneggbli/main/src/main/resources/plugin.yml" alt="" width="0" height="0">
  <b>A legendary, high-performance Dragon Egg artifact plugin for Paper 1.21.x</b><br>
  Transforms the vanilla Dragon Egg into a contested, game-changing artifact granting god-tier attributes and buffs while strictly preventing hoarding or safe container storage.
</p>

<p align="center">
  <a href="https://papermc.io/"><img src="https://img.shields.io/badge/Paper-1.21.11-brightgreen.svg?style=for-the-badge&logo=minecraft" alt="Paper"></a>
  <a href="https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html"><img src="https://img.shields.io/badge/Java-21-orange.svg?style=for-the-badge&logo=openjdk" alt="Java"></a>
  <a href="https://gradle.org/"><img src="https://img.shields.io/badge/Built%20with-Gradle-02303A.svg?style=for-the-badge&logo=gradle" alt="Gradle"></a>
  <a href="https://github.com/Ab0hasiba/mc_dragoneggbli/releases"><img src="https://img.shields.io/badge/Release-v1.0.0-blue.svg?style=for-the-badge" alt="Release"></a>
</p>

---

## ✨ Features

### ⚔️ Legendary Carrier Buffs
Carrying the Dragon Egg in your inventory immediately awakens ancient draconic powers:
- **Extra Hearts (+10 Hearts / +20 HP by default):** Grants immense resilience while held.
- **Potion Effects:**
  - 💥 **Strength II**
  - ⚡ **Speed II**
  - 🛡️ **Resistance I**
  - 🔥 **Fire Resistance**
  - ⛏️ **Haste II**
  - 💖 **Regeneration I**
  - 🌊 **Water Breathing**
- **Non-Intrusive FX:** Ambient particles with icons to ensure carrier visibility remains clean during intense combat.

### 🛡️ Smart Modifier & Stacking Engine (100% Plugin Compatible)
- **Namespaced Attribute Modifier:** Health bonuses are applied using a dedicated `NamespacedKey` (`dragonegg:dragon_egg_health`) via the modern Paper Attributes API.
- **Zero Conflict with Lifesteal & RankedSMP:** Never overrides base player health or wipes modifiers belonging to other plugins (Lifesteal, RankedSMP, RPG attributes, or custom armor).
- **Safe Health Clamping:** Gracefully reverts maximum health without dealing lethal self-damage when the egg leaves the player's possession.
- **Potion Backup & Restore:** If a player already drank a higher-tier potion before picking up the egg, the plugin will never downgrade it. Once the egg is dropped or lost, legitimate potion buffs that were active beforehand are preserved and restored.

### 🔒 Anti-Hoarding & Exploit-Proof Container Protection
The Dragon Egg is designed to be an **active, world-contested artifact**—not locked away in a safe or hidden base:
- ❌ **Container Storage Blocked:** Prevents storing the egg in Chests, Double Chests, Trapped Chests, Barrels, Shulker Boxes, Hoppers, Droppers, Dispensers, Ender Chests, Crafters, etc.
- ❌ **Anti-Inventory Tricks:** Blocks shift-click insertion, number-key hotbar swapping, off-hand swapping, and double-click collector actions into external inventories.
- ❌ **Item Bundles & Storage Blocks:** Prevents inserting the egg into vanilla 1.21 Bundles, Decorated Pots, Chiseled Bookshelves, or Jukeboxes.
- ❌ **Entity Storage Protected:** Cannot be placed inside Armor Stands, Item Frames, Glow Item Frames, Storage Minecarts, Hopper Minecarts, or Chested Animals (Horses, Donkeys, Mules, Llamas).
- ❌ **Automated Redstone Blocked:** Hoppers, droppers, and hopper minecarts cannot move or suck up dropped Dragon Eggs from the ground.
- ❌ **Block Placement Prevention:** Prevents placing the egg on the ground as a block (`prevent-block-placement: true`), guaranteeing it remains strictly an inventory artifact or dropped item entity.

---

## ⚙️ Configuration (`config.yml`)

The plugin is fully customizable with instant reload support:

```yaml
# ========================================================
# 🐉 DragonEgg Configuration — Paper 1.21.11
# ========================================================

dragon-egg:
  enabled: true

  # Configurable potion effects granted while directly carrying the Dragon Egg.
  # Note: amplifier levels are standard 1-based Minecraft levels:
  # amplifier: 1 -> Level I
  # amplifier: 2 -> Level II
  # amplifier: 3 -> Level III
  effects:
    strength:
      enabled: true
      amplifier: 2

    speed:
      enabled: true
      amplifier: 2

    resistance:
      enabled: true
      amplifier: 1

    fire-resistance:
      enabled: true

    haste:
      enabled: true
      amplifier: 2

    regeneration:
      enabled: true
      amplifier: 1

    water-breathing:
      enabled: true

  # Extra hearts added to the player's maximum health.
  # 1 heart = 2 HP (10 hearts = +20 HP).
  # Stacks safely with RankedSMP, Lifesteal, and all other plugins!
  extra-hearts:
    enabled: true
    amount: 10

  # Scheduled safety check interval (in ticks) to ensure carrier state is verified.
  # 20 ticks = 1 second. 10 ticks = 0.5 seconds.
  check-interval-ticks: 10

  # Container & world storage protection
  container-protection:
    # If true, prevents placing the Dragon Egg as a block so it remains strictly an inventory artifact or dropped item.
    prevent-block-placement: true

# Configurable messages with color code support (&)
messages:
  gained: "&5🐉 The Dragon Egg empowers you!"
  lost: "&c🐉 The Dragon Egg's power has left you."
  cannot-store: "&cThe Dragon Egg cannot be stored in containers."
  reloaded: "&aDragonEgg configuration reloaded."
  no-permission: "&cYou don't have permission to do that."
```

---

## ⌨️ Commands & Permissions

| Command | Description | Permission | Default |
| :--- | :--- | :--- | :--- |
| `/dragonegg reload` | Reloads `config.yml` and refreshes tasks | `dragonegg.admin` | OP |

---

## 🚀 Installation

1. Download the latest `DragonEgg-1.0.0.jar` from [Releases](https://github.com/Ab0hasiba/mc_dragoneggbli/releases).
2. Place the jar into your server's `plugins/` directory.
3. Start or restart your server (running **Paper 1.21+** on **Java 21+**).
4. Edit `plugins/DragonEgg/config.yml` to your liking and execute `/dragonegg reload`.

---

## 🛠️ Building from Source

To compile and build the plugin from source code:

### Prerequisites
- JDK 21 or higher installed
- Git

### Build Steps
```bash
# Clone the repository
git clone https://github.com/Ab0hasiba/mc_dragoneggbli.git
cd mc_dragoneggbli

# Build with Gradle wrapper
./gradlew build
```

The compiled plugin jar will be generated in `build/libs/DragonEgg-1.0.0.jar`.

To run the automated unit tests:
```bash
./gradlew test
```

---

## 🧪 Testing & Reliability

The codebase features a robust JUnit 5 and Mockito test suite ensuring:
- **Isolation of Health Modifiers:** Verifies player health modifiers stack and unstack without touching other plugins.
- **Config Validation:** Ensures 1-based amplifier conversions and default effect states function accurately.
- **Scan & Event Cycles:** Tests carrier tracking on pickup, drop, death, and server safety checks.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
