package com.github.postyizhan.murderAddon

import com.github.postyizhan.murderAddon.command.MurderAddonCommand
import com.github.postyizhan.murderAddon.config.ConfigManager
import com.github.postyizhan.murderAddon.listener.MurderMysteryListener
import com.github.postyizhan.murderAddon.manager.CorpseManager
import com.github.postyizhan.murderAddon.manager.DeathMessageManager
import org.bukkit.plugin.java.JavaPlugin

/**
 * MurderAddon 主插件类
 * MurderAddon Main Plugin Class
 *
 * 为 MurderMystery 游戏添加尸体生成功能的附属插件
 * An addon plugin that adds corpse spawning functionality to MurderMystery games
 */
class MurderAddon : JavaPlugin() {

    // 配置管理器 / Configuration manager
    lateinit var configManager: ConfigManager
        private set

    // 尸体管理器 / Corpse manager
    lateinit var corpseManager: CorpseManager
        private set

    // 亡语管理器 / Death message manager
    lateinit var deathMessageManager: DeathMessageManager
        private set

    // 事件监听器 / Event listener
    private var eventListener: MurderMysteryListener? = null

    override fun onEnable() {
        try {
            // 检查依赖插件 / Check dependency plugins
            if (!checkDependencies()) {
                logger.severe("Required dependencies not found! Disabling plugin...")
                server.pluginManager.disablePlugin(this)
                return
            }

            // 初始化配置管理器 / Initialize configuration manager
            configManager = ConfigManager(this)
            configManager.initialize()

            // 初始化尸体管理器 / Initialize corpse manager
            corpseManager = CorpseManager(this)
            corpseManager.initialize()

            // 初始化亡语管理器 / Initialize death message manager
            deathMessageManager = DeathMessageManager(this)
            deathMessageManager.initialize()

            // 注册事件监听器 / Register event listener
            eventListener = MurderMysteryListener(this)
            server.pluginManager.registerEvents(eventListener!!, this)

            // 注册命令 / Register commands
            val commandHandler = MurderAddonCommand(this)
            getCommand("murderaddon")?.setExecutor(commandHandler)
            getCommand("murderaddon")?.tabCompleter = commandHandler

            // 插件启用成功 / Plugin enabled successfully
            logger.info(configManager.getMessage("plugin.enabled"))

        } catch (e: Exception) {
            logger.severe("Failed to enable MurderAddon: ${e.message}")
            e.printStackTrace()
            server.pluginManager.disablePlugin(this)
        }
    }

    override fun onDisable() {
        try {
            // 关闭尸体管理器 / Shutdown corpse manager
            if (::corpseManager.isInitialized) {
                corpseManager.shutdown()
            }

            // 关闭亡语管理器 / Shutdown death message manager
            if (::deathMessageManager.isInitialized) {
                deathMessageManager.shutdown()
            }

            // 插件禁用消息 / Plugin disabled message
            if (::configManager.isInitialized) {
                logger.info(configManager.getMessage("plugin.disabled"))
            } else {
                logger.info("MurderAddon disabled")
            }

        } catch (e: Exception) {
            logger.severe("Error during plugin shutdown: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 检查依赖插件
     * Check dependency plugins
     */
    private fun checkDependencies(): Boolean {
        val pluginManager = server.pluginManager

        // 检查 MurderMystery 插件 / Check MurderMystery plugin
        if (!pluginManager.isPluginEnabled("MurderMystery")) {
            logger.severe("MurderMystery plugin not found or not enabled!")
            return false
        }

        // 检查 Adyeshach 插件 / Check Adyeshach plugin
        if (!pluginManager.isPluginEnabled("Adyeshach")) {
            logger.severe("Adyeshach plugin not found or not enabled!")
            return false
        }

        logger.info("All required dependencies found")
        return true
    }

    /**
     * 重新加载插件配置
     * Reload plugin configuration
     */
    fun reloadPlugin() {
        try {
            configManager.reload()
            logger.info(configManager.getMessage("plugin.reload"))
        } catch (e: Exception) {
            logger.severe("Failed to reload configuration: ${e.message}")
            e.printStackTrace()
        }
    }
}
