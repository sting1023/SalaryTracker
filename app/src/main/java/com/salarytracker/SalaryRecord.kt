package com.salarytracker

import java.util.*

/** 某一天的工资记录 */
data class SalaryRecord(
    val id: Long = 0,
    val date: String,          // "yyyy-MM-dd"
    val hourlyRate: Double,      // 当天时薪（可能与预设不同）
    val hours: Double,           // 当天工作小时数
    val dailyRate: Double,       // 当天日薪（加班固定金额，不填=0）
    val isHoliday: Boolean,     // 是否节假日（手动标记）
    val isWeekend: Boolean,     // 是否周末（自动计算）
    val total: Double,          // 合计 = 时薪×小时 + 日薪
    val note: String = ""
) {
    companion object {
        fun calculate(hourlyRate: Double, hours: Double, dailyRate: Double): Double {
            return hourlyRate * hours + dailyRate
        }

        fun fromForm(date: String, hourlyRate: Double, hours: Double, dailyRate: Double,
                      isHoliday: Boolean, isWeekend: Boolean, note: String = ""): SalaryRecord {
            val total = calculate(hourlyRate, hours, dailyRate)
            return SalaryRecord(
                date = date,
                hourlyRate = hourlyRate,
                hours = hours,
                dailyRate = dailyRate,
                isHoliday = isHoliday,
                isWeekend = isWeekend,
                total = total,
                note = note
            )
        }
    }
}

/** 应用设置 */
data class SalarySettings(
    val normalHourlyRate: Double = 20.0,
    val overtimeHourlyRate: Double = 30.0,
    val holidayHourlyRate: Double = 50.0,
    val customHolidays: Set<String> = emptySet()  // "yyyy-MM-dd" 格式的自定义节假日
)

/** 月份统计数据 */
data class MonthStats(
    val year: Int,
    val month: Int,
    val totalHours: Double,
    val totalSalary: Double,
    val workdayCount: Int,
    val holidayCount: Int,
    val records: List<SalaryRecord>
)
