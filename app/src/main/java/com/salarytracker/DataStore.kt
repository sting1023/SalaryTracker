package com.salarytracker

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

/** 数据存储：SharedPreferences + JSON */
class DataStore(private val ctx: Context) {

    companion object {
        private const val PREFS = "salary_prefs"
        private const val KEY_SETTINGS = "settings"
        private const val KEY_RECORDS = "records"
        private const val KEY_HOLIDAYS = "custom_holidays"
    }

    private val prefs: SharedPreferences by lazy {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    // ===================== 设置 =====================

    fun loadSettings(): SalarySettings {
        val json = prefs.getString(KEY_SETTINGS, null) ?: return SalarySettings()
        return try {
            val obj = JSONObject(json)
            val holidays = prefs.getStringSet(KEY_HOLIDAYS, emptySet()) ?: emptySet()
            SalarySettings(
                normalHourlyRate = obj.optDouble("normalHourlyRate", 20.0),
                overtimeHourlyRate = obj.optDouble("overtimeHourlyRate", 30.0),
                holidayHourlyRate = obj.optDouble("holidayHourlyRate", 50.0),
                customHolidays = holidays
            )
        } catch (e: Exception) {
            SalarySettings()
        }
    }

    fun saveSettings(settings: SalarySettings) {
        val obj = JSONObject().apply {
            put("normalHourlyRate", settings.normalHourlyRate)
            put("overtimeHourlyRate", settings.overtimeHourlyRate)
            put("holidayHourlyRate", settings.holidayHourlyRate)
        }
        prefs.edit()
            .putString(KEY_SETTINGS, obj.toString())
            .putStringSet(KEY_HOLIDAYS, settings.customHolidays)
            .apply()
    }

    // ===================== 记录 =====================

    private fun getRecords(): MutableMap<String, SalaryRecord> {
        val json = prefs.getString(KEY_RECORDS, null) ?: return mutableMapOf()
        val map = mutableMapOf<String, SalaryRecord>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val record = SalaryRecord(
                    id = obj.optLong("id", 0),
                    date = obj.getString("date"),
                    hourlyRate = obj.optDouble("hourlyRate"),
                    hours = obj.optDouble("hours"),
                    dailyRate = obj.optDouble("dailyRate"),
                    isHoliday = obj.optBoolean("isHoliday"),
                    isWeekend = obj.optBoolean("isWeekend"),
                    total = obj.optDouble("total"),
                    note = obj.optString("note", "")
                )
                map[record.date] = record
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

    private fun saveRecords(records: Map<String, SalaryRecord>) {
        val arr = JSONArray()
        records.values.forEach { r ->
            arr.put(JSONObject().apply {
                put("id", r.id)
                put("date", r.date)
                put("hourlyRate", r.hourlyRate)
                put("hours", r.hours)
                put("dailyRate", r.dailyRate)
                put("isHoliday", r.isHoliday)
                put("isWeekend", r.isWeekend)
                put("total", r.total)
                put("note", r.note)
            })
        }
        prefs.edit().putString(KEY_RECORDS, arr.toString()).apply()
    }

    fun saveRecord(record: SalaryRecord) {
        val records = getRecords()
        records[record.date] = record
        saveRecords(records)
    }

    fun deleteRecord(date: String) {
        val records = getRecords()
        records.remove(date)
        saveRecords(records)
    }

    fun getRecord(date: String): SalaryRecord? = getRecords()[date]

    fun getRecordsForMonth(year: Int, month: Int): List<SalaryRecord> {
        val prefix = String.format(Locale.CHINA, "%04d-%02d", year, month)
        return getRecords().values
            .filter { it.date.startsWith(prefix) }
            .sortedBy { it.date }
    }

    fun getMonthStats(year: Int, month: Int): MonthStats {
        val records = getRecordsForMonth(year, month)
        val totalHours = records.sumOf { it.hours }
        val totalSalary = records.sumOf { it.total }
        val workdayCount = records.count { !it.isWeekend && !it.isHoliday }
        val holidayCount = records.count { it.isHoliday }

        return MonthStats(
            year = year,
            month = month,
            totalHours = totalHours,
            totalSalary = totalSalary,
            workdayCount = workdayCount,
            holidayCount = holidayCount,
            records = records
        )
    }

    fun isHoliday(date: String): Boolean {
        val settings = loadSettings()
        if (settings.customHolidays.contains(date)) return true

        val cal = Calendar.getInstance()
        val parts = date.split("-")
        cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        // 周六周日
        return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
    }
}
