package com.busetas.busetasgo.startpage

import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.busetas.busetasgo.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import android.widget.Spinner
import android.widget.ArrayAdapter
import com.google.android.gms.maps.model.LatLng

class InfomacionBusetaConductor : AppCompatActivity() {
    private lateinit var placaEditText: AppCompatEditText
    private lateinit var colorEditText: AppCompatEditText
    private lateinit var entidadEditText: AppCompatEditText
    private lateinit var capacidadEditText: AppCompatEditText
    private lateinit var rutaSpinner: Spinner
    private lateinit var registrarButton: AppCompatButton
    private lateinit var cerrarSesionButton: AppCompatButton
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var datosRegistrados = false
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var btnCompartirUbicacion: AppCompatButton
    private var compartiendoUbicacion = true
    private var locationCallback: LocationCallback? = null
    private var locationRequest: LocationRequest? = null
    private val rutas = arrayOf("Seleccione una ruta", "Ruta 1", "Ruta 2", "Ruta 3")

    // Mapa de rutas con sus puntos
    private val puntosRuta = mapOf(
        "Ruta 1" to listOf(
            LatLng(4.6097, -74.0817), // Las Quintas
            LatLng(4.6100, -74.0820), // Supermercado Zapatoca
            LatLng(4.6105, -74.0825), // Plaza de Mercado
            LatLng(4.6110, -74.0830), // Almacenes Éxito
            LatLng(4.6115, -74.0835), // Universidad Cundinamarca
            LatLng(4.6120, -74.0840)  // Ecopetrol
        ),
        "Ruta 2" to listOf(
            LatLng(4.6097, -74.0817), // Las Quintas
            LatLng(4.6105, -74.0825), // Plaza de Mercado
            LatLng(4.6110, -74.0830), // Barrio Girardot
            LatLng(4.6115, -74.0835), // San Benito
            LatLng(4.6120, -74.0840), // Parque Santa Rita
            LatLng(4.6125, -74.0845), // Supermercado Metro
            LatLng(4.6130, -74.0850)  // Brasilia
        ),
        "Ruta 3" to listOf(
            LatLng(4.6130, -74.0850), // Brasilia
            LatLng(4.6135, -74.0855), // Clínica Santa Ana
            LatLng(4.6140, -74.0860), // Comando de Policía
            LatLng(4.6145, -74.0865), // Portal de María
            LatLng(4.6150, -74.0870)  // San Benito
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_infomacion_buseta_conductor)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        placaEditText = findViewById(R.id.placa_v)
        colorEditText = findViewById(R.id.color_v)
        entidadEditText = findViewById(R.id.entidad_v)
        capacidadEditText = findViewById(R.id.capacidad_v)
        rutaSpinner = findViewById(R.id.ruta_spinner)
        registrarButton = findViewById(R.id.registrarV)
        cerrarSesionButton = findViewById(R.id.cerrarSesionButton)
        btnCompartirUbicacion = findViewById(R.id.btnCompartirUbicacion)

        verificarInformacionExistente()
        verificarCompatibilidadMapa()

        registrarButton.setOnClickListener {
            obtenerYGuardarInformacionVehiculo()
        }

        cerrarSesionButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, IniciarSesionC::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        btnCompartirUbicacion.setOnClickListener {
            compartiendoUbicacion = !compartiendoUbicacion
            actualizarEstadoBotonUbicacion()
            if (compartiendoUbicacion) {
                iniciarActualizacionesUbicacion()
            } else {
                detenerActualizacionesUbicacion()
                eliminarUbicacionDeFirestore()
            }
        }

        actualizarEstadoBotonUbicacion()

        // Inicializar LocationRequest para actualizaciones periódicas
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L) // cada 10 segundos
            .setMinUpdateIntervalMillis(5000L)
            .build()

        // Inicializar LocationCallback
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (compartiendoUbicacion) {
                    val location = result.lastLocation
                    if (location != null) {
                        actualizarUbicacionEnFirestore(location)
                    }
                }
            }
        }

        // Iniciar actualizaciones automáticas al iniciar la actividad
        iniciarActualizacionesUbicacion()

        // Configurar el Spinner de rutas
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, rutas)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        rutaSpinner.adapter = adapter
    }

    private fun verificarCompatibilidadMapa() {
        val pm = packageManager
        val hasGoogleMaps = pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)
        if (!hasGoogleMaps) {
            Toast.makeText(this, "Su dispositivo no es compatible con Google Maps. Algunas funciones pueden no estar disponibles.", Toast.LENGTH_LONG).show()
        }
    }

    private fun verificarInformacionExistente() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("vehiculos").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        cargarDatosVehiculo(document.data)
                        datosRegistrados = true
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
            val ruta = it["ruta"] as? String ?: ""
            val posicion = rutas.indexOf(ruta)
            if (posicion != -1) {
                rutaSpinner.setSelection(posicion)
            }
        }
    }

    private fun bloquearCampos() {
        placaEditText.isEnabled = false
        colorEditText.isEnabled = false
        entidadEditText.isEnabled = false
        capacidadEditText.isEnabled = false
        rutaSpinner.isEnabled = false
        registrarButton.isEnabled = false
    }

    private fun desbloquearCampos() {
        placaEditText.isEnabled = true
        colorEditText.isEnabled = true
        entidadEditText.isEnabled = true
        capacidadEditText.isEnabled = true
        rutaSpinner.isEnabled = true
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

    private fun actualizarEstadoBotonUbicacion() {
        if (compartiendoUbicacion) {
            btnCompartirUbicacion.text = "Dejar de compartir ubicación"
        } else {
            btnCompartirUbicacion.text = "Reanudar ubicación"
        }
    }

    private fun obtenerYGuardarInformacionVehiculo() {
        if (!compartiendoUbicacion) {
            Toast.makeText(this, "La ubicación está pausada. Reanúdala para registrar.", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val placa = placaEditText.text.toString().trim()
        val color = colorEditText.text.toString().trim()
        val entidad = entidadEditText.text.toString().trim()
        val capacidad = capacidadEditText.text.toString().trim()
        val ruta = rutaSpinner.selectedItem.toString()

        if (placa.isEmpty() || color.isEmpty() || entidad.isEmpty() || capacidad.isEmpty() || ruta == "Seleccione una ruta") {
            Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        // Obtener ubicación actual automáticamente
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).addOnSuccessListener { location ->
            if (location != null) {
                val latitud = location.latitude
                val longitud = location.longitude
                
                // Obtener puntos de la ruta seleccionada
                val puntosRutaSeleccionada = puntosRuta[ruta] ?: emptyList()
                val puntosRutaData = puntosRutaSeleccionada.map { latLng ->
                    mapOf(
                        "lat" to latLng.latitude,
                        "lng" to latLng.longitude
                    )
                }

                val vehiculoData = hashMapOf(
                    "placa" to placa,
                    "color" to color,
                    "entidad" to entidad,
                    "capacidad" to capacidad,
                    "ruta" to ruta,
                    "ubicacion" to hashMapOf(
                        "lat" to latitud,
                        "lng" to longitud
                    ),
                    "puntosRuta" to puntosRutaData,
                    "ultimaActualizacion" to Date()
                )
                db.collection("vehiculos").document(userId)
                    .set(vehiculoData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Información guardada exitosamente", Toast.LENGTH_SHORT).show()
                        datosRegistrados = true
                        bloquearCampos()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error al obtener la ubicación: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun iniciarActualizacionesUbicacion() {
        if (compartiendoUbicacion && locationRequest != null && locationCallback != null) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest!!,
                locationCallback!!,
                mainLooper
            )
        }
    }

    private fun detenerActualizacionesUbicacion() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback!!)
        }
    }

    private fun actualizarUbicacionEnFirestore(location: Location) {
        val userId = auth.currentUser?.uid ?: return
        val vehiculoRef = db.collection("vehiculos").document(userId)
        vehiculoRef.update(
            mapOf(
                "ubicacion" to mapOf(
                    "lat" to location.latitude,
                    "lng" to location.longitude
                ),
                "ultimaActualizacion" to Date()
            )
        )
    }

    private fun eliminarUbicacionDeFirestore() {
        val userId = auth.currentUser?.uid ?: return
        val vehiculoRef = db.collection("vehiculos").document(userId)
        vehiculoRef.update(
            mapOf(
                "ubicacion" to null,
                "pasajeros" to "",
                "ultimaActualizacion" to Date()
            )
        )
    }

    override fun onResume() {
        super.onResume()
        if (datosRegistrados) {
            verificarHoraEdicion()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        detenerActualizacionesUbicacion()
    }
}