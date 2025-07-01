package com.github.postyizhan.murderAddon.manager

import com.github.postyizhan.murderAddon.MurderAddon
import com.github.postyizhan.murderAddon.data.CorpseData
import ink.ptms.adyeshach.core.Adyeshach
import ink.ptms.adyeshach.core.bukkit.BukkitPose
import ink.ptms.adyeshach.core.entity.manager.ManagerType
import ink.ptms.adyeshach.core.entity.type.AdyHuman
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 尸体管理器
 * Corpse Manager
 * 
 * 负责管理尸体 NPC 的创建、销毁和生命周期
 * Manages the creation, destruction and lifecycle of corpse NPCs
 */
class CorpseManager(private val plugin: MurderAddon) {
    
    // 存储所有尸体数据 / Store all corpse data
    private val corpses = ConcurrentHashMap<UUID, CorpseData>()
    
    // 清理任务 / Cleanup task
    private var cleanupTask: BukkitTask? = null
    
    /**
     * 初始化尸体管理器
     * Initialize corpse manager
     */
    fun initialize() {
        // 启动定期清理任务 / Start periodic cleanup task
        startCleanupTask()
        
        if (plugin.configManager.debugEnabled) {
            plugin.logger.info(plugin.configManager.getDebugMessage("CorpseManager initialized successfully"))
        }
    }
    
    /**
     * 关闭尸体管理器
     * Shutdown corpse manager
     */
    fun shutdown() {
        // 取消清理任务 / Cancel cleanup task
        cleanupTask?.cancel()
        
        // 清理所有尸体 / Clean up all corpses
        cleanupAllCorpses()
        
        plugin.logger.info("CorpseManager shutdown completed")
    }
    
    /**
     * 为死亡玩家创建尸体
     * Create corpse for dead player
     * 
     * @param deadPlayer 死亡的玩家 / Dead player
     * @param location 尸体生成位置 / Corpse spawn location
     */
    fun createCorpse(deadPlayer: Player, location: Location) {
        try {
            // 检查世界是否有效 / Check if world is valid
            if (location.world == null) {
                plugin.logger.warning("Cannot create corpse for ${deadPlayer.name}: location world is null")
                return
            }

            // 调整位置高度 / Adjust location height
            val adjustedLocation = location.clone().apply {
                y += plugin.configManager.heightOffset
            }

            // 获取 Adyeshach 管理器 / Get Adyeshach manager
            val manager = Adyeshach.api().getPublicEntityManager(ManagerType.TEMPORARY)
            
            // 创建玩家 NPC / Create player NPC
            val corpseEntity = manager.create(ink.ptms.adyeshach.core.entity.EntityTypes.PLAYER, adjustedLocation) { entity ->
                val humanEntity = entity as AdyHuman
                
                // 设置基本属性 / Set basic properties
                humanEntity.setName(plugin.configManager.formatCorpseName(deadPlayer.name))
                humanEntity.isHideFromTabList = plugin.configManager.hideFromTabList
                
                // 设置皮肤 / Set skin using the method from the image
                val serialize = deadPlayer.playerProfile.serialize()
                val properties = serialize["properties"]
                var value: String? = null
                var signature: String? = null
                
                if (properties is List<*>) {
                    properties.forEach { property ->
                        property as Map<*, *>
                        value = property["value"] as? String
                        signature = property["signature"] as? String
                    }
                }
                
                // 应用皮肤纹理 / Apply skin texture
                if (value != null && signature != null) {
                    humanEntity.setTexture(value!!, signature!!)
                } else {
                    // 如果无法获取皮肤，使用玩家名称 / Use player name if skin unavailable
                    humanEntity.setTexture(deadPlayer.name)
                }
                
                // 设置躺下姿势 / Set sleeping pose
                humanEntity.setSleeping(true)
                
                // 设置名称可见性 / Set name visibility
                if (plugin.configManager.showCorpseName) {
                    humanEntity.setCustomNameVisible(true)
                } else {
                    humanEntity.setCustomNameVisible(false)
                }
            }
            
            // 创建尸体数据 / Create corpse data
            val corpseData = CorpseData(
                playerId = deadPlayer.uniqueId,
                playerName = deadPlayer.name,
                entity = corpseEntity as AdyHuman,
                location = adjustedLocation,
                spawnTime = System.currentTimeMillis()
            )
            
            // 存储尸体数据 / Store corpse data
            corpses[deadPlayer.uniqueId] = corpseData
            
            // 发送消息 / Send message
            if (plugin.configManager.debugEnabled) {
                val worldName = adjustedLocation.world?.name ?: "unknown"
                plugin.logger.info(plugin.configManager.getDebugMessage(
                    "Corpse spawned for player {player} at {location}",
                    "player" to deadPlayer.name,
                    "location" to "$worldName:${adjustedLocation.blockX},${adjustedLocation.blockY},${adjustedLocation.blockZ}"
                ))
            }
            
        } catch (e: Exception) {
            plugin.logger.severe("Failed to create corpse for player ${deadPlayer.name}: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * 移除指定玩家的尸体
     * Remove corpse for specified player
     * 
     * @param playerId 玩家 UUID / Player UUID
     */
    fun removeCorpse(playerId: UUID) {
        val corpseData = corpses.remove(playerId) ?: return
        
        try {
            // 销毁实体 / Destroy entity
            corpseData.entity.remove()
            
            if (plugin.configManager.debugEnabled) {
                plugin.logger.info(plugin.configManager.getDebugMessage(
                    "Corpse removed for player {player}",
                    "player" to corpseData.playerName
                ))
            }
            
        } catch (e: Exception) {
            plugin.logger.severe("Failed to remove corpse for player ${corpseData.playerName}: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * 清理过期的尸体
     * Clean up expired corpses
     */
    private fun cleanupExpiredCorpses() {
        val currentTime = System.currentTimeMillis()
        val expireDuration = plugin.configManager.corpseDuration * 1000L
        val toRemove = mutableListOf<UUID>()
        
        corpses.forEach { (playerId, corpseData) ->
            if (currentTime - corpseData.spawnTime > expireDuration) {
                toRemove.add(playerId)
            }
        }
        
        toRemove.forEach { playerId ->
            removeCorpse(playerId)
        }
        
        if (toRemove.isNotEmpty() && plugin.configManager.debugEnabled) {
            plugin.logger.info(plugin.configManager.getDebugMessage(
                "Cleaned up {count} expired corpses",
                "count" to toRemove.size.toString()
            ))
        }
    }
    
    /**
     * 清理所有尸体
     * Clean up all corpses
     */
    private fun cleanupAllCorpses() {
        val count = corpses.size
        corpses.keys.toList().forEach { playerId ->
            removeCorpse(playerId)
        }
        
        if (count > 0) {
            plugin.logger.info("Cleaned up $count corpses during shutdown")
        }
    }
    
    /**
     * 启动清理任务
     * Start cleanup task
     */
    private fun startCleanupTask() {
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            cleanupExpiredCorpses()
        }, 20L * 10, 20L * 5) // 每5秒检查一次 / Check every 5 seconds
        
        if (plugin.configManager.debugEnabled) {
            plugin.logger.info(plugin.configManager.getDebugMessage("Starting corpse cleanup task"))
        }
    }
    
    /**
     * 获取当前尸体数量
     * Get current corpse count
     */
    fun getCorpseCount(): Int = corpses.size
    
    /**
     * 检查玩家是否有尸体
     * Check if player has a corpse
     */
    fun hasCorpse(playerId: UUID): Boolean = corpses.containsKey(playerId)
}
