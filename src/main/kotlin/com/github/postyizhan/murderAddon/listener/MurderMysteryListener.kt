package com.github.postyizhan.murderAddon.listener

import com.github.postyizhan.murderAddon.MurderAddon
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import plugily.projects.murdermystery.Main
import plugily.projects.murdermystery.arena.Arena

/**
 * MurderMystery 事件监听器
 * MurderMystery Event Listener
 * 
 * 监听 MurderMystery 游戏中的玩家死亡事件
 * Listens to player death events in MurderMystery games
 */
class MurderMysteryListener(private val plugin: MurderAddon) : Listener {
    
    private val murderMysteryPlugin: Main? by lazy {
        try {
            plugin.server.pluginManager.getPlugin("MurderMystery") as? Main
        } catch (e: Exception) {
            plugin.logger.warning("Failed to get MurderMystery plugin instance: ${e.message}")
            null
        }
    }
    
    /**
     * 监听玩家死亡事件
     * Listen to player death events
     * 
     * 使用 MONITOR 优先级确保在 MurderMystery 处理完事件后再处理
     * Use MONITOR priority to handle after MurderMystery processes the event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        
        // 检查 MurderMystery 插件是否可用 / Check if MurderMystery plugin is available
        val mmPlugin = murderMysteryPlugin ?: return
        
        try {
            // 检查玩家是否在 MurderMystery 游戏中 / Check if player is in MurderMystery game
            val arena = mmPlugin.arenaRegistry.getArena(player) ?: return
            
            // 确保是在游戏中死亡 / Ensure death occurred during game
            if (!isInGame(arena)) {
                return
            }
            
            // 调试信息 / Debug information
            if (plugin.configManager.debugEnabled) {
                plugin.logger.info(plugin.configManager.getDebugMessage(
                    "Received death event for player {player} in arena {arena}",
                    "player" to player.name,
                    "arena" to arena.id
                ))
            }
            
            // 延迟生成尸体，确保 MurderMystery 完成所有处理
            // Delay corpse spawning to ensure MurderMystery completes all processing
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                // 再次检查玩家状态 / Check player status again
                if (shouldCreateCorpse(player, arena)) {
                    plugin.corpseManager.createCorpse(player, player.location)
                }
            }, 5L) // 延迟 5 tick (0.25秒) / Delay 5 ticks (0.25 seconds)
            
        } catch (e: Exception) {
            plugin.logger.severe("Error handling player death event for ${player.name}: ${e.message}")
            if (plugin.configManager.debugEnabled) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 检查竞技场是否在游戏中
     * Check if arena is in game
     */
    private fun isInGame(arena: Arena): Boolean {
        return try {
            // 检查游戏状态 / Check game state
            val arenaState = arena.arenaState
            arenaState.name == "IN_GAME"
        } catch (e: Exception) {
            plugin.logger.warning("Failed to check arena state: ${e.message}")
            false
        }
    }
    
    /**
     * 判断是否应该为玩家创建尸体
     * Determine if a corpse should be created for the player
     */
    private fun shouldCreateCorpse(player: org.bukkit.entity.Player, arena: Arena): Boolean {
        try {
            // 检查玩家是否仍在竞技场中 / Check if player is still in arena
            if (!arena.players.contains(player)) {
                return false
            }
            
            // 检查玩家是否已经有尸体 / Check if player already has a corpse
            if (plugin.corpseManager.hasCorpse(player.uniqueId)) {
                return false
            }
            

            // 获取用户信息 / Get user information
            val mmPlugin = murderMysteryPlugin ?: return false
            val user = mmPlugin.userManager.getUser(player)
            
            // 检查玩家是否是观察者（已死亡） / Check if player is spectator (dead)
            return user.isSpectator
            
        } catch (e: Exception) {
            plugin.logger.warning("Error checking if corpse should be created for ${player.name}: ${e.message}")
            return false
        }
    }
}
