package org.dailytext.widget

import android.content.Context

/** 사용자가 바꿀 수 있는 위젯 표시 설정. */
object WidgetSettings {
    private const val PREFS = "widget_settings"

    const val DEFAULT_SIZE = 15      // 본문 글자 크기(sp)
    const val MIN_SIZE = 10
    const val MAX_SIZE = 26
    const val DEFAULT_OPACITY = 90   // 배경 불투명도(%) — 0이면 완전히 투명

    fun fontSize(ctx: Context): Int =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("size", DEFAULT_SIZE)

    fun opacity(ctx: Context): Int =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("opacity", DEFAULT_OPACITY)

    /** 본문·출처 표시 여부. 기본은 날짜와 주제 성구만 보인다. */
    fun showBody(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("showBody", false)

    fun setShowBody(ctx: Context, v: Boolean) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("showBody", v).apply()

    fun setFontSize(ctx: Context, v: Int) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt("size", v).apply()

    fun setOpacity(ctx: Context, v: Int) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt("opacity", v).apply()

    /** 본문 크기를 기준으로 나머지 글자 크기를 비례해서 정한다. */
    fun headingSize(base: Int) = base + 4f
    fun themeSize(base: Int) = base.toFloat()
    fun bodySize(base: Int) = base.toFloat()
    fun sourceSize(base: Int) = base - 1f
    fun statusSize(base: Int) = (base - 3f).coerceAtLeast(9f)

    /** 0–100 % → 0–255 */
    fun alpha255(percent: Int) = (percent.coerceIn(0, 100) * 255 + 50) / 100
}
