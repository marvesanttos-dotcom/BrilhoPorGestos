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
import kotlin.math.abs

class BrightnessOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var tapStrip: View? = null
    private var gestureLayer: View? = null
    private var lastTapAt = 0L
    private var startY = 0f
    private var startBrightness = 0
    private var active = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showTapStrip()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (tapStrip == null && !active) showTapStrip()
        return START_STICKY
    }

    private fun overlayParams(width: Int, height: Int, gravity: Int): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { this.gravity = gravity }
    }

    private fun showTapStrip() {
        if (!Settings.canDrawOverlays(this) || tapStrip != null) return
        val strip = View(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    val now = System.currentTimeMillis()
                    if (now - lastTapAt in 1..450) {
                        lastTapAt = 0L
                        beginGesture()
                    } else {
                        lastTapAt = now
                    }
                }
                true
            }
        }
        tapStrip = strip
        try {
            windowManager.addView(strip, overlayParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                dp(72),
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ))
        } catch (_: Exception) {
            tapStrip = null
        }
    }

    private fun beginGesture() {
        if (active || !Settings.System.canWrite(this)) return
        active = true
        tapStrip?.let { try { windowManager.removeView(it) } catch (_: Exception) { } }
        tapStrip = null

        val current = Settings.System.getInt(
            contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128
        ).coerceIn(0, 255)
        startBrightness = current
        val layer = View(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        startY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val delta = startY - event.rawY
                        val change = (delta / dp(3)).toInt()
                        val value = (startBrightness + change).coerceIn(1, 255)
                        try {
                            Settings.System.putInt(
                                contentResolver, Settings.System.SCREEN_BRIGHTNESS, value
                            )
                        } catch (_: Exception) { }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        endGesture()
                        true
                    }
                    else -> true
                }
            }
        }
        gestureLayer = layer
        try {
            windowManager.addView(layer, overlayParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                Gravity.TOP or Gravity.START
            ))
        } catch (_: Exception) {
            gestureLayer = null
            active = false
            showTapStrip()
        }
    }

    private fun endGesture() {
        gestureLayer?.let { try { windowManager.removeView(it) } catch (_: Exception) { } }
        gestureLayer = null
        active = false
        showTapStrip()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        tapStrip?.let { try { windowManager.removeView(it) } catch (_: Exception) { } }
        gestureLayer?.let { try { windowManager.removeView(it) } catch (_: Exception) { } }
        tapStrip = null
        gestureLayer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
