package com.github.postyizhan.murderAddon.data

import ink.ptms.adyeshach.core.entity.type.AdyHuman
import org.bukkit.Location
import java.util.*

/**
 * 尸体数据类
 * Corpse Data Class
 * 
 * 存储尸体相关的所有信息
 * Stores all information related to a corpse
 */
data class CorpseData(
    /** 玩家 UUID / Player UUID */
    val playerId: UUID,
    
    /** 玩家名称 / Player name */
    val playerName: String,
    
    /** Adyeshach 实体 / Adyeshach entity */
    val entity: AdyHuman,
    
    /** 尸体位置 / Corpse location */
    val location: Location,
    
    /** 生成时间戳 / Spawn timestamp */
    val spawnTime: Long
)
