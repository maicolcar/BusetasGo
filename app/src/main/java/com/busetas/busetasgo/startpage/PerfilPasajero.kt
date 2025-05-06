package com.busetas.busetasgo.startpage

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.busetas.busetasgo.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PerfilPasajero : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var nombreTextView: AppCompatTextView
    private lateinit var fechaNacimientoTextView: AppCompatTextView
    private lateinit var correoTextView: AppCompatTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        enableEdgeToEdge()
        setContentView(R.layout.activity_perfil_pasajero)
        
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Inicializar las vistas
        nombreTextView = findViewById(R.id.contieneNom)
        fechaNacimientoTextView = findViewById(R.id.contieneApe)
        correoTextView = findViewById(R.id.contieneCorreo)

        // Verificar si el usuario está autenticado
        if (auth.currentUser == null) {
            val intent = Intent(this, InicioSesionP::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Cargar la información del usuario
        cargarInformacionUsuario()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val cerrarSesion = findViewById<AppCompatButton>(R.id.registrarP)
        cerrarSesion.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, InicioSesionP::class.java)
            startActivity(intent)
            finish()
        }

        val home = findViewById<AppCompatButton>(R.id.home)
        home.setOnClickListener {
            // No hacemos nada ya que estamos en la pantalla de perfil
        }

        val busetasCercanas = findViewById<AppCompatButton>(R.id.busetas_cercanas)
        busetasCercanas.setOnClickListener {
            try {
                // Verificar si el usuario sigue autenticado
                if (auth.currentUser == null) {
                    val intent = Intent(this, InicioSesionP::class.java)
            startActivity(intent)
                    finish()
                    return@setOnClickListener
                }

                // Ocultar todos los elementos de la interfaz actual
                findViewById<AppCompatButton>(R.id.registrarP).visibility = android.view.View.GONE
                findViewById<AppCompatButton>(R.id.home).visibility = android.view.View.GONE
                findViewById<AppCompatButton>(R.id.busetas_cercanas).visibility = android.view.View.GONE
                
                // Ocultar otros elementos de la interfaz
                findViewById<androidx.appcompat.widget.AppCompatImageView>(R.id.imagenP).visibility = android.view.View.GONE
                findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.usuarioP).visibility = android.view.View.GONE
                findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.nom_usuario).visibility = android.view.View.GONE
                findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.contieneNom).visibility = android.view.View.GONE
                findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.ape_usuario).visibility = android.view.View.GONE
                findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.contieneApe).visibility = android.view.View.GONE
                findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.correo_usuario).visibility = android.view.View.GONE
                findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.contieneCorreo).visibility = android.view.View.GONE
                
                // Mostrar el contenedor del mapa
                findViewById<android.widget.FrameLayout>(R.id.map_container).visibility = android.view.View.VISIBLE
                
                // Cargar el fragmento de busetas cercanas
                val fragment = BusetasCercanasFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.map_container, fragment)
                    .addToBackStack(null)
                    .commit()
            } catch (e: Exception) {
                Toast.makeText(this, "Error al cargar el mapa: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    private fun cargarInformacionUsuario() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("usuarios").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Mostrar la información en los TextView
                        nombreTextView.text = document.getString("nombre") ?: ""
                        fechaNacimientoTextView.text = document.getString("fechaNacimiento") ?: ""
                        correoTextView.text = auth.currentUser?.email ?: ""
                    } else {
                        Toast.makeText(this, "No se encontró información del usuario", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al cargar la información: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            // Ocultar el contenedor del mapa
            findViewById<android.widget.FrameLayout>(R.id.map_container).visibility = android.view.View.GONE
            
            // Mostrar todos los elementos de la interfaz nuevamente
            findViewById<AppCompatButton>(R.id.registrarP).visibility = android.view.View.VISIBLE
            findViewById<AppCompatButton>(R.id.home).visibility = android.view.View.VISIBLE
            findViewById<AppCompatButton>(R.id.busetas_cercanas).visibility = android.view.View.VISIBLE
            
            // Mostrar otros elementos de la interfaz
            findViewById<androidx.appcompat.widget.AppCompatImageView>(R.id.imagenP).visibility = android.view.View.VISIBLE
            findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.usuarioP).visibility = android.view.View.VISIBLE
            findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.nom_usuario).visibility = android.view.View.VISIBLE
            findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.contieneNom).visibility = android.view.View.VISIBLE
            findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.ape_usuario).visibility = android.view.View.VISIBLE
            findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.contieneApe).visibility = android.view.View.VISIBLE
            findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.correo_usuario).visibility = android.view.View.VISIBLE
            findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.contieneCorreo).visibility = android.view.View.VISIBLE
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        // Verificar si el usuario sigue autenticado
        if (auth.currentUser == null) {
            val intent = Intent(this, InicioSesionP::class.java)
            startActivity(intent)
            finish()
        }
    }
}