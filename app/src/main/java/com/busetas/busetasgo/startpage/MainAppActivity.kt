package com.busetas.busetasgo.startpage

import android.os.Bundle
import android.content.Intent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.busetas.busetasgo.R

class MainAppActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val btnPasajero = findViewById<AppCompatButton>(R.id.btnPasajero)
        btnPasajero.setOnClickListener {
            val intent = Intent(this, InicioSesionP::class.java)
            startActivity(intent)
        }
        val btnConductor = findViewById<AppCompatButton>(R.id.btnConductor)
        btnConductor.setOnClickListener {
            val intent = Intent(this, IniciarSesionC::class.java)
            startActivity(intent)
        }
    }
}
