package com.github.postyizhan.murderAddon.listener

import com.github.postyizhan.murderAddon.MurderAddon
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerQuitEvent
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
            
            // 延迟生成尸体和亡语，确保 MurderMystery 完成所有处理
            // Delay corpse and death message spawning to ensure MurderMystery completes all processing
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                try {
                    // 再次检查玩家状态 / Check player status again
                    if (shouldCreateCorpse(player, arena)) {
                        if (plugin.configManager.debugEnabled) {
                            plugin.logger.info(plugin.configManager.getDebugMessage(
                                "Creating corpse for player {player} at location {location}",
                                "player" to player.name,
                                "location" to "${player.location.world?.name}:${player.location.blockX},${player.location.blockY},${player.location.blockZ}"
                            ))
                        }

                        plugin.corpseManager.createCorpse(player, player.location)

                        // 创建自定义亡语浮空字 / Create custom death message floating text
                        if (plugin.configManager.deathMessageEnabled) {
                            val deathMessage = formatDeathMessage(player, event)
                            plugin.deathMessageManager.createDeathMessage(player, player.location, deathMessage)
                        }
                    } else if (plugin.configManager.debugEnabled) {
                        plugin.logger.info(plugin.configManager.getDebugMessage(
                            "Skipping corpse creation for player {player} - conditions not met",
                            "player" to player.name
                        ))
                    }
                } catch (e: Exception) {
                    plugin.logger.severe("Error creating corpse for player ${player.name}: ${e.message}")
                    if (plugin.configManager.debugEnabled) {
                        e.printStackTrace()
                    }
                }
            }, plugin.configManager.corpseCreationDelay) // 可配置的延迟 / Configurable delay
            
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
                if (plugin.configManager.debugEnabled) {
                    plugin.logger.info(plugin.configManager.getDebugMessage(
                        "Player {player} not in arena players list",
                        "player" to player.name
                    ))
                }
                return false
            }

            // 检查玩家是否已经有尸体 / Check if player already has a corpse
            if (plugin.corpseManager.hasCorpse(player.uniqueId)) {
                if (plugin.configManager.debugEnabled) {
                    plugin.logger.info(plugin.configManager.getDebugMessage(
                        "Player {player} already has a corpse",
                        "player" to player.name
                    ))
                }
                return false
            }

            // 检查玩家是否在线 / Check if player is online
            if (!player.isOnline) {
                if (plugin.configManager.debugEnabled) {
                    plugin.logger.info(plugin.configManager.getDebugMessage(
                        "Player {player} is not online",
                        "player" to player.name
                    ))
                }
                return false
            }

            // 获取用户信息 / Get user information
            val mmPlugin = murderMysteryPlugin ?: return false
            val user = mmPlugin.userManager.getUser(player)

            // 检查玩家是否是观察者（已死亡） / Check if player is spectator (dead)
            val isSpectator = user.isSpectator

            if (plugin.configManager.debugEnabled) {
                plugin.logger.info(plugin.configManager.getDebugMessage(
                    "Player {player} spectator status: {status}",
                    "player" to player.name,
                    "status" to isSpectator.toString()
                ))
            }

            return isSpectator

        } catch (e: Exception) {
            plugin.logger.warning("Error checking if corpse should be created for ${player.name}: ${e.message}")
            if (plugin.configManager.debugEnabled) {
                e.printStackTrace()
            }
            return false
        }
    }

    /**
     * 监听玩家退出事件
     * Listen to player quit events
     *
     * 当玩家退出时，检查是否需要清理竞技场尸体
     * When player quits, check if arena corpses need cleanup
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player

        // 检查 MurderMystery 插件是否可用 / Check if MurderMystery plugin is available
        val mmPlugin = murderMysteryPlugin ?: return

        try {
            // 检查玩家是否在 MurderMystery 游戏中 / Check if player is in MurderMystery game
            val arena = mmPlugin.arenaRegistry.getArena(player) ?: return

            // 延迟检查游戏状态，如果游戏结束则清理尸体
            // Delay checking game state, cleanup corpses if game ended
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                checkAndCleanupArenaCorpses(arena)
            }, plugin.configManager.gameStateCheckDelay) // 可配置的延迟 / Configurable delay

            // 额外的延迟检查，确保游戏状态更新
            // Additional delayed check to ensure game state is updated
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                checkAndCleanupArenaCorpses(arena)
            }, plugin.configManager.gameStateCheckDelay * 2) // 双倍延迟确保状态更新 / Double delay to ensure state update

        } catch (e: Exception) {
            if (plugin.configManager.debugEnabled) {
                plugin.logger.warning("Error handling player quit event for ${player.name}: ${e.message}")
            }
        }
    }

    /**
     * 检查并清理竞技场尸体
     * Check and cleanup arena corpses
     */
    private fun checkAndCleanupArenaCorpses(arena: Arena) {
        try {
            // 检查游戏是否结束 / Check if game has ended
            val arenaState = arena.arenaState
            val shouldCleanup = when (arenaState.name.uppercase()) {
                "ENDING", "RESTARTING", "WAITING_FOR_PLAYERS", "STARTING" -> true
                "IN_GAME" -> {
                    // 检查游戏中的玩家数量 / Check player count in game
                    val alivePlayers = arena.players.filter { player ->
                        try {
                            val mmPlugin = murderMysteryPlugin ?: return@filter false
                            val user = mmPlugin.userManager.getUser(player)
                            !user.isSpectator // 只计算非观察者玩家 / Only count non-spectator players
                        } catch (e: Exception) {
                            false
                        }
                    }
                    alivePlayers.size <= 1 // 如果存活玩家<=1，游戏应该结束 / Game should end if alive players <= 1
                }
                else -> false
            }

            if (shouldCleanup) {
                // 游戏已结束，清理该竞技场的所有尸体 / Game ended, cleanup all corpses in this arena
                plugin.corpseManager.cleanupArenaCorpses(arena.id)

                if (plugin.configManager.debugEnabled) {
                    plugin.logger.info(plugin.configManager.getDebugMessage(
                        "Game ended in arena {arena} (state: {state}), cleaned up corpses",
                        "arena" to arena.id,
                        "state" to arenaState.name
                    ))
                }
            } else if (plugin.configManager.debugEnabled) {
                plugin.logger.info(plugin.configManager.getDebugMessage(
                    "Arena {arena} still in game (state: {state}), keeping corpses",
                    "arena" to arena.id,
                    "state" to arenaState.name
                ))
            }
        } catch (e: Exception) {
            plugin.logger.warning("Error checking arena state for cleanup: ${e.message}")
            if (plugin.configManager.debugEnabled) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 格式化亡语消息
     * Format death message
     */
    private fun formatDeathMessage(player: org.bukkit.entity.Player, event: PlayerDeathEvent): String {
        // 获取原始亡语消息 / Get original death message
        val originalMessage = event.deathMessage ?: "${player.name} died"

        // 可以在这里添加更多的自定义格式化逻辑 / Can add more custom formatting logic here
        return "§c☠ §7$originalMessage"
    }
}
