package org.dailytext.widget

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView

/** 설치 확인용 안내 화면. 위젯은 홈 화면에서 추가한다. */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (24 * resources.displayMetrics.density).toInt()
        setContentView(TextView(this).apply {
            text = "설치되었습니다.\n\n홈 화면의 빈 곳을 길게 누르고\n위젯 → '오늘의 성구'를 끌어다 놓으세요.\n\n" +
                "· 위젯 안의 본문은 위아래로 스크롤됩니다.\n" +
                "· ↻ 버튼: 새로 고침\n" +
                "· 날짜나 본문을 누르면 원문 페이지가 열립니다."
            textSize = 17f
            gravity = Gravity.CENTER
            setPadding(pad, pad, pad, pad)
        })
        // 앱을 열 때 위젯도 한 번 갱신
        sendBroadcast(
            android.content.Intent(this, DailyTextWidget::class.java)
                .setAction(DailyTextWidget.ACTION_REFRESH)
        )
    }
}
