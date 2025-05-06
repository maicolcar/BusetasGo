package com.busetas.busetasgo.startpage

import android.os.Bundle
import android.content.Intent
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.busetas.busetasgo.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class InicioSesionP : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_inicio_sesion_p)
        
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val correo = findViewById<AppCompatEditText>(R.id.correo)
        val password = findViewById<AppCompatEditText>(R.id.password)
        val crearCuenta = findViewById<AppCompatButton>(R.id.crearCuenta)
        val inicio_sesion = findViewById<AppCompatButton>(R.id.inicio_sesion)
        val new_passwordP = findViewById<AppCompatTextView>(R.id.new_passwordP)
        val cambiarAConductor = findViewById<AppCompatButton>(R.id.cambiarAConductor)

        crearCuenta.setOnClickListener {
            val intent = Intent(this, CrearCuentaP::class.java)
            startActivity(intent)
        }

        inicio_sesion.setOnClickListener {
            val email = correo.text.toString()
            val pass = password.text.toString()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            // Verificar el tipo de usuario en Firestore
                            db.collection("usuarios").document(user.uid)
                                .get()
                                .addOnSuccessListener { document ->
                                    if (document != null && document.exists()) {
                                        val tipo = document.getString("tipo")
                                        if (tipo == "pasajero") {
            val intent = Intent(this, PerfilPasajero::class.java)
            startActivity(intent)
                                            finish()
                                        } else {
                                            // Si no es pasajero, cerrar sesión y mostrar error
                                            auth.signOut()
                                            Toast.makeText(this, "Esta cuenta no corresponde a un pasajero", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Error al verificar tipo de usuario: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        Toast.makeText(this, "Error al iniciar sesión: ${task.exception?.message}",
                            Toast.LENGTH_SHORT).show()
                    }
                }
        }

        new_passwordP.setOnClickListener {
            val intent = Intent(this, RecuperarPasswordP::class.java)
            startActivity(intent)
        }

        cambiarAConductor.setOnClickListener {
            val intent = Intent(this, IniciarSesionC::class.java)
            startActivity(intent)
            finish()
        }
    }
}
