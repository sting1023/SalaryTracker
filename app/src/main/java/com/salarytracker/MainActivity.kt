package com.salarytracker

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
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
    private var settings = SalarySettings()

    // 顶部月份选择器
    private lateinit var tvYearMonth: TextView
    private lateinit var btnPrevMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton
    private lateinit var tvMonthTotal: TextView

    // 日期选择
    private lateinit var tvSelectedDate: TextView
    private lateinit var btnPickDate: Button

    // 当天信息
    private lateinit var tvDayTotal: TextView
    private lateinit var tvDayStatus: TextView

    // 输入表单
    private lateinit var etHourlyRate: EditText
    private lateinit var etHours: EditText
    private lateinit var etDailyRate: EditText
    private lateinit var cbHoliday: CheckBox
    private lateinit var etNote: EditText
    private lateinit var btnSave: Button
    private lateinit var btnDelete: Button

    // 月统计列表
    private lateinit var lvRecords: ListView

    private var currentYear = 0
    private var currentMonth = 0
    private var selectedDate = ""
    private var isHoliday = false
    private var isWeekend = false

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    private val displayDateFormat = SimpleDateFormat("yyyy年MM月dd日 E", Locale.CHINA)
    private val monthFormat = SimpleDateFormat("yyyy年MM月", Locale.CHINA)

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
        // 顶部月份
        tvYearMonth   = findViewById(R.id.tv_year_month)
        btnPrevMonth  = findViewById(R.id.btn_prev_month)
        btnNextMonth  = findViewById(R.id.btn_next_month)
        tvMonthTotal  = findViewById(R.id.tv_month_total)

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

        // 日期选择
        tvSelectedDate = findViewById(R.id.tv_selected_date)
        btnPickDate   = findViewById(R.id.btn_pick_date)
        btnPickDate.setOnClickListener { showDatePicker() }

        // 当天信息
        tvDayTotal  = findViewById(R.id.tv_day_total)
        tvDayStatus = findViewById(R.id.tv_day_status)

        // 输入表单
        etHourlyRate = findViewById(R.id.et_hourly_rate)
        etHours      = findViewById(R.id.et_hours)
        etDailyRate  = findViewById(R.id.et_daily_rate)
        cbHoliday    = findViewById(R.id.cb_holiday)
        etNote       = findViewById(R.id.et_note)
        btnSave      = findViewById(R.id.btn_save)
        btnDelete    = findViewById(R.id.btn_delete)

        btnSave.setOnClickListener   { saveRecord() }
        btnDelete.setOnClickListener { deleteRecord() }

        // 月记录列表
        lvRecords = findViewById(R.id.lv_records)
        lvRecords.setOnItemClickListener { _, _, pos, _ ->
            val records = ds.getRecordsForMonth(currentYear, currentMonth)
            if (pos < records.size) {
                val r = records[pos]
                selectedDate = r.date
                updateAll()
            }
        }

        // 设置按钮
        findViewById<Button>(R.id.btn_settings).setOnClickListener {
            showSettingsDialog()
        }

        // 日期点击也能选日期
        tvSelectedDate.setOnClickListener { showDatePicker() }
    }

    private fun updateAll() {
        settings = ds.loadSettings()

        // 月份显示
        val cal = Calendar.getInstance()
        cal.set(currentYear, currentMonth - 1, 1)
        tvYearMonth.text = monthFormat.format(cal.time)

        // 月统计
        val monthStats = ds.getMonthStats(currentYear, currentMonth)
        tvMonthTotal.text = "当月合计: ${String.format("%.2f", monthStats.totalSalary)}元  |  ${String.format("%.1f", monthStats.totalHours)}小时"

        // 选中日期显示
        val parts = selectedDate.split("-")
        val selCal = Calendar.getInstance()
        selCal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        tvSelectedDate.text = displayDateFormat.format(selCal.time)

        // 计算当天属性
        isHoliday = ds.isHoliday(selectedDate)
        val selParts = selectedDate.split("-")
        selCal.set(selParts[0].toInt(), selParts[1].toInt() - 1, selParts[2].toInt())
        val dow = selCal.get(Calendar.DAY_OF_WEEK)
        isWeekend = dow == Calendar.SATURDAY || dow == Calendar.SUNDAY

        // 当天记录
        val record = ds.getRecord(selectedDate)
        if (record != null) {
            tvDayTotal.text = "当天工资: ${String.format("%.2f", record.total)}元"
            val typeStr = when {
                record.isHoliday -> "节假日"
                record.isWeekend -> "周末"
                else -> "工作日"
            }
            tvDayStatus.text = "$typeStr | 时薪${record.hourlyRate}元/h × ${record.hours}h + 日薪${record.dailyRate}元"
            etHourlyRate.setText(String.format("%.1f", record.hourlyRate))
            etHours.setText(String.format("%.1f", record.hours))
            etDailyRate.setText(String.format("%.1f", record.dailyRate))
            cbHoliday.isChecked = record.isHoliday
            etNote.setText(record.note)
            btnDelete.visibility = View.VISIBLE
        } else {
            tvDayTotal.text = "当天工资: 0.00元"
            tvDayStatus.text = if (isHoliday) "节假日" else if (isWeekend) "周末" else "工作日"
            // 自动填充时薪
            val autoRate = when {
                isHoliday -> settings.holidayHourlyRate
                else -> settings.normalHourlyRate
            }
            etHourlyRate.setText(String.format("%.1f", autoRate))
            etHours.setText("")
            etDailyRate.setText("0")
            cbHoliday.isChecked = isHoliday
            etNote.setText("")
            btnDelete.visibility = View.GONE
        }

        // 月记录列表
        val monthRecords = ds.getRecordsForMonth(currentYear, currentMonth)
        val adapter = RecordAdapter(monthRecords)
        lvRecords.adapter = adapter

        if (monthRecords.isEmpty()) {
            tvMonthTotal.text = (tvMonthTotal.text.toString() + " (本月暂无记录)")
        }
    }

    private fun showDatePicker() {
        val parts = selectedDate.split("-")
        val y = parts[0].toInt()
        val m = parts[1].toInt() - 1
        val d = parts[2].toInt()

        DatePickerDialog(this, { _, year, month, day ->
            val cal = Calendar.getInstance()
            cal.set(year, month, day)
            selectedDate = dateFormat.format(cal.time)
            updateAll()
        }, y, m, d).show()
    }

    private fun saveRecord() {
        val hourlyRate = etHourlyRate.text.toString().toDoubleOrNull()
        val hours = etHours.text.toString().toDoubleOrNull()
        val dailyRate = etDailyRate.text.toString().toDoubleOrNull() ?: 0.0

        if (hourlyRate == null || hours == null) {
            Toast.makeText(this, "请填写时薪和小时数", Toast.LENGTH_SHORT).show()
            return
        }

        if (hourlyRate < 0 || hours < 0) {
            Toast.makeText(this, "数值不能为负", Toast.LENGTH_SHORT).show()
            return
        }

        // 如果没填小时，当作0
        val h = hours ?: 0.0

        val record = SalaryRecord.fromForm(
            date = selectedDate,
            hourlyRate = hourlyRate,
            hours = h,
            dailyRate = dailyRate,
            isHoliday = cbHoliday.isChecked,
            isWeekend = isWeekend,
            note = etNote.text.toString()
        )

        ds.saveRecord(record)
        Toast.makeText(this, "已保存: ${String.format("%.2f", record.total)}元", Toast.LENGTH_SHORT).show()
        updateAll()
    }

    private fun deleteRecord() {
        AlertDialog.Builder(this)
            .setTitle("删除记录")
            .setMessage("确定删除 ${selectedDate} 的记录吗？")
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

        val etNormal = dialog.findViewById<EditText>(R.id.et_normal_rate)
        val etOvertime = dialog.findViewById<EditText>(R.id.et_overtime_rate)
        val etHoliday = dialog.findViewById<EditText>(R.id.et_holiday_rate)
        val etHolidays = dialog.findViewById<EditText>(R.id.et_holidays)
        val btnSave = dialog.findViewById<Button>(R.id.btn_save_settings)
        val btnCancel = dialog.findViewById<Button>(R.id.btn_cancel_settings)

        val s = ds.loadSettings()
        etNormal.setText(String.format("%.1f", s.normalHourlyRate))
        etOvertime.setText(String.format("%.1f", s.overtimeHourlyRate))
        etHoliday.setText(String.format("%.1f", s.holidayHourlyRate))
        etHolidays.setText(s.customHolidays.joinToString("\n"))

        btnSave.setOnClickListener {
            val normalRate = etNormal.text.toString().toDoubleOrNull()
            val overtimeRate = etOvertime.text.toString().toDoubleOrNull()
            val holidayRate = etHoliday.text.toString().toDoubleOrNull()
            val holidaysText = etHolidays.text.toString()

            if (normalRate == null || overtimeRate == null || holidayRate == null) {
                Toast.makeText(this, "请填写有效的数值", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val holidays = holidaysText.split("\n")
                .map { it.trim() }
                .filter { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
                .toSet()

            val newSettings = SalarySettings(
                normalHourlyRate = normalRate,
                overtimeHourlyRate = overtimeRate,
                holidayHourlyRate = holidayRate,
                customHolidays = holidays
            )
            ds.saveSettings(newSettings)
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
            val dayLabel = r.date.substring(8) + "日"

            val isToday = r.date == dateFormat.format(Date())

            (view.findViewById<TextView>(R.id.tv_day)).text = dayLabel
            (view.findViewById<TextView>(R.id.tv_hours)).text = "${r.hours}h"
            (view.findViewById<TextView>(R.id.tv_rate)).text = "${r.hourlyRate}/h"
            (view.findViewById<TextView>(R.id.tv_total)).text = "${String.format("%.2f", r.total)}元"

            val typeFlag = view.findViewById<TextView>(R.id.tv_flag)
            val flagText = when {
                r.isHoliday -> "节假日"
                r.isWeekend -> "周末"
                else -> "工作"
            }
            val flagColor = when {
                r.isHoliday -> ContextCompat.getColor(this@MainActivity, R.color.holiday_color)
                r.isWeekend -> ContextCompat.getColor(this@MainActivity, R.color.weekend_color)
                else -> ContextCompat.getColor(this@MainActivity, R.color.workday_color)
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
