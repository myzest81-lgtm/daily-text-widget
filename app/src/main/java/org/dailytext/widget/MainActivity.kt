package org.dailytext.widget

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView

/** 설정 화면: 글자 크기와 배경 투명도를 바꾸고 미리 본다. 위젯의 ⚙ 버튼으로도 열린다. */
class MainActivity : Activity() {

    private lateinit var cardBg: GradientDrawable
    private lateinit var pHeading: TextView
    private lateinit var pTheme: TextView
    private lateinit var pBody: TextView
    private lateinit var pSource: TextView
    private lateinit var sizeLabel: TextView
    private lateinit var opacityLabel: TextView
    private lateinit var sizeBar: SeekBar
    private lateinit var opacityBar: SeekBar
    private var bodySwitch: android.widget.Switch? = null

    private fun dp(x: Int) = (x * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val textColor = getColor(R.color.text)
        val muted = getColor(R.color.muted)
        val link = getColor(R.color.link)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(28), dp(20), dp(28))
        }

        root.addView(TextView(this).apply {
            text = "오늘의 성구 위젯 설정"
            textSize = 21f
            setTypeface(null, Typeface.BOLD)
            setTextColor(getColor(R.color.title))
        })
        root.addView(TextView(this).apply {
            text = "홈 화면 빈 곳을 길게 누르고 위젯 → '오늘의 성구'를 놓으세요. " +
                "위젯 왼쪽 위 ⚙ 버튼으로 언제든 이 화면을 열 수 있습니다."
            textSize = 14f
            setTextColor(muted)
            setPadding(0, dp(8), 0, dp(18))
        })

        // ---- 미리 보기: 투명도가 보이도록 색 있는 바탕 위에 위젯 모양을 올린다 ----
        val backdrop = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xFF4F7CC4.toInt(), 0xFF8E6BB5.toInt(), 0xFFE3A35C.toInt())
            ).apply { cornerRadius = dp(18).toFloat() }
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        cardBg = GradientDrawable().apply {
            setColor(getColor(R.color.bg))
            cornerRadius = dp(22).toFloat()
        }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBg
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }
        pHeading = TextView(this).apply {
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            setTextColor(getColor(R.color.title))
        }
        pTheme = TextView(this).apply {
            gravity = Gravity.CENTER
            setTextColor(textColor)
            setLineSpacing(0f, 1.15f)
            setPadding(0, dp(8), 0, 0)
        }
        pBody = TextView(this).apply {
            setTextColor(textColor)
            setLineSpacing(0f, 1.3f)
            maxLines = 5
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, dp(10), 0, 0)
        }
        pSource = TextView(this).apply {
            text = getString(R.string.source)
            setTextColor(link)
            setPadding(0, dp(10), 0, 0)
        }
        card.addView(pHeading); card.addView(pTheme); card.addView(pBody); card.addView(pSource)
        backdrop.addView(card)
        root.addView(backdrop)

        val data = DailyTextRepo.load(this)
        if (data != null) {
            pHeading.text = data.heading
            pTheme.text = DailyTextRepo.styled(data.themeHtml, link, italic = true)
            pBody.text = DailyTextRepo.styled(data.bodyHtml, link)
        } else {
            pHeading.text = "10월 27일 화요일"
            pTheme.text = DailyTextRepo.styled("<em>정신을 새롭게 함으로 변화되십시오.—</em><a href='#'><em>로마 12:2</em></a><em>.</em>", link)
            pBody.text = "한때 사나운 짐승 같은 성격을 가지고 있던 사람들이 하느님의 영의 도움으로 삶을 크게 변화시켰습니다."
        }

        // ---- 글자 크기 ----
        sizeLabel = sectionLabel()
        root.addView(sizeLabel)
        sizeBar = SeekBar(this).apply {
            min = WidgetSettings.MIN_SIZE
            max = WidgetSettings.MAX_SIZE
            progress = WidgetSettings.fontSize(this@MainActivity)
            setOnSeekBarChangeListener(listener { WidgetSettings.setFontSize(this@MainActivity, it) })
        }
        root.addView(sizeBar)

        // ---- 배경 불투명도 ----
        opacityLabel = sectionLabel()
        root.addView(opacityLabel)
        opacityBar = SeekBar(this).apply {
            min = 0
            max = 100
            progress = WidgetSettings.opacity(this@MainActivity)
            setOnSeekBarChangeListener(listener { WidgetSettings.setOpacity(this@MainActivity, it) })
        }
        root.addView(opacityBar)
        root.addView(TextView(this).apply {
            text = "0%는 완전히 투명(배경화면이 그대로 보임), 100%는 불투명입니다."
            textSize = 13f
            setTextColor(muted)
            setPadding(dp(4), dp(2), 0, 0)
        })

        // ---- 본문 표시 ----
        bodySwitch = android.widget.Switch(this).apply {
            text = "본문도 표시하기"
            textSize = 16f
            setTextColor(getColor(R.color.title))
            isChecked = WidgetSettings.showBody(this@MainActivity)
            setPadding(dp(4), dp(24), 0, 0)
            setOnCheckedChangeListener { _, on ->
                WidgetSettings.setShowBody(this@MainActivity, on)
                applyPreview()
                pushToWidgets()
            }
        }
        root.addView(bodySwitch)
        root.addView(TextView(this).apply {
            text = "끄면 날짜와 주제 성구만 보입니다. 위젯을 누르면 언제든 원문 전체가 열립니다."
            textSize = 13f
            setTextColor(muted)
            setPadding(dp(4), dp(2), 0, 0)
        })

        // ---- 기본값 ----
        root.addView(Button(this).apply {
            text = "기본값으로 되돌리기 (15 · 90%)"
            setOnClickListener {
                WidgetSettings.setFontSize(this@MainActivity, WidgetSettings.DEFAULT_SIZE)
                WidgetSettings.setOpacity(this@MainActivity, WidgetSettings.DEFAULT_OPACITY)
                sizeBar.progress = WidgetSettings.DEFAULT_SIZE
                opacityBar.progress = WidgetSettings.DEFAULT_OPACITY
                applyPreview()
                pushToWidgets()
            }
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(22); gravity = Gravity.CENTER_HORIZONTAL })

        setContentView(ScrollView(this).apply {
            setBackgroundColor(getColor(R.color.bg))
            addView(root)
        })
        applyPreview()

        // 앱을 열 때 위젯 성구도 한 번 갱신
        sendBroadcast(Intent(this, DailyTextWidget::class.java).setAction(DailyTextWidget.ACTION_REFRESH))
    }

    private fun sectionLabel() = TextView(this).apply {
        textSize = 16f
        setTypeface(null, Typeface.BOLD)
        setTextColor(getColor(R.color.title))
        setPadding(dp(4), dp(24), 0, dp(4))
    }

    /** 움직이는 동안은 미리 보기만, 손을 떼면 저장하고 위젯에 반영한다. */
    private fun listener(save: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(sb: SeekBar, value: Int, fromUser: Boolean) {
            if (fromUser) save(value)
            applyPreview()
        }
        override fun onStartTrackingTouch(sb: SeekBar) {}
        override fun onStopTrackingTouch(sb: SeekBar) = pushToWidgets()
    }

    private fun applyPreview() {
        val size = sizeBar.progress
        val opacity = opacityBar.progress
        sizeLabel.text = "글자 크기: $size"
        opacityLabel.text = "배경 불투명도: $opacity%"
        pHeading.setTextSize(TypedValue.COMPLEX_UNIT_SP, WidgetSettings.headingSize(size))
        pTheme.setTextSize(TypedValue.COMPLEX_UNIT_SP, WidgetSettings.themeSize(size))
        pBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, WidgetSettings.bodySize(size))
        pSource.setTextSize(TypedValue.COMPLEX_UNIT_SP, WidgetSettings.sourceSize(size))
        cardBg.alpha = WidgetSettings.alpha255(opacity)
        val vis = if (WidgetSettings.showBody(this)) android.view.View.VISIBLE else android.view.View.GONE
        pBody.visibility = vis
        pSource.visibility = vis
    }

    private fun pushToWidgets() {
        try {
            DailyTextWidget.updateAll(applicationContext, loading = false)
        } catch (_: Exception) {
        }
    }

    override fun onPause() {
        super.onPause()
        pushToWidgets()
    }
}
