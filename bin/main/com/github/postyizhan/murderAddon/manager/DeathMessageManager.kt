package com.github.postyizhan.murderAddon.manager

import com.github.postyizhan.murderAddon.MurderAddon
import ink.ptms.adyeshach.core.Adyeshach
import ink.ptms.adyeshach.core.entity.manager.ManagerType
import ink.ptms.adyeshach.core.entity.type.AdyArmorStand
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 亡语管理器
 * Death Message Manager
 * 
 * 负责管理自定义亡语浮空字的显示和生命周期
 * Manages the display and lifecycle of custom death message floating text
 */
class DeathMessageManager(private val plugin: MurderAddon) {
    
    // 存储所有亡语浮空字数据 / Store all death message floating text data
    private val deathMessages = ConcurrentHashMap<UUID, DeathMessageData>()
    
    // 清理任务 / Cleanup task
    private var cleanupTask: BukkitTask? = null
    
    /**
     * 初始化亡语管理器
     * Initialize death message manager
     */
    fun initialize() {
        if (plugin.configManager.deathMessageEnabled) {
            // 启动定期清理任务 / Start periodic cleanup task
            startCleanupTask()
            
            if (plugin.configManager.debugEnabled) {
                plugin.logger.info(plugin.configManager.getDebugMessage("DeathMessageManager initialized successfully"))
            }
        }
    }
    
    /**
     * 关闭亡语管理器
     * Shutdown death message manager
     */
    fun shutdown() {
        // 取消清理任务 / Cancel cleanup task
        cleanupTask?.cancel()
        
        // 清理所有亡语浮空字 / Clean up all death messages
        cleanupAllDeathMessages()
        
        if (plugin.configManager.deathMessageEnabled) {
            plugin.logger.info("DeathMessageManager shutdown completed")
        }
    }
    
    /**
     * 创建亡语浮空字
     * Create death message floating text
     * 
     * @param deadPlayer 死亡的玩家 / Dead player
     * @param location 浮空字生成位置 / Floating text spawn location
     * @param message 亡语内容 / Death message content
     */
    fun createDeathMessage(deadPlayer: Player, location: Location, message: String) {
        if (!plugin.configManager.deathMessageEnabled) {
            return
        }
        
        try {
            // 检查世界是否有效 / Check if world is valid
            if (location.world == null) {
                plugin.logger.warning("Cannot create death message for ${deadPlayer.name}: location world is null")
                return
            }

            // 调整位置高度 / Adjust location height
            val adjustedLocation = location.clone().apply {
                y += plugin.configManager.deathMessageHeightOffset
            }

            // 获取 Adyeshach 管理器 / Get Adyeshach manager
            val manager = Adyeshach.api().getPublicEntityManager(ManagerType.TEMPORARY)
            
            // 创建盔甲架作为浮空字载体 / Create armor stand as floating text carrier
            val messageEntity = manager.create(ink.ptms.adyeshach.core.entity.EntityTypes.ARMOR_STAND, adjustedLocation) { entity ->
                val armorStand = entity as AdyArmorStand
                
                // 设置基本属性 / Set basic properties
                armorStand.setCustomName(message)
                armorStand.setCustomNameVisible(true)
                // 设置盔甲架属性 / Set armor stand properties
                try {
                    armorStand.setSmall(true) // 设置为小型盔甲架 / Set as small armor stand
                    armorStand.setMarker(true) // 设置为标记模式，不可交互 / Set as marker mode, non-interactive
                    armorStand.setGravity(false) // 禁用重力 / Disable gravity
                } catch (e: Exception) {
                    // 如果某些方法不可用，忽略错误 / Ignore errors if some methods are not available
                    plugin.logger.warning("Some armor stand properties could not be set: ${e.message}")
                }
            }
            
            // 创建亡语数据 / Create death message data
            val deathMessageData = DeathMessageData(
                playerId = deadPlayer.uniqueId,
                playerName = deadPlayer.name,
                entity = messageEntity as AdyArmorStand,
                location = adjustedLocation,
                spawnTime = System.currentTimeMillis(),
                message = message
            )
            
            // 存储亡语数据 / Store death message data
            deathMessages[deadPlayer.uniqueId] = deathMessageData
            
            // 发送调试消息 / Send debug message
            if (plugin.configManager.debugEnabled) {
                val worldName = adjustedLocation.world?.name ?: "unknown"
                plugin.logger.info(plugin.configManager.getDebugMessage(
                    "Death message created for player {player} at {location}: {message}",
                    "player" to deadPlayer.name,
                    "location" to "$worldName:${adjustedLocation.blockX},${adjustedLocation.blockY},${adjustedLocation.blockZ}",
                    "message" to message
                ))
            }
            
        } catch (e: Exception) {
            plugin.logger.severe("Failed to create death message for player ${deadPlayer.name}: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * 移除指定玩家的亡语浮空字
     * Remove death message for specified player
     * 
     * @param playerId 玩家 UUID / Player UUID
     */
    fun removeDeathMessage(playerId: UUID) {
        val deathMessageData = deathMessages.remove(playerId) ?: return
        
        try {
            // 销毁实体 / Destroy entity
            deathMessageData.entity.remove()
            
            if (plugin.configManager.debugEnabled) {
                plugin.logger.info(plugin.configManager.getDebugMessage(
                    "Death message removed for player {player}",
                    "player" to deathMessageData.playerName
                ))
            }
            
        } catch (e: Exception) {
            plugin.logger.severe("Failed to remove death message for player ${deathMessageData.playerName}: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * 清理过期的亡语浮空字
     * Clean up expired death messages
     */
    private fun cleanupExpiredDeathMessages() {
        if (plugin.configManager.deathMessageDuration == -1) {
            return // 使用默认时间，不进行清理 / Use default duration, no cleanup
        }
        
        val currentTime = System.currentTimeMillis()
        val expireDuration = plugin.configManager.deathMessageDuration * 1000L
        val toRemove = mutableListOf<UUID>()
        
        deathMessages.forEach { (playerId, deathMessageData) ->
            if (currentTime - deathMessageData.spawnTime > expireDuration) {
                toRemove.add(playerId)
            }
        }
        
        toRemove.forEach { playerId ->
            removeDeathMessage(playerId)
        }
        
        if (toRemove.isNotEmpty() && plugin.configManager.debugEnabled) {
            plugin.logger.info(plugin.configManager.getDebugMessage(
                "Cleaned up {count} expired death messages",
                "count" to toRemove.size.toString()
            ))
        }
    }
    
    /**
     * 清理所有亡语浮空字
     * Clean up all death messages
     */
    private fun cleanupAllDeathMessages() {
        val count = deathMessages.size
        deathMessages.keys.toList().forEach { playerId ->
            removeDeathMessage(playerId)
        }
        
        if (count > 0) {
            plugin.logger.info("Cleaned up $count death messages during shutdown")
        }
    }
    
    /**
     * 启动清理任务
     * Start cleanup task
     */
    private fun startCleanupTask() {
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            cleanupExpiredDeathMessages()
        }, 20L * 2, 20L * 1) // 每1秒检查一次 / Check every 1 second
        
        if (plugin.configManager.debugEnabled) {
            plugin.logger.info(plugin.configManager.getDebugMessage("Starting death message cleanup task"))
        }
    }
    
    /**
     * 获取当前亡语浮空字数量
     * Get current death message count
     */
    fun getDeathMessageCount(): Int = deathMessages.size
    
    /**
     * 检查玩家是否有亡语浮空字
     * Check if player has a death message
     */
    fun hasDeathMessage(playerId: UUID): Boolean = deathMessages.containsKey(playerId)
}

/**
 * 亡语浮空字数据类
 * Death Message Floating Text Data Class
 */
data class DeathMessageData(
    /** 玩家 UUID / Player UUID */
    val playerId: UUID,
    
    /** 玩家名称 / Player name */
    val playerName: String,
    
    /** Adyeshach 盔甲架实体 / Adyeshach armor stand entity */
    val entity: AdyArmorStand,
    
    /** 浮空字位置 / Floating text location */
    val location: Location,
    
    /** 生成时间戳 / Spawn timestamp */
    val spawnTime: Long,
    
    /** 亡语内容 / Death message content */
    val message: String
)
