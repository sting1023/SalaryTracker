package com.salarytracker

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var ds: DataStore
    private lateinit var settings: SalarySettings

    // Views
    private lateinit var tvYearMonth: TextView
    private lateinit var tvMonthTotal: TextView
    private lateinit var tvMonthHours: TextView
    private lateinit var calendarTable: TableLayout
    private lateinit var tvSelectedDate: TextView
    private lateinit var tvDayTotal: TextView
    private lateinit var tvDayStatus: TextView
    private lateinit var etHourlyRate: EditText
    private lateinit var etHours: EditText
    private lateinit var etDailyRate: EditText
    private lateinit var llDailyRate: LinearLayout
    private lateinit var llHourlyRate: LinearLayout
    private lateinit var cbHoliday: CheckBox
    private lateinit var etNote: EditText
    private lateinit var btnSave: Button
    private lateinit var btnDelete: Button
    private lateinit var btnPrevMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton
    private lateinit var btnModeHourly: Button
    private lateinit var btnModeDaily: Button
    private lateinit var lvRecords: ListView

    // State
    private var currentYear = 0
    private var currentMonth = 0
    private var selectedDate = ""
    private var isHoliday = false
    private var isWeekend = false
    private var useDailyRateMode = false  // true=日薪模式, false=时薪模式

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    private val monthFormat = SimpleDateFormat("yyyy年MM月", Locale.CHINA)
    private val dayDisplayFormat = SimpleDateFormat("M月d日 E", Locale.CHINA)

    // 日历单元格高度（像素）
    private val dayCellHeight = 48

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ds = DataStore(this)

        val now = Calendar.getInstance()
        currentYear = now.get(Calendar.YEAR)
        currentMonth = now.get(Calendar.MONTH) + 1
        selectedDate = dateFormat.format(now.time)

        initViews()
        updateAll()
    }

    private fun initViews() {
        tvYearMonth    = findViewById(R.id.tv_year_month)
        tvMonthTotal   = findViewById(R.id.tv_month_total)
        tvMonthHours   = findViewById(R.id.tv_month_hours)
        calendarTable  = findViewById(R.id.calendar_table)
        tvSelectedDate = findViewById(R.id.tv_selected_date)
        tvDayTotal     = findViewById(R.id.tv_day_total)
        tvDayStatus   = findViewById(R.id.tv_day_status)
        etHourlyRate  = findViewById(R.id.et_hourly_rate)
        etHours       = findViewById(R.id.et_hours)
        etDailyRate   = findViewById(R.id.et_daily_rate)
        llHourlyRate  = findViewById(R.id.ll_hourly_rate)
        llDailyRate   = findViewById(R.id.ll_daily_rate)
        cbHoliday     = findViewById(R.id.cb_holiday)
        etNote        = findViewById(R.id.et_note)
        btnSave       = findViewById(R.id.btn_save)
        btnDelete     = findViewById(R.id.btn_delete)
        btnPrevMonth  = findViewById(R.id.btn_prev_month)
        btnNextMonth  = findViewById(R.id.btn_next_month)
        btnModeHourly = findViewById(R.id.btn_mode_hourly)
        btnModeDaily  = findViewById(R.id.btn_mode_daily)
        lvRecords     = findViewById(R.id.lv_records)

        btnModeHourly.setOnClickListener {
            useDailyRateMode = false
            updateAll()
        }
        btnModeDaily.setOnClickListener {
            useDailyRateMode = true
            updateAll()
        }

        btnPrevMonth.setOnClickListener {
            if (currentMonth == 1) { currentMonth = 12; currentYear-- }
            else currentMonth--
            updateAll()
        }
        btnNextMonth.setOnClickListener {
            if (currentMonth == 12) { currentMonth = 1; currentYear++ }
            else currentMonth++
            updateAll()
        }

        btnSave.setOnClickListener   { saveRecord() }
        btnDelete.setOnClickListener { deleteRecord() }

        lvRecords.setOnItemClickListener { _, _, pos, _ ->
            val records = ds.getRecordsForMonth(currentYear, currentMonth)
            if (pos < records.size) {
                selectedDate = records[pos].date
                updateAll()
            }
        }

        findViewById<Button>(R.id.btn_settings).setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun updateAll() {
        settings = ds.loadSettings()

        // 顶部月份
        val cal = Calendar.getInstance()
        cal.set(currentYear, currentMonth - 1, 1)
        tvYearMonth.text = monthFormat.format(cal.time)

        // 月合计
        val monthStats = ds.getMonthStats(currentYear, currentMonth)
        tvMonthTotal.text = "¥${String.format("%.2f", monthStats.totalSalary)}"
        tvMonthHours.text = "${String.format("%.1f", monthStats.totalHours)} 小时"

        // 选中日期信息
        val selCal = Calendar.getInstance()
        val parts = selectedDate.split("-")
        selCal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        tvSelectedDate.text = dayDisplayFormat.format(selCal.time)

        // 当天属性
        isHoliday = ds.isHoliday(selectedDate)
        val dow = selCal.get(Calendar.DAY_OF_WEEK)
        isWeekend = dow == Calendar.SATURDAY || dow == Calendar.SUNDAY

        // 当天记录填充表单
        val record = ds.getRecord(selectedDate)
        if (record != null) {
            tvDayTotal.text = "¥${String.format("%.2f", record.total)}"
            val typeStr = when {
                record.isHoliday -> "节假日"
                record.isWeekend -> "周末"
                else -> "工作日"
            }
            tvDayStatus.text = typeStr
            // 优先使用已保存的模式
            useDailyRateMode = record.dailyRate > 0
            etHourlyRate.setText(String.format("%.1f", record.hourlyRate))
            etHours.setText(String.format("%.1f", record.hours))
            etDailyRate.setText(String.format("%.1f", record.dailyRate))
            cbHoliday.isChecked = record.isHoliday
            etNote.setText(record.note)
            btnDelete.visibility = View.VISIBLE
        } else {
            tvDayTotal.text = "¥0.00"
            tvDayStatus.text = when {
                isHoliday -> "节假日"
                isWeekend -> "周末"
                else -> "工作日"
            }
            val autoRate = if (isHoliday) settings.holidayHourlyRate else settings.normalHourlyRate
            etHourlyRate.setText(String.format("%.1f", autoRate))
            etHours.setText("")
            etDailyRate.setText("0")
            cbHoliday.isChecked = isHoliday
            etNote.setText("")
            btnDelete.visibility = View.GONE
        }

        // 模式切换UI
        applyModeUI()

        // 渲染日历
        buildCalendarTable()

        // 月记录列表
        val monthRecords = ds.getRecordsForMonth(currentYear, currentMonth)
        lvRecords.adapter = RecordAdapter(monthRecords)
    }

    private fun applyModeUI() {
        if (useDailyRateMode) {
            llHourlyRate.visibility = View.GONE
            llDailyRate.visibility = View.VISIBLE
            btnModeHourly.setBackgroundColor(Color.parseColor("#E0E0E0"))
            btnModeHourly.setTextColor(Color.parseColor("#888888"))
            btnModeDaily.setBackgroundColor(Color.parseColor("#1565C0"))
            btnModeDaily.setTextColor(Color.WHITE)
        } else {
            llHourlyRate.visibility = View.VISIBLE
            llDailyRate.visibility = View.GONE
            btnModeHourly.setBackgroundColor(Color.parseColor("#1565C0"))
            btnModeHourly.setTextColor(Color.WHITE)
            btnModeDaily.setBackgroundColor(Color.parseColor("#E0E0E0"))
            btnModeDaily.setTextColor(Color.parseColor("#888888"))
        }
    }

    private fun buildCalendarTable() {
        calendarTable.removeAllViews()

        val cal = Calendar.getInstance()
        cal.set(currentYear, currentMonth - 1, 1)
        val firstDow = cal.get(Calendar.DAY_OF_WEEK) // 1=周日
        // 周一列开始：周一的下标是0
        val startOffset = if (firstDow == Calendar.SUNDAY) 6 else firstDow - 2

        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val today = dateFormat.format(Date())
        val todayCal = Calendar.getInstance()
        val todayYear = todayCal.get(Calendar.YEAR)
        val todayMonth = todayCal.get(Calendar.MONTH) + 1

        val monthRecords = ds.getRecordsForMonth(currentYear, currentMonth)
        val recordMap = monthRecords.associateBy { it.date }

        // 6行×7列
        for (row in 0 until 6) {
            val tableRow = TableRow(this)
            tableRow.layoutParams = TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                dayCellHeight
            )

            for (col in 0 until 7) {
                val cellIndex = row * 7 + col
                val day = cellIndex - startOffset + 1

                if (day < 1 || day > daysInMonth) {
                    // 空白格子
                    val spacer = Space(this)
                    val lp = TableRow.LayoutParams(0, dayCellHeight, 1f)
                    spacer.layoutParams = lp
                    tableRow.addView(spacer)
                } else {
                    val dayStr = String.format("%04d-%02d-%02d", currentYear, currentMonth, day)
                    val isSelected = dayStr == selectedDate
                    val isToday = currentYear == todayYear && currentMonth == todayMonth &&
                                  day == todayCal.get(Calendar.DAY_OF_MONTH)
                    val hasRecord = recordMap.containsKey(dayStr)

                    cal.set(currentYear, currentMonth - 1, day)
                    val dow = cal.get(Calendar.DAY_OF_WEEK)
                    val isSat = dow == Calendar.SATURDAY
                    val isSun = dow == Calendar.SUNDAY
                    val isHol = ds.isHoliday(dayStr)

                    // 文字：日期 + 可选圆点
                    val label = if (hasRecord) "$day\n●" else day.toString()

                    val tv = TextView(this)
                    tv.text = label
                    tv.gravity = Gravity.CENTER
                    tv.textSize = 13f
                    tv.setLineSpacing(0f, 1.1f)

                    val lp = TableRow.LayoutParams(0, dayCellHeight, 1f)
                    tv.layoutParams = lp

                    // 样式
                    when {
                        isSelected -> {
                            tv.setBackgroundResource(R.drawable.day_selected_bg)
                            tv.setTextColor(Color.WHITE)
                        }
                        isToday -> {
                            tv.setBackgroundResource(R.drawable.day_today_bg)
                            tv.setTextColor(Color.parseColor("#1565C0"))
                        }
                        else -> {
                            tv.setBackgroundResource(R.drawable.day_normal)
                            tv.setTextColor(when {
                                isHol -> Color.parseColor("#F44336")
                                isSat || isSun -> Color.parseColor("#FF9800")
                                else -> Color.parseColor("#333333")
                            })
                        }
                    }

                    tv.setOnClickListener {
                        selectedDate = dayStr
                        updateAll()
                    }

                    tableRow.addView(tv)
                }
            }

            calendarTable.addView(tableRow)
        }
    }

    private fun saveRecord() {
        if (!useDailyRateMode) {
            // 时薪模式
            val hourlyRate = etHourlyRate.text.toString().toDoubleOrNull()
            val hours = etHours.text.toString().toDoubleOrNull() ?: 0.0
            if (hourlyRate == null) {
                Toast.makeText(this, "请填写时薪", Toast.LENGTH_SHORT).show()
                return
            }
            if (hourlyRate < 0 || hours < 0) {
                Toast.makeText(this, "数值不能为负", Toast.LENGTH_SHORT).show()
                return
            }
            val record = SalaryRecord.fromForm(
                date = selectedDate,
                hourlyRate = hourlyRate,
                hours = hours,
                dailyRate = 0.0,
                isHoliday = cbHoliday.isChecked,
                isWeekend = isWeekend,
                note = etNote.text.toString()
            )
            ds.saveRecord(record)
        } else {
            // 日薪模式
            val dailyRate = etDailyRate.text.toString().toDoubleOrNull() ?: 0.0
            if (dailyRate <= 0) {
                Toast.makeText(this, "请填写日薪", Toast.LENGTH_SHORT).show()
                return
            }
            val record = SalaryRecord.fromForm(
                date = selectedDate,
                hourlyRate = 0.0,
                hours = 0.0,
                dailyRate = dailyRate,
                isHoliday = cbHoliday.isChecked,
                isWeekend = isWeekend,
                note = etNote.text.toString()
            )
            ds.saveRecord(record)
        }
        Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
        updateAll()
    }

    private fun deleteRecord() {
        AlertDialog.Builder(this)
            .setTitle("删除记录")
            .setMessage("确定删除 $selectedDate 的记录吗？")
            .setPositiveButton("删除") { _, _ ->
                ds.deleteRecord(selectedDate)
                updateAll()
                Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showSettingsDialog() {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_settings)
        dialog.show()

        val etNormal  = dialog.findViewById<EditText>(R.id.et_normal_rate)
        val etOvertime= dialog.findViewById<EditText>(R.id.et_overtime_rate)
        val etHoliday = dialog.findViewById<EditText>(R.id.et_holiday_rate)
        val etHolidays= dialog.findViewById<EditText>(R.id.et_holidays)
        val btnSave   = dialog.findViewById<Button>(R.id.btn_save_settings)
        val btnCancel = dialog.findViewById<Button>(R.id.btn_cancel_settings)

        val s = ds.loadSettings()
        etNormal.setText(String.format("%.1f", s.normalHourlyRate))
        etOvertime.setText(String.format("%.1f", s.overtimeDailyRate))
        etHoliday.setText(String.format("%.1f", s.holidayHourlyRate))
        etHolidays.setText(s.customHolidays.joinToString("\n"))

        btnSave.setOnClickListener {
            val normalRate = etNormal.text.toString().toDoubleOrNull()
            val overtimeDaily = etOvertime.text.toString().toDoubleOrNull() ?: 0.0
            val holidayRate = etHoliday.text.toString().toDoubleOrNull()
            val holidaysText = etHolidays.text.toString()

            if (normalRate == null || holidayRate == null) {
                Toast.makeText(this, "请填写有效的数值", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val holidays = holidaysText.split("\n")
                .map { it.trim() }
                .filter { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
                .toSet()

            ds.saveSettings(SalarySettings(normalRate, overtimeDaily, holidayRate, holidays))
            Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show()
            updateAll()
            dialog.dismiss()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
    }

    // ===================== 列表适配器 =====================

    inner class RecordAdapter(private val records: List<SalaryRecord>) : BaseAdapter() {
        override fun getCount() = records.size
        override fun getItem(pos: Int) = records[pos]
        override fun getItemId(pos: Int) = pos.toLong()

        override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.item_record, parent, false)
            val r = records[pos]
            val dayNum = r.date.substring(8, 10).toInt().toString() + "日"
            val isToday = r.date == dateFormat.format(Date())

            (view.findViewById<TextView>(R.id.tv_day)).text = dayNum
            (view.findViewById<TextView>(R.id.tv_hours)).text = "${r.hours}h"
            (view.findViewById<TextView>(R.id.tv_rate)).text = "${r.hourlyRate}/h"
            (view.findViewById<TextView>(R.id.tv_total)).text = "¥${String.format("%.2f", r.total)}"

            val typeFlag = view.findViewById<TextView>(R.id.tv_flag)
            val (flagText, flagColor) = when {
                r.isHoliday -> "节假日" to ContextCompat.getColor(this@MainActivity, R.color.holiday_color)
                r.isWeekend -> "周末" to ContextCompat.getColor(this@MainActivity, R.color.weekend_color)
                else -> "工作" to ContextCompat.getColor(this@MainActivity, R.color.workday_color)
            }
            typeFlag.text = flagText
            typeFlag.setBackgroundColor(flagColor)

            if (isToday) {
                view.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.today_bg))
            }

            return view
        }
    }

    override fun onResume() {
        super.onResume()
        updateAll()
    }
}
