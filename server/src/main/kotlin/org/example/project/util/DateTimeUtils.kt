package org.example.project.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 日期时间工具类
 */
object DateTimeUtils {
    /**
     * 获取当前本地时间
     * 用于数据库存储和业务逻辑计算
     */
    fun nowUTC(): LocalDateTime = LocalDateTime.now()
    
    /**
     * 将时间转换为 ISO 8601 格式字符串
     * 用于 API 响应
     */
    fun toISOString(dateTime: LocalDateTime): String {
        return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
}

