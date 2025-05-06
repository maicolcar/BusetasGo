package com.busetas.busetasgo.conductor

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.busetas.busetasgo.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import android.content.Intent
import com.busetas.busetasgo.startpage.InicioSesionP

class InformacionVehiculoActivity : AppCompatActivity() {
    private lateinit var placaEditText: EditText
    private lateinit var colorEditText: EditText
    private lateinit var entidadEditText: EditText
    private lateinit var capacidadEditText: EditText
    private lateinit var ubicacionEditText: EditText
    private lateinit var registrarButton: Button
    private lateinit var cerrarSesionButton: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var datosRegistrados = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_informacion_vehiculo)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Inicializar vistas
        placaEditText = findViewById(R.id.placaEditText)
        colorEditText = findViewById(R.id.colorEditText)
        entidadEditText = findViewById(R.id.entidadEditText)
        capacidadEditText = findViewById(R.id.capacidadEditText)
        ubicacionEditText = findViewById(R.id.ubicacionEditText)
        registrarButton = findViewById(R.id.registrarButton)
        cerrarSesionButton = findViewById(R.id.cerrarSesionButton)

        // Verificar si ya existe información del vehículo
        verificarInformacionExistente()

        registrarButton.setOnClickListener {
            guardarInformacionVehiculo()
        }

        cerrarSesionButton.setOnClickListener {
            cerrarSesion()
        }
    }

    private fun cerrarSesion() {
        auth.signOut()
        val intent = Intent(this, InicioSesionP::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun verificarInformacionExistente() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("vehiculos").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Cargar datos existentes
                        cargarDatosVehiculo(document.data)
                        datosRegistrados = true
                        // Verificar si se puede editar
                        bloquearCampos()
                        verificarHoraEdicion()
                    }
                }
        }
    }

    private fun cargarDatosVehiculo(datos: Map<String, Any>?) {
        datos?.let {
            placaEditText.setText(it["placa"] as? String ?: "")
            colorEditText.setText(it["color"] as? String ?: "")
            entidadEditText.setText(it["entidad"] as? String ?: "")
            capacidadEditText.setText(it["capacidad"] as? String ?: "")
            ubicacionEditText.setText(it["ubicacion"] as? String ?: "")
        }
    }

    private fun bloquearCampos() {
        placaEditText.isEnabled = false
        colorEditText.isEnabled = false
        entidadEditText.isEnabled = false
        capacidadEditText.isEnabled = false
        ubicacionEditText.isEnabled = false
        registrarButton.isEnabled = false
    }

    private fun desbloquearCampos() {
        placaEditText.isEnabled = true
        colorEditText.isEnabled = true
        entidadEditText.isEnabled = true
        capacidadEditText.isEnabled = true
        ubicacionEditText.isEnabled = true
        registrarButton.isEnabled = true
    }

    private fun verificarHoraEdicion() {
        val calendar = Calendar.getInstance()
        val horaActual = calendar.get(Calendar.HOUR_OF_DAY)
        
        if (datosRegistrados) {
            if (horaActual >= 21) {
                desbloquearCampos()
                Toast.makeText(this, "Puede editar la información", Toast.LENGTH_SHORT).show()
            } else {
                bloquearCampos()
                Toast.makeText(this, "La información solo puede ser modificada después de las 9 PM", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun guardarInformacionVehiculo() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val placa = placaEditText.text.toString().trim()
        val color = colorEditText.text.toString().trim()
        val entidad = entidadEditText.text.toString().trim()
        val capacidad = capacidadEditText.text.toString().trim()
        val ubicacion = ubicacionEditText.text.toString().trim()

        // Validar campos
        if (placa.isEmpty() || color.isEmpty() || entidad.isEmpty() || 
            capacidad.isEmpty() || ubicacion.isEmpty()) {
            Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val vehiculoData = hashMapOf(
            "placa" to placa,
            "color" to color,
            "entidad" to entidad,
            "capacidad" to capacidad,
            "ubicacion" to ubicacion,
            "ultimaActualizacion" to Date()
        )

        db.collection("vehiculos").document(userId)
            .set(vehiculoData)
            .addOnSuccessListener {
                Toast.makeText(this, "Información guardada exitosamente", Toast.LENGTH_SHORT).show()
                datosRegistrados = true
                // Bloquear campos inmediatamente después de guardar
                bloquearCampos()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        // Verificar la hora de edición cada vez que la actividad vuelve a primer plano
        if (datosRegistrados) {
            verificarHoraEdicion()
        }
    }
} 