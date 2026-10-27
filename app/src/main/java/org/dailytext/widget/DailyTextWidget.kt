package org.dailytext.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.concurrent.thread

class DailyTextWidget : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        scheduleMidnight(ctx)
        refresh(ctx, force = false)
    }

    override fun onEnabled(ctx: Context) {
        scheduleMidnight(ctx)
    }

    override fun onDisabled(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        am.cancel(broadcast(ctx, ACTION_MIDNIGHT, 2))
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        when (intent.action) {
            ACTION_REFRESH -> refresh(ctx, force = true)
            ACTION_MIDNIGHT, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> {
                scheduleMidnight(ctx)
                refresh(ctx, force = false)
            }
        }
    }

    /** 오늘 성구가 없거나 강제 갱신이면 새로 받아 오고, 모든 위젯을 다시 그린다. */
    private fun refresh(ctx: Context, force: Boolean) {
        val pending = goAsync()
        val app = ctx.applicationContext
        thread {
            try {
                val today = LocalDate.now()
                val cached = DailyTextRepo.load(app)
                if (force || cached?.date != today.toString()) {
                    if (force) updateAll(app, loading = true)
                    try {
                        DailyTextRepo.save(app, DailyTextRepo.fetch(today))
                    } catch (e: Exception) {
                        DailyTextRepo.saveError(app, e.message ?: "불러오지 못했습니다.")
                    }
                }
                updateAll(app, loading = false)
            } catch (e: Exception) {
                // 위젯을 그리다 문제가 생겨도 앱 전체가 멈추지 않게 한다.
                DailyTextRepo.saveError(app, e.message ?: e.javaClass.simpleName)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "org.dailytext.widget.REFRESH"
        const val ACTION_MIDNIGHT = "org.dailytext.widget.MIDNIGHT"

        fun updateAll(ctx: Context, loading: Boolean) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, DailyTextWidget::class.java))
            val today = LocalDate.now()
            val saved = DailyTextRepo.load(ctx)
            val isToday = saved?.date == today.toString()

            for (id in ids) {
                val v = RemoteViews(ctx.packageName, R.layout.widget)

                // 머리글: 날짜(페이지의 제목, 없으면 휴대폰 날짜)
                val heading = if (isToday && saved!!.heading.isNotBlank()) saved.heading
                else "${today.monthValue}월 ${today.dayOfMonth}일 " + dayName(today)
                v.setTextViewText(R.id.heading, heading)

                // 상태 줄
                val status = when {
                    loading -> "불러오는 중…"
                    !isToday -> DailyTextRepo.lastError(ctx)?.let { "⚠ $it  (새로 고침을 눌러 보세요)" }
                        ?: "불러오는 중…"
                    else -> ""
                }
                v.setTextViewText(R.id.status, status)
                v.setViewVisibility(R.id.status, if (status.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE)

                // 새로 고침 버튼
                v.setOnClickPendingIntent(R.id.refresh, broadcast(ctx, ACTION_REFRESH, 1))
                // 머리글 탭 → 원문 열기
                v.setOnClickPendingIntent(R.id.heading, openPage(ctx, today, 3, mutable = false))

                // 본문 목록(스크롤 가능)
                val svc = Intent(ctx, WidgetService::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
                }
                @Suppress("DEPRECATION")
                v.setRemoteAdapter(R.id.list, svc)
                v.setEmptyView(R.id.list, R.id.empty)
                v.setPendingIntentTemplate(R.id.list, openPage(ctx, today, 4, mutable = true))

                mgr.updateAppWidget(id, v)
            }
            @Suppress("DEPRECATION")
            mgr.notifyAppWidgetViewDataChanged(ids, R.id.list)
        }

        private fun dayName(d: LocalDate): String =
            arrayOf("월요일", "화요일", "수요일", "목요일", "금요일", "토요일", "일요일")[d.dayOfWeek.value - 1]

        /**
         * 원문 페이지 열기. Android 14부터 '변경 가능 + 암시적 인텐트' 조합은 금지되어 앱이 멈추므로,
         * 우리 앱의 OpenActivity를 명시적으로 지정하고 그 화면이 브라우저를 열게 한다.
         */
        private fun openPage(ctx: Context, date: LocalDate, code: Int, mutable: Boolean): PendingIntent {
            val i = Intent(ctx, OpenActivity::class.java)
                .setData(Uri.parse(DailyTextRepo.pageUrl(date)))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val flag = if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE
            return PendingIntent.getActivity(ctx, code, i, PendingIntent.FLAG_UPDATE_CURRENT or flag)
        }

        fun broadcast(ctx: Context, action: String, code: Int): PendingIntent {
            val i = Intent(ctx, DailyTextWidget::class.java).setAction(action)
            return PendingIntent.getBroadcast(
                ctx, code, i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        /** 다음 자정 1분 뒤에 새 성구를 받아 오도록 예약한다. */
        fun scheduleMidnight(ctx: Context) {
            val zone = ZoneId.systemDefault()
            val at = LocalDate.now().plusDays(1).atTime(LocalTime.of(0, 1))
                .atZone(zone).toInstant().toEpochMilli()
            val am = ctx.getSystemService(AlarmManager::class.java)
            am.setAndAllowWhileIdle(AlarmManager.RTC, at, broadcast(ctx, ACTION_MIDNIGHT, 2))
        }
    }
}
