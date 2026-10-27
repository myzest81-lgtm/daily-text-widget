package org.dailytext.widget

import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import java.time.LocalDate

/** 위젯 안에서 본문을 스크롤할 수 있도록 한 줄짜리 목록으로 제공한다. */
class WidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = Factory(applicationContext)

    private class Factory(private val ctx: Context) : RemoteViewsFactory {
        private var data: DailyText? = null

        override fun onCreate() {}
        override fun onDestroy() {}

        override fun onDataSetChanged() {
            val d = DailyTextRepo.load(ctx)
            data = if (d?.date == LocalDate.now().toString()) d else null
        }

        override fun getCount(): Int = if (data == null) 0 else 1

        override fun getViewAt(position: Int): RemoteViews {
            val v = RemoteViews(ctx.packageName, R.layout.widget_item)
            val d = data ?: return v
            val link = ctx.getColor(R.color.link)
            val size = WidgetSettings.fontSize(ctx)
            v.setTextViewTextSize(R.id.theme, TypedValue.COMPLEX_UNIT_SP, WidgetSettings.themeSize(size))
            v.setTextViewTextSize(R.id.body, TypedValue.COMPLEX_UNIT_SP, WidgetSettings.bodySize(size))
            v.setTextViewTextSize(R.id.source, TypedValue.COMPLEX_UNIT_SP, WidgetSettings.sourceSize(size))
            v.setTextViewText(R.id.theme, DailyTextRepo.styled(d.themeHtml, link, italic = true))
            v.setTextViewText(R.id.body, DailyTextRepo.styled(d.bodyHtml, link))
            v.setOnClickFillInIntent(R.id.item_root, Intent())
            return v
        }

        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount(): Int = 1
        override fun getItemId(position: Int): Long = position.toLong()
        override fun hasStableIds(): Boolean = true
    }
}
