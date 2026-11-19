package br.com.brilhoporgestos

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) updateStatus()
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 40, 32, 40)
        }
        val title = TextView(this).apply {
            text = "Brilho por Gestos"
            textSize = 26f
            gravity = Gravity.CENTER
        }
        val description = TextView(this).apply {
            text = "Dê dois toques rápidos na faixa inferior da tela e deslize para cima para aumentar o brilho ou para baixo para diminuir."
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 24)
        }
        status = TextView(this).apply {
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 18)
        }
        val overlayPermission = Button(this).apply {
            text = "Permitir sobreposição"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
        }
        val writePermission = Button(this).apply {
            text = "Permitir alterar brilho"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName")))
            }
        }
        val start = Button(this).apply {
            text = "Ativar Brilho por Gestos"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    status.text = "Primeiro permita a sobreposição."
                } else if (!Settings.System.canWrite(this@MainActivity)) {
                    status.text = "Primeiro permita alterar as configurações do sistema."
                } else {
                    startService(Intent(this@MainActivity, BrightnessOverlayService::class.java))
                    status.text = "Ativo! Use dois toques rápidos na parte inferior da tela."
                }
            }
        }
        val stop = Button(this).apply {
            text = "Desativar"
            setOnClickListener {
                stopService(Intent(this@MainActivity, BrightnessOverlayService::class.java))
                status.text = "Desativado."
            }
        }
        root.addView(title)
        root.addView(description)
        root.addView(status)
        root.addView(overlayPermission)
        root.addView(writePermission)
        root.addView(start)
        root.addView(stop)
        setContentView(root)
        updateStatus()
    }

    private fun updateStatus() {
        val overlay = Settings.canDrawOverlays(this)
        val write = Settings.System.canWrite(this)
        status.text = "Sobreposição: ${if (overlay) "permitida" else "pendente"}\nAjuste de brilho: ${if (write) "permitido" else "pendente"}"
    }
}
