[简体中文](README.md) | English

<div align="center">

# 🔪 MurderAddon

**An addon plugin for MurderMystery that adds corpse spawning functionality**

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20+-green.svg)](https://minecraft.net)
[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://openjdk.org)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.0-purple.svg)](https://kotlinlang.org)

![Banner](banner.png)

</div>

---

## 📋 System Requirements

| Component | Version Required |
|-----------|------------------|
| **Minecraft** | 1.20+ |
| **Java** | 21+ |
| **Server** | Spigot/Paper |

## 📦 Dependencies

- [**Adyeshach**](https://github.com/TabooLib/adyeshach) - NPC entity management
- [**MurderMystery**](https://github.com/Plugily-Projects/MurderMystery) - Main game plugin

## 🚀 Installation

### 1️⃣ Install Dependencies
First, install the required prerequisite plugins:
- Download and install [Adyeshach](https://github.com/TabooLib/adyeshach)
- Download and install [MurderMystery](https://github.com/Plugily-Projects/MurderMystery)

### 2️⃣ Install Plugin
Place `MurderAddon.jar` in your server's `plugins` folder

### 3️⃣ Configure MurderMystery
**Disable** the native corpse feature in MurderMystery configuration:

```yaml
# plugins/MurderMystery/config.yml
Last-Words:
  Enable: true          # Keep last words system enabled
  Show-Hologram: true   # Keep hologram display enabled
  Show-Corpse: false    # ❌ Disable native corpse display
```

### 4️⃣ Restart Server
Restart your server to load all plugins

---

## 🎮 Usage

The plugin works automatically after installation, no additional setup required. When players die in MurderMystery games, corresponding corpses will be automatically generated.

### Admin Commands

```
/murderaddon reload  - Reload configuration
/murderaddon info    - View plugin information
```

**Permission:** `murderaddon.admin`

---

<div align="center">

**🎯 Make your MurderMystery games more realistic!**

</div>
