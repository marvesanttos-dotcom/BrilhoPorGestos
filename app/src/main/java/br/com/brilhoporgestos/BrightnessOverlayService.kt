package br.com.brilhoporgestos

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

class BrightnessOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var tapStrip: View? = null
    private var lastTapAt = 0L
    private var startY = 0f
    private var startBrightness = 128
    private var trackingGesture = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showTapStrip()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (tapStrip == null) showTapStrip()
        return START_STICKY
    }

    private fun showTapStrip() {
        if (!Settings.canDrawOverlays(this) || tapStrip != null) return

        val strip = View(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        val now = System.currentTimeMillis()
                        trackingGesture = lastTapAt != 0L &&
                            now - lastTapAt in 1..450

                        if (trackingGesture) {
                            lastTapAt = 0L
                            startY = event.rawY
                            // O controle manual permite que o valor escolhido seja aplicado
                            // mesmo quando o brilho automático estava ativado.
                            try {
                                Settings.System.putInt(
                                    contentResolver,
                                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                                )
                            } catch (_: Exception) { }
                            startBrightness = Settings.System.getInt(
                                contentResolver,
                                Settings.System.SCREEN_BRIGHTNESS,
                                128
                            ).coerceIn(1, 255)
                        }
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if (trackingGesture) {
                            val delta = startY - event.rawY
                            val change = (delta / dp(3)).toInt()
                            val value = (startBrightness + change).coerceIn(1, 255)
                            try {
                                Settings.System.putInt(
                                    contentResolver,
                                    Settings.System.SCREEN_BRIGHTNESS,
                                    value
                                )
                            } catch (_: Exception) { }
                        }
                        true
                    }

                    MotionEvent.ACTION_UP -> {
                        if (trackingGesture) {
                            trackingGesture = false
                        } else {
                            lastTapAt = System.currentTimeMillis()
                        }
                        true
                    }

                    MotionEvent.ACTION_CANCEL -> {
                        trackingGesture = false
                        true
                    }

                    else -> true
                }
            }
        }

        tapStrip = strip
        try {
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                dp(72),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }
            windowManager.addView(strip, params)
        } catch (_: Exception) {
            tapStrip = null
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        tapStrip?.let {
            try { windowManager.removeView(it) } catch (_: Exception) { }
        }
        tapStrip = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
