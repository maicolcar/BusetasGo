package com.busetas.busetasgo.startpage

import android.content.Intent
import android.os.Bundle
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

class IniciarSesionC : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_iniciar_sesion_c)
        
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val correo = findViewById<AppCompatEditText>(R.id.correoC)
        val password = findViewById<AppCompatEditText>(R.id.passwordC)
        val crearCuenta_C = findViewById<AppCompatButton>(R.id.crearCuenta_C)
        val inicio_sesionC = findViewById<AppCompatButton>(R.id.inicio_sesionC)
        val new_passwordC = findViewById<AppCompatTextView>(R.id.new_passwordC)
        val cambiarAPasajero = findViewById<AppCompatButton>(R.id.cambiarAPasajero)

        crearCuenta_C.setOnClickListener {
            val intent = Intent(this, CrearCuentaC::class.java)
            startActivity(intent)
        }

        inicio_sesionC.setOnClickListener {
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
                                        if (tipo == "conductor") {
            val intent = Intent(this, InfomacionBusetaConductor::class.java)
            startActivity(intent)
                                            finish()
                                        } else {
                                            // Si no es conductor, cerrar sesión y mostrar error
                                            auth.signOut()
                                            Toast.makeText(this, "Esta cuenta no corresponde a un conductor", Toast.LENGTH_SHORT).show()
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

        new_passwordC.setOnClickListener {
            val intent = Intent(this, RecuperarPasswordC::class.java)
            startActivity(intent)
        }

        cambiarAPasajero.setOnClickListener {
            val intent = Intent(this, InicioSesionP::class.java)
            startActivity(intent)
            finish()
        }
    }
}