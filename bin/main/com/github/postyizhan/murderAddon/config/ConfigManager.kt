package com.github.postyizhan.murderAddon.config

import com.github.postyizhan.murderAddon.MurderAddon
import org.bukkit.ChatColor
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * 配置管理器
 * Configuration Manager
 * 
 * 负责处理插件配置和国际化消息
 * Handles plugin configuration and internationalization messages
 */
class ConfigManager(private val plugin: MurderAddon) {
    
    private var messagesConfig: FileConfiguration? = null
    private var messagesFile: File? = null
    
    // 配置项缓存 / Configuration cache
    var language: String = "zh_CN"
        private set
    var corpseDuration: Int = 30
        private set
    var showCorpseName: Boolean = true
        private set
    var corpseNameFormat: String = "&7{player} 的尸体"
        private set
    var hideFromTabList: Boolean = true
        private set

    // 位置偏移配置 / Position offset configuration
    var xOffset: Double = 0.0
        private set
    var yOffset: Double = 0.0
        private set
    var zOffset: Double = 0.0
        private set

    // 朝向配置 / Rotation configuration
    var rotationMode: String = "KEEP_DEATH"
        private set
    var fixedYaw: Float = 0.0f
        private set
    var fixedPitch: Float = 0.0f
        private set

    // 亡语配置 / Death message configuration
    var deathMessageEnabled: Boolean = false
        private set
    var deathMessageDuration: Int = 5
        private set
    var deathMessageHeightOffset: Double = 2.0
        private set

    // 高级配置 / Advanced configuration
    var corpseCreationDelay: Long = 10L
        private set
    var gameStateCheckDelay: Long = 40L
        private set
    var debugEnabled: Boolean = false
        private set
    var debugPrefix: String = "&8[&6MurderAddon-Debug&8]&7 "
        private set
    
    /**
     * 初始化配置管理器
     * Initialize configuration manager
     */
    fun initialize() {
        // 保存默认配置文件 / Save default config files
        plugin.saveDefaultConfig()
        
        // 加载配置 / Load configuration
        loadConfig()
        
        // 加载消息文件 / Load messages file
        loadMessages()
        
        plugin.logger.info("Configuration loaded with language: $language")
    }
    
    /**
     * 加载主配置文件
     * Load main configuration file
     */
    private fun loadConfig() {
        plugin.reloadConfig()
        val config = plugin.config
        
        language = config.getString("language", "zh_CN") ?: "zh_CN"
        corpseDuration = config.getInt("corpse.duration", 30)
        showCorpseName = config.getBoolean("corpse.show-name", true)
        corpseNameFormat = config.getString("corpse.name-format", "&7{player} 的尸体") ?: "&7{player} 的尸体"
        hideFromTabList = config.getBoolean("corpse.hide-from-tab-list", true)

        // 加载位置偏移配置 / Load position offset configuration
        xOffset = config.getDouble("corpse.position-offset.x", 0.0)
        yOffset = config.getDouble("corpse.position-offset.y", 0.0)
        zOffset = config.getDouble("corpse.position-offset.z", 0.0)

        // 加载朝向配置 / Load rotation configuration
        rotationMode = config.getString("corpse.rotation.mode", "KEEP_DEATH") ?: "KEEP_DEATH"
        fixedYaw = config.getDouble("corpse.rotation.fixed.yaw", 0.0).toFloat()
        fixedPitch = config.getDouble("corpse.rotation.fixed.pitch", 0.0).toFloat()

        // 加载亡语配置 / Load death message configuration
        deathMessageEnabled = config.getBoolean("death-message.enabled", false)
        deathMessageDuration = config.getInt("death-message.duration", 5)
        deathMessageHeightOffset = config.getDouble("death-message.height-offset", 2.0)

        // 加载高级配置 / Load advanced configuration
        corpseCreationDelay = config.getLong("advanced.corpse-creation-delay", 10L)
        gameStateCheckDelay = config.getLong("advanced.game-state-check-delay", 40L)
        debugEnabled = config.getBoolean("debug.enabled", false)
        debugPrefix = config.getString("debug.prefix", "&8[&6MurderAddon-Debug&8]&7 ") ?: "&8[&6MurderAddon-Debug&8]&7 "
    }
    
    /**
     * 加载消息文件
     * Load messages file
     */
    private fun loadMessages() {
        val fileName = "messages_$language.yml"
        messagesFile = File(plugin.dataFolder, fileName)
        
        // 如果文件不存在，从资源中复制 / Copy from resources if file doesn't exist
        if (!messagesFile!!.exists()) {
            plugin.saveResource(fileName, false)
        }
        
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile!!)
        
        // 加载默认消息作为备用 / Load default messages as fallback
        val defaultStream = plugin.getResource(fileName)
        if (defaultStream != null) {
            val defaultConfig = YamlConfiguration.loadConfiguration(
                InputStreamReader(defaultStream, StandardCharsets.UTF_8)
            )
            messagesConfig!!.setDefaults(defaultConfig)
        }
    }
    
    /**
     * 重新加载配置
     * Reload configuration
     */
    fun reload() {
        loadConfig()
        loadMessages()
        plugin.logger.info("Configuration reloaded")
    }
    
    /**
     * 获取消息
     * Get message
     * 
     * @param path 消息路径 / Message path
     * @param placeholders 占位符替换 / Placeholder replacements
     * @return 格式化的消息 / Formatted message
     */
    fun getMessage(path: String, vararg placeholders: Pair<String, String>): String {
        var message = messagesConfig?.getString(path) ?: path
        
        // 替换占位符 / Replace placeholders
        for ((placeholder, value) in placeholders) {
            message = message.replace("{$placeholder}", value)
        }
        
        return ChatColor.translateAlternateColorCodes('&', message)
    }
    
    /**
     * 获取调试消息（硬编码英文）
     * Get debug message (hardcoded English)
     */
    fun getDebugMessage(message: String, vararg placeholders: Pair<String, String>): String {
        var debugMsg = message
        
        // 替换占位符 / Replace placeholders
        for ((placeholder, value) in placeholders) {
            debugMsg = debugMsg.replace("{$placeholder}", value)
        }
        
        return ChatColor.translateAlternateColorCodes('&', debugPrefix) + debugMsg
    }
    
    /**
     * 格式化尸体名称
     * Format corpse name
     */
    fun formatCorpseName(playerName: String): String {
        return ChatColor.translateAlternateColorCodes('&', 
            corpseNameFormat.replace("{player}", playerName)
        )
    }
}
