package com.github.postyizhan.murderAddon.command

import com.github.postyizhan.murderAddon.MurderAddon
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

/**
 * MurderAddon 命令处理器
 * MurderAddon Command Handler
 */
class MurderAddonCommand(private val plugin: MurderAddon) : CommandExecutor, TabCompleter {
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        // 检查权限 / Check permission
        if (!sender.hasPermission("murderaddon.admin")) {
            sender.sendMessage(plugin.configManager.getMessage("plugin.no-permission"))
            return true
        }
        
        when {
            args.isEmpty() -> {
                // 显示帮助信息 / Show help information
                showHelp(sender)
            }
            
            args[0].equals("reload", ignoreCase = true) -> {
                // 重新加载配置 / Reload configuration
                if (!sender.hasPermission("murderaddon.reload")) {
                    sender.sendMessage(plugin.configManager.getMessage("plugin.no-permission"))
                    return true
                }
                
                plugin.reloadPlugin()
                sender.sendMessage(plugin.configManager.getMessage("plugin.reload"))
            }
            
            args[0].equals("info", ignoreCase = true) -> {
                // 显示插件信息 / Show plugin information
                showInfo(sender)
            }
            
            else -> {
                showHelp(sender)
            }
        }
        
        return true
    }
    
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (!sender.hasPermission("murderaddon.admin")) {
            return emptyList()
        }
        
        return when (args.size) {
            1 -> {
                val subCommands = mutableListOf("reload", "info")
                subCommands.filter { it.startsWith(args[0], ignoreCase = true) }
            }
            else -> emptyList()
        }
    }
    
    /**
     * 显示帮助信息
     * Show help information
     */
    private fun showHelp(sender: CommandSender) {
        sender.sendMessage("§6=== MurderAddon Help ===")
        sender.sendMessage("§e/murderaddon reload §7- 重新加载配置")
        sender.sendMessage("§e/murderaddon info §7- 显示插件信息")
    }
    
    /**
     * 显示插件信息
     * Show plugin information
     */
    private fun showInfo(sender: CommandSender) {
        sender.sendMessage("§6=== MurderAddon Info ===")
        sender.sendMessage("§7版本: §e${plugin.description.version}")
        sender.sendMessage("§7作者: §e${plugin.description.authors.joinToString(", ")}")
        sender.sendMessage("§7当前尸体数量: §e${plugin.corpseManager.getCorpseCount()}")
        sender.sendMessage("§7语言: §e${plugin.configManager.language}")
        sender.sendMessage("§7调试模式: §e${if (plugin.configManager.debugEnabled) "启用" else "禁用"}")
    }
}
