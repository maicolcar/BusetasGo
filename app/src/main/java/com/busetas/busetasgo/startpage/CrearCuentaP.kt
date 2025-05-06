package com.busetas.busetasgo.startpage

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.busetas.busetasgo.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CrearCuentaP : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_crear_cuenta_p)
        
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val nombreP = findViewById<AppCompatEditText>(R.id.nombreP)
        val correoP = findViewById<AppCompatEditText>(R.id.correoP)
        val passwordP = findViewById<AppCompatEditText>(R.id.passwordP)
        val spinnerDia: Spinner = findViewById(R.id.diaP)
        val spinnerMes: Spinner = findViewById(R.id.mesP)
        val spinnerYear: Spinner = findViewById(R.id.yearP)

        val dias = (1..31).map { it.toString() }
        val meses = listOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )
        val anios = (1940..2025).map { it.toString() }

        spinnerDia.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, dias)
        spinnerMes.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, meses)
        spinnerYear.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, anios)

        val registrarP = findViewById<AppCompatButton>(R.id.registrarP)
        registrarP.setOnClickListener {
            val nombre = nombreP.text.toString()
            val email = correoP.text.toString()
            val password = passwordP.text.toString()
            val fechaNacimiento = "${spinnerDia.selectedItem} de ${spinnerMes.selectedItem} de ${spinnerYear.selectedItem}"

            if (nombre.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val userId = user?.uid

                        if (userId != null) {
                            val userData = hashMapOf(
                                "nombre" to nombre,
                                "email" to email,
                                "fechaNacimiento" to fechaNacimiento,
                                "tipo" to "pasajero"
                            )

                            db.collection("usuarios").document(userId)
                                .set(userData)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Cuenta creada exitosamente", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, PerfilPasajero::class.java)
            startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Error al guardar datos: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        Toast.makeText(this, "Error al crear cuenta: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
        }
        }
    }
}