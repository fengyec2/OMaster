package com.silas.omaster.data.local

import android.content.Context
import android.content.SharedPreferences
import android.icu.util.ChineseCalendar
import com.silas.omaster.R
import java.time.LocalDate
import kotlin.random.Random

data class HolidayGreeting(
    val holidayKey: String,
    val titleResId: Int,
    val greetings: List<Int>,
    val emoji: String
)

class HolidayGreetingManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val resources = context.resources

    fun getTodayHoliday(): HolidayGreeting? {
        val today = LocalDate.now()
        val chineseCalendar = ChineseCalendar()
        chineseCalendar.timeInMillis = System.currentTimeMillis()
        val lunarMonth = chineseCalendar.get(ChineseCalendar.MONTH) + 1
        val lunarDay = chineseCalendar.get(ChineseCalendar.DAY_OF_MONTH)
        val isLeap = chineseCalendar.get(ChineseCalendar.IS_LEAP_MONTH) == 1

        for (holiday in SOLAR_HOLIDAYS) {
            if (holiday.matchSolar(today.monthValue, today.dayOfMonth)) {
                val greeting = buildHolidayGreeting(holiday)
                val year = today.year
                if (!hasShownToday(greeting.holidayKey, year)) {
                    return greeting
                }
            }
        }

        if (!isLeap) {
            for (holiday in LUNAR_HOLIDAYS) {
                if (holiday.matchLunar(lunarMonth, lunarDay)) {
                    val greeting = buildHolidayGreeting(holiday)
                    val year = today.year
                    if (!hasShownToday(greeting.holidayKey, year)) {
                        return greeting
                    }
                }
            }
        }

        return null
    }

    fun hasShownToday(key: String): Boolean {
        val today = LocalDate.now()
        return hasShownToday(key, today.year)
    }

    private fun hasShownToday(key: String, year: Int): Boolean {
        val shownKey = "$KEY_PREFIX${key}_$year"
        return prefs.getBoolean(shownKey, false)
    }

    fun markShown(key: String) {
        val today = LocalDate.now()
        val shownKey = "$KEY_PREFIX${key}_${today.year}"
        prefs.edit().putBoolean(shownKey, true).apply()
    }

    fun getRandomGreeting(greetingsResIds: List<Int>): String {
        if (greetingsResIds.isEmpty()) return ""
        val index = Random.nextInt(greetingsResIds.size)
        return resources.getString(greetingsResIds[index])
    }

    private fun buildHolidayGreeting(holiday: HolidayDef): HolidayGreeting {
        return HolidayGreeting(
            holidayKey = holiday.key,
            titleResId = holiday.titleResId,
            greetings = holiday.greetingResIds,
            emoji = holiday.emoji
        )
    }

    private data class HolidayDef(
        val key: String,
        val titleResId: Int,
        val greetingResIds: List<Int>,
        val emoji: String,
        val lunarMonth: Int = -1,
        val lunarDay: Int = -1,
        val solarMonth: Int = -1,
        val solarDay: Int = -1
    ) {
        fun matchLunar(month: Int, day: Int): Boolean {
            return lunarMonth == month && lunarDay == day
        }

        fun matchSolar(month: Int, day: Int): Boolean {
            return solarMonth == month && solarDay == day
        }
    }

    companion object {
        private const val PREFS_NAME = "omaster_holiday_greetings"
        private const val KEY_PREFIX = "holiday_"

        @Volatile
        private var INSTANCE: HolidayGreetingManager? = null

        fun getInstance(context: Context): HolidayGreetingManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HolidayGreetingManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }

        private val SOLAR_HOLIDAYS = listOf(
            HolidayDef(
                key = "new_year",
                titleResId = R.string.holiday_new_year_title,
                greetingResIds = listOf(
                    R.string.holiday_new_year_1,
                    R.string.holiday_new_year_2
                ),
                emoji = "🎆",
                solarMonth = 1, solarDay = 1
            ),
            HolidayDef(
                key = "valentine",
                titleResId = R.string.holiday_valentine_title,
                greetingResIds = listOf(
                    R.string.holiday_valentine_1,
                    R.string.holiday_valentine_2
                ),
                emoji = "💕",
                solarMonth = 2, solarDay = 14
            ),
            HolidayDef(
                key = "labor_day",
                titleResId = R.string.holiday_labor_day_title,
                greetingResIds = listOf(
                    R.string.holiday_labor_day_1
                ),
                emoji = "☀️",
                solarMonth = 5, solarDay = 1
            ),
            HolidayDef(
                key = "national_day",
                titleResId = R.string.holiday_national_day_title,
                greetingResIds = listOf(
                    R.string.holiday_national_day_1,
                    R.string.holiday_national_day_2
                ),
                emoji = "🍂",
                solarMonth = 10, solarDay = 1
            ),
            HolidayDef(
                key = "christmas",
                titleResId = R.string.holiday_christmas_title,
                greetingResIds = listOf(
                    R.string.holiday_christmas_1,
                    R.string.holiday_christmas_2
                ),
                emoji = "🎄",
                solarMonth = 12, solarDay = 25
            )
        )

        private val LUNAR_HOLIDAYS = listOf(
            HolidayDef(
                key = "spring_festival",
                titleResId = R.string.holiday_spring_festival_title,
                greetingResIds = listOf(
                    R.string.holiday_spring_festival_1,
                    R.string.holiday_spring_festival_2
                ),
                emoji = "🧧",
                lunarMonth = 1, lunarDay = 1
            ),
            HolidayDef(
                key = "lantern_festival",
                titleResId = R.string.holiday_lantern_festival_title,
                greetingResIds = listOf(
                    R.string.holiday_lantern_festival_1,
                    R.string.holiday_lantern_festival_2
                ),
                emoji = "🏮",
                lunarMonth = 1, lunarDay = 15
            ),
            HolidayDef(
                key = "dragon_boat",
                titleResId = R.string.holiday_dragon_boat_title,
                greetingResIds = listOf(
                    R.string.holiday_dragon_boat_1,
                    R.string.holiday_dragon_boat_2
                ),
                emoji = "🐉",
                lunarMonth = 5, lunarDay = 5
            ),
            HolidayDef(
                key = "qixi",
                titleResId = R.string.holiday_qixi_title,
                greetingResIds = listOf(
                    R.string.holiday_qixi_1,
                    R.string.holiday_qixi_2
                ),
                emoji = "💕",
                lunarMonth = 7, lunarDay = 7
            ),
            HolidayDef(
                key = "mid_autumn",
                titleResId = R.string.holiday_mid_autumn_title,
                greetingResIds = listOf(
                    R.string.holiday_mid_autumn_1,
                    R.string.holiday_mid_autumn_2
                ),
                emoji = "🌕",
                lunarMonth = 8, lunarDay = 15
            ),
            HolidayDef(
                key = "double_ninth",
                titleResId = R.string.holiday_double_ninth_title,
                greetingResIds = listOf(
                    R.string.holiday_double_ninth_1,
                    R.string.holiday_double_ninth_2
                ),
                emoji = "🎋",
                lunarMonth = 9, lunarDay = 9
            )
        )
    }
}
