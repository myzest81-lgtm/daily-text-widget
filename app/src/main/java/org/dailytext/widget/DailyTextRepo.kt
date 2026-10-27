package org.dailytext.widget

import android.content.Context
import android.graphics.Typeface
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

/** 하루 치 성구. 각 필드는 원문 HTML 조각을 그대로 보관한다. */
data class DailyText(
    val date: String,      // 2026-10-27
    val heading: String,   // 10월 27일 화요일
    val themeHtml: String, // 주제 성구
    val bodyHtml: String,  // 본문(출처 포함)
)

object DailyTextRepo {
    private const val PREFS = "daily_text"
    private const val BASE = "https://wol.jw.org/ko/wol/h/r8/lp-ko"

    /** 그날의 원문 페이지 주소 (위젯을 탭하면 열린다). */
    fun pageUrl(date: LocalDate): String =
        "$BASE/${date.year}/${date.monthValue}/${date.dayOfMonth}"

    /** wol.jw.org 첫 페이지(날짜 지정)를 받아 해당 날짜 블록만 잘라 낸다. */
    fun fetch(date: LocalDate): DailyText {
        val html = download(pageUrl(date))
        val iso = date.toString()

        // 한 페이지에 어제·오늘·내일이 함께 있으므로 data-date로 오늘 블록을 찾는다.
        val start = html.indexOf("data-date=\"$iso")
        require(start >= 0) { "오늘($iso) 성구를 페이지에서 찾지 못했습니다." }
        val next = html.indexOf("class=\"tabContent\"", start + 20)
        val block = html.substring(start, if (next < 0) html.length else next)

        val opts = setOf(RegexOption.DOT_MATCHES_ALL)
        val heading = Regex("<h2[^>]*>(.*?)</h2>", opts).find(block)?.groupValues?.get(1)
        val theme = Regex("class=\"themeScrp\"[^>]*>(.*?)</p>", opts).find(block)?.groupValues?.get(1)
        val body = Regex("class=\"bodyTxt\"[^>]*>(.*?)</div>", opts).find(block)?.groupValues?.get(1)
        require(theme != null && body != null) { "페이지 구조가 바뀌어 성구를 읽지 못했습니다." }

        return DailyText(iso, plain(heading ?: ""), theme.trim(), body.trim())
    }

    private fun download(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 20_000
        conn.setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126 Mobile Safari/537.36"
        )
        conn.setRequestProperty("Accept-Language", "ko-KR,ko;q=0.9")
        try {
            if (conn.responseCode !in 200..299) error("서버 응답 ${conn.responseCode}")
            return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    // ---------- 저장 ----------

    fun save(ctx: Context, t: DailyText) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("date", t.date)
            .putString("heading", t.heading)
            .putString("theme", t.themeHtml)
            .putString("body", t.bodyHtml)
            .putString("error", null)
            .apply()
    }

    fun saveError(ctx: Context, message: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("error", message).apply()
    }

    fun load(ctx: Context): DailyText? {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val date = p.getString("date", null) ?: return null
        return DailyText(
            date,
            p.getString("heading", "") ?: "",
            p.getString("theme", "") ?: "",
            p.getString("body", "") ?: "",
        )
    }

    fun lastError(ctx: Context): String? =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("error", null)

    // ---------- 표시용 변환 ----------

    fun plain(html: String): String =
        Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim()

    /** HTML → 위젯에 쓸 글자. 성구 링크는 파란색으로 바꾼다(위젯은 링크 자체를 지원하지 않음). */
    /** 주제 성구용: 성구 출처 앞의 대시(—)에서 항상 줄을 바꾼다. */
    fun themeStyled(html: String, linkColor: Int): CharSequence {
        val sb = SpannableStringBuilder(styled(html, linkColor, italic = true))
        val dash = sb.lastIndexOf('—')
        if (dash > 0) {
            var cut = dash
            while (cut > 0 && sb[cut - 1].isWhitespace()) cut--
            sb.replace(cut, dash, "\n")
        }
        return sb
    }

    fun styled(html: String, linkColor: Int, italic: Boolean = false): CharSequence {
        val src = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        val sb = SpannableStringBuilder(src)
        for (span in sb.getSpans(0, sb.length, URLSpan::class.java)) {
            val s = sb.getSpanStart(span)
            val e = sb.getSpanEnd(span)
            sb.removeSpan(span)
            sb.setSpan(ForegroundColorSpan(linkColor), s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        if (italic) sb.setSpan(StyleSpan(Typeface.ITALIC), 0, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        // 앞뒤 공백·줄바꿈 정리
        while (sb.isNotEmpty() && sb[sb.length - 1].isWhitespace()) sb.delete(sb.length - 1, sb.length)
        while (sb.isNotEmpty() && sb[0].isWhitespace()) sb.delete(0, 1)
        return sb
    }
}
