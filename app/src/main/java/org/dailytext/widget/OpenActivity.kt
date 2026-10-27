package org.dailytext.widget

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/** 화면 없이 원문 주소를 브라우저로 넘기고 바로 닫히는 중계 화면. */
class OpenActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent?.data?.let { url ->
            try {
                startActivity(Intent(Intent.ACTION_VIEW, url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: Exception) {
            }
        }
        finish()
    }
}
