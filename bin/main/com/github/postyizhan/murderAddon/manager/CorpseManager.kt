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
import plugily.projects.murdermystery.arena.Arena
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

            // 调整位置偏移 / Adjust location offset
            val adjustedLocation = location.clone().apply {
                x += plugin.configManager.xOffset
                y += plugin.configManager.yOffset
                z += plugin.configManager.zOffset
            }

            // 根据配置设置朝向 / Set rotation based on configuration
            val finalLocation = adjustedLocation.clone().apply {
                when (plugin.configManager.rotationMode.uppercase()) {
                    "FIXED" -> {
                        // 使用固定朝向 / Use fixed rotation
                        yaw = plugin.configManager.fixedYaw
                        pitch = plugin.configManager.fixedPitch
                    }
                    "RANDOM" -> {
                        // 使用随机朝向 / Use random rotation
                        yaw = (Math.random() * 360).toFloat()
                        pitch = ((Math.random() * 180) - 90).toFloat() // -90 到 90 度
                    }
                    "KEEP_DEATH" -> {
                        // 保持死亡时的朝向（默认行为）/ Keep death rotation (default behavior)
                        // 不需要修改朝向，使用原始位置的朝向
                    }
                    else -> {
                        // 默认保持死亡时朝向 / Default to keep death rotation
                        plugin.logger.warning("Unknown rotation mode: ${plugin.configManager.rotationMode}, using KEEP_DEATH")
                    }
                }
            }

            // 获取 Adyeshach 管理器 / Get Adyeshach manager
            val manager = Adyeshach.api().getPublicEntityManager(ManagerType.TEMPORARY)
            
            // 创建玩家 NPC / Create player NPC
            val corpseEntity = manager.create(ink.ptms.adyeshach.core.entity.EntityTypes.PLAYER, finalLocation) { entity ->
                val humanEntity = entity as AdyHuman
                
                // 设置基本属性 / Set basic properties
                humanEntity.isHideFromTabList = plugin.configManager.hideFromTabList

                // 设置名称和可见性 / Set name and visibility
                if (plugin.configManager.showCorpseName) {
                    humanEntity.setName(plugin.configManager.formatCorpseName(deadPlayer.name))
                    humanEntity.setCustomNameVisible(true)
                } else {
                    humanEntity.setName("") // 清空名称 / Clear name
                    humanEntity.setCustomNameVisible(false)
                }

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
            }
            
            // 创建尸体数据 / Create corpse data
            val corpseData = CorpseData(
                playerId = deadPlayer.uniqueId,
                playerName = deadPlayer.name,
                entity = corpseEntity as AdyHuman,
                location = finalLocation,
                spawnTime = System.currentTimeMillis()
            )
            
            // 存储尸体数据 / Store corpse data
            corpses[deadPlayer.uniqueId] = corpseData
            
            // 发送消息 / Send message
            if (plugin.configManager.debugEnabled) {
                val worldName = finalLocation.world?.name ?: "unknown"
                plugin.logger.info(plugin.configManager.getDebugMessage(
                    "Corpse spawned for player {player} at {location} with rotation mode {mode}",
                    "player" to deadPlayer.name,
                    "location" to "$worldName:${finalLocation.blockX},${finalLocation.blockY},${finalLocation.blockZ}",
                    "mode" to plugin.configManager.rotationMode
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
        // 如果设置为-1，表示尸体在游戏结束前一直存在，不进行时间清理
        // If set to -1, corpses persist until game ends, no time-based cleanup
        if (plugin.configManager.corpseDuration == -1) {
            return
        }

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
            // 额外检查游戏状态清理 / Additional game state cleanup check
            checkGameStateCleanup()
        }, 20L * 10, 20L * 10) // 每10秒检查一次 / Check every 10 seconds

        if (plugin.configManager.debugEnabled) {
            plugin.logger.info(plugin.configManager.getDebugMessage("Starting corpse cleanup task"))
        }
    }

    /**
     * 检查游戏状态并清理尸体
     * Check game state and cleanup corpses
     */
    private fun checkGameStateCleanup() {
        if (plugin.configManager.corpseDuration != -1) {
            return // 只在永久模式下检查 / Only check in permanent mode
        }

        try {
            val mmPlugin = plugin.server.pluginManager.getPlugin("MurderMystery") as? plugily.projects.murdermystery.Main
            if (mmPlugin == null) return

            val arenasToCleanup = mutableSetOf<String>()

            // 检查每个尸体所在的竞技场状态 / Check arena state for each corpse
            corpses.values.forEach { corpseData ->
                try {
                    val location = corpseData.location
                    val world = location.world ?: return@forEach

                    // 查找包含此位置的竞技场 / Find arena containing this location
                    mmPlugin.arenaRegistry.arenas.forEach { arena ->
                        if (arena.startLocation?.world == world) {
                            val arenaState = arena.arenaState.name.uppercase()
                            if (arenaState == "WAITING_FOR_PLAYERS" || arenaState == "STARTING" ||
                                arenaState == "ENDING" || arenaState == "RESTARTING") {
                                arenasToCleanup.add(arena.id)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // 忽略单个尸体的检查错误 / Ignore individual corpse check errors
                }
            }

            // 清理标记的竞技场 / Cleanup marked arenas
            arenasToCleanup.forEach { arenaId ->
                cleanupArenaCorpses(arenaId)
            }

        } catch (e: Exception) {
            if (plugin.configManager.debugEnabled) {
                plugin.logger.warning("Error in game state cleanup check: ${e.message}")
            }
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

    /**
     * 清理指定竞技场的所有尸体
     * Clean up all corpses in specified arena
     *
     * @param arenaId 竞技场ID / Arena ID
     */
    fun cleanupArenaCorpses(arenaId: String) {
        val mmPlugin = try {
            plugin.server.pluginManager.getPlugin("MurderMystery") as? plugily.projects.murdermystery.Main
        } catch (e: Exception) {
            plugin.logger.warning("Failed to get MurderMystery plugin instance: ${e.message}")
            return
        }

        if (mmPlugin == null) return

        val arena = mmPlugin.arenaRegistry.getArena(arenaId) ?: return
        val toRemove = mutableListOf<UUID>()

        // 找到属于该竞技场的所有尸体 / Find all corpses belonging to this arena
        corpses.forEach { (playerId, corpseData) ->
            // 检查尸体位置是否在竞技场范围内 / Check if corpse location is within arena bounds
            if (isLocationInArena(corpseData.location, arena)) {
                toRemove.add(playerId)
            }
        }

        // 移除找到的尸体 / Remove found corpses
        toRemove.forEach { playerId ->
            removeCorpse(playerId)
        }

        if (toRemove.isNotEmpty()) {
            plugin.logger.info("Cleaned up ${toRemove.size} corpses from arena $arenaId")
            if (plugin.configManager.debugEnabled) {
                plugin.logger.info(plugin.configManager.getDebugMessage(
                    "Cleaned up {count} corpses from arena {arena}",
                    "count" to toRemove.size.toString(),
                    "arena" to arenaId
                ))
            }
        }
    }

    /**
     * 检查位置是否在竞技场范围内
     * Check if location is within arena bounds
     */
    private fun isLocationInArena(location: Location, arena: Arena): Boolean {
        return try {
            // 检查世界是否匹配 / Check if world matches
            val arenaWorld = arena.startLocation?.world ?: return false
            if (location.world != arenaWorld) return false

            // 简单的范围检查，可以根据需要调整 / Simple range check, can be adjusted as needed
            val arenaLoc = arena.startLocation ?: return false
            val distance = location.distance(arenaLoc)
            distance <= 200.0 // 假设竞技场半径为200格 / Assume arena radius is 200 blocks
        } catch (e: Exception) {
            plugin.logger.warning("Error checking if location is in arena: ${e.message}")
            false
        }
    }
}
