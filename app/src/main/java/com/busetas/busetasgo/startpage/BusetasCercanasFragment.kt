package com.busetas.busetasgo.startpage

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.busetas.busetasgo.R
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import android.widget.SearchView
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import android.app.AlertDialog
import kotlin.math.roundToInt
import com.google.firebase.auth.FirebaseAuth

class BusetasCercanasFragment : Fragment(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val LOCATION_PERMISSION_REQUEST = 1
    private val PLAY_SERVICES_RESOLUTION_REQUEST = 9000
    private var locationUpdateState = false
    private var lastKnownLocation: Location? = null
    private var mapFragment: SupportMapFragment? = null
    private var conductorMarkers: MutableMap<String, Marker> = mutableMapOf()
    private lateinit var placesClient: PlacesClient
    private var isUserInteracting = false
    private var isFirstLocationUpdate = true
    private var busetaTomadaId: String? = null
    private var busetaTomadaLatLng: LatLng? = null
    private var mostroMensajeAutoSalida = false
    private var mostroNotificacion10m = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_busetas_cercanas, container, false)
        
        // Inicializar Places API
        Places.initialize(requireContext(), "TU_API_KEY") // Reemplaza con tu API key
        placesClient = Places.createClient(requireContext())

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        if (!checkPlayServices()) {
            Toast.makeText(context, "Este dispositivo no soporta Google Play Services", Toast.LENGTH_LONG).show()
            return
        }

        try {
            // Configurar botones de navegación
            view.findViewById<AppCompatButton>(R.id.home).setOnClickListener {
                activity?.let { act ->
                    val intent = Intent(act, PerfilPasajero::class.java)
                    startActivity(intent)
                    act.finish()
                }
            }

            // Configurar la barra de búsqueda
            val searchView = view.findViewById<SearchView>(R.id.searchView)
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    buscarUbicacion(query)
                    return true
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    return false
                }
            })

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
            
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    for (location in locationResult.locations) {
                        lastKnownLocation = location
                        updateMapLocation(location)
                        verificarDistanciaABuseta(location)
                    }
                }
            }
            
            // Inicializar el mapa de forma segura
            initializeMap()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al inicializar el mapa: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeMap() {
        try {
            mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
            if (mapFragment == null) {
                mapFragment = SupportMapFragment.newInstance()
                childFragmentManager.beginTransaction()
                    .replace(R.id.map, mapFragment!!)
                    .commit()
            }
            mapFragment?.getMapAsync(this)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al inicializar el mapa: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkPlayServices(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(requireContext())
        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(requireActivity(), resultCode,
                    PLAY_SERVICES_RESOLUTION_REQUEST)?.show()
            }
            return false
        }
        return true
    }

    private fun updateMapLocation(location: Location) {
        try {
            mMap?.let { map ->
                if (isFirstLocationUpdate) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f))
                    isFirstLocationUpdate = false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al actualizar la ubicación en el mapa", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        try {
            mMap = googleMap

            // Eliminar el estilo personalizado para evitar incompatibilidades
            // context?.let { ctx ->
            //     try {
            //         val success = googleMap.setMapStyle(
            //             MapStyleOptions.loadRawResourceStyle(ctx, R.raw.map_style)
            //         )
            //         if (!success) {
            //             Toast.makeText(ctx, "Error al cargar el estilo del mapa", Toast.LENGTH_SHORT).show()
            //         }
            //     } catch (e: Exception) {
            //         e.printStackTrace()
            //     }
            // }

            mMap?.mapType = GoogleMap.MAP_TYPE_NORMAL

            // Configurar los controles del mapa
            mMap?.uiSettings?.apply {
                isZoomControlsEnabled = true
                isCompassEnabled = true
                isMyLocationButtonEnabled = true
                isMapToolbarEnabled = true
                isZoomGesturesEnabled = true
                isScrollGesturesEnabled = true
                isRotateGesturesEnabled = true
                isTiltGesturesEnabled = true
            }

            // Configurar ubicación inicial por defecto (Bogotá)
            val defaultLocation = LatLng(4.6097, -74.0817)
            mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f))

            // Verificar y solicitar permisos si es necesario
            if (!checkPermissions()) {
                requestPermissions()
            } else {
                enableMyLocation()
            }

            // Escuchar ubicaciones de conductores en tiempo real
            escucharUbicacionesConductores()

            // Adaptador personalizado para la ventana de información
            mMap?.setInfoWindowAdapter(BusetaInfoWindowAdapter())

            // Agregar listener para detectar interacción del usuario
            mMap?.setOnCameraMoveStartedListener { reason ->
                if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                    isUserInteracting = true
                }
            }

            mMap?.setOnCameraIdleListener {
                isUserInteracting = false
            }

            mMap?.setOnMyLocationButtonClickListener {
                lastKnownLocation?.let { location ->
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f))
                }
                true
            }

            // Agregar listener para cuando se presiona un marcador
            mMap?.setOnMarkerClickListener { marker ->
                if (marker.title == "Buseta") {
                    lastKnownLocation?.let { userLocation ->
                        val busetaLocation = Location("").apply {
                            latitude = marker.position.latitude
                            longitude = marker.position.longitude
                        }
                        val distance = userLocation.distanceTo(busetaLocation)
                        
                        if (distance <= 4000) { // 10 metros
                            mostrarDialogoTomarBuseta(marker)
                            true
                        } else {
                            Toast.makeText(context, "La buseta está demasiado lejos", Toast.LENGTH_SHORT).show()
                            false
                        }
                    } ?: run {
                        Toast.makeText(context, "No se puede determinar tu ubicación", Toast.LENGTH_SHORT).show()
                        false
                    }
                } else {
                    false
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al configurar el mapa: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissions(): Boolean {
        return context?.let { ctx ->
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } ?: false
    }

    private fun requestPermissions() {
        requestPermissions(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST
        )
    }

    private fun startLocationUpdates() {
        if (checkPermissions()) {
            try {
                val locationRequest = LocationRequest.Builder(3000)
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .setWaitForAccurateLocation(false)
                    .setMinUpdateIntervalMillis(2000)
                    .setMaxUpdateDelayMillis(5000)
                    .build()

                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
                locationUpdateState = true

                // Obtener ubicación inmediatamente
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        location?.let {
                            lastKnownLocation = it
                            updateMapLocation(it)
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            context,
                            "Error al obtener la ubicación: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            } catch (e: SecurityException) {
                e.printStackTrace()
                Toast.makeText(
                    context,
                    "Error al obtener la ubicación: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun enableMyLocation() {
        if (checkPermissions()) {
            try {
                mMap?.isMyLocationEnabled = true
                startLocationUpdates()
            } catch (e: SecurityException) {
                e.printStackTrace()
                Toast.makeText(
                    context,
                    "Error al habilitar la ubicación: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            requestPermissions()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && 
                    grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    enableMyLocation()
                } else {
                    Toast.makeText(
                        context,
                        "La aplicación necesita permisos de ubicación para mostrar tu posición",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!locationUpdateState && checkPermissions()) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            locationUpdateState = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            locationUpdateState = false
            mMap = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun escucharUbicacionesConductores() {
        val db = FirebaseFirestore.getInstance()
        db.collection("vehiculos")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(context, "Error al escuchar ubicaciones: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    val idsConUbicacion = mutableSetOf<String>()
                    for (doc in snapshots.documents) {
                        val ubicacion = doc.get("ubicacion") as? Map<*, *>
                        if (ubicacion == null) continue // No mostrar marcador si no hay ubicación
                        val id = doc.id
                        idsConUbicacion.add(id)
                        val lat = (ubicacion["lat"] as? Double) ?: continue
                        val lng = (ubicacion["lng"] as? Double) ?: continue
                        val posicion = LatLng(lat, lng)

                        // Obtén los datos del conductor
                        val placa = doc.getString("placa") ?: "Sin placa"
                        val color = doc.getString("color") ?: "Sin color"
                        val entidad = doc.getString("entidad") ?: "Sin entidad"
                        val capacidad = doc.getString("capacidad") ?: "Sin capacidad"
                        val ruta = doc.getString("ruta") ?: "Sin ruta"
                        val inforuta = when (ruta) {
                            "Ruta 1" -> """
                            RUTA 1
                            Las Quintas
                            Supermercado Zapatoca
                            Plaza de Mercado
                            Almacenes Éxito
                            Universidad Cundinamarca
                            Ecopetrol
                        """.trimIndent()
                            "Ruta 2" -> """
                            RUTA 2
                            Las Quintas
                            Plaza de Mercado
                            Barrio Girardot
                            San Benito
                            Parque Santa Rita
                            Supermercado Metro
                            Brasilia
                        """.trimIndent()
                            "Ruta 3" -> """
                            RUTA 3
                            Brasilia
                            Clínica Santa Ana
                            Comando de Policía
                            Portal de María
                            San Benito
                        """.trimIndent()
                            else -> "Sin información de ruta"
                        }
                        val info = "Placa: $placa\nColor: $color\nEntidad: $entidad\nCapacidad: $capacidad\nruta: $inforuta"


                        // Actualiza o crea el marcador
                        if (conductorMarkers[id] == null) {
                            conductorMarkers[id] = mMap?.addMarker(
                                MarkerOptions()
                                    .position(posicion)
                                    .title("Buseta")
                                    .snippet(info)
                            )!!
                        } else {
                            conductorMarkers[id]?.position = posicion
                            conductorMarkers[id]?.snippet = info
                        }
                    }
                    // Eliminar marcadores de busetas que ya no tienen ubicación
                    val idsAEliminar = conductorMarkers.keys - idsConUbicacion
                    for (id in idsAEliminar) {
                        conductorMarkers[id]?.remove()
                        conductorMarkers.remove(id)
                    }
                }
            }
    }

    // Adaptador personalizado para la ventana de información
    inner class BusetaInfoWindowAdapter : GoogleMap.InfoWindowAdapter {
        override fun getInfoWindow(marker: Marker): View? {
            return null // Usar el diseño por defecto del marco
        }
        override fun getInfoContents(marker: Marker): View? {
            val context = requireContext()
            val view = View.inflate(context, R.layout.info_window_buseta, null)
            val title = view.findViewById<android.widget.TextView>(R.id.info_title)
            val snippet = view.findViewById<android.widget.TextView>(R.id.info_snippet)
            title.text = marker.title
            snippet.text = marker.snippet
            return view
        }
    }

    private fun buscarUbicacion(location: String) {
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(location)
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response: FindAutocompletePredictionsResponse ->
                if (response.autocompletePredictions.isNotEmpty()) {
                    val prediction = response.autocompletePredictions[0]
                    val placeId = prediction.placeId
                    
                    val placeFields = listOf(Place.Field.LAT_LNG)
                    val fetchRequest = FetchPlaceRequest.builder(placeId, placeFields).build()

                    placesClient.fetchPlace(fetchRequest)
                        .addOnSuccessListener { fetchResponse: FetchPlaceResponse ->
                            val place = fetchResponse.place
                            val latLng = place.latLng
                            
                            if (latLng != null) {
                                // Mover la cámara a la ubicación encontrada
                                mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                                
                                // Agregar un marcador en la ubicación
                                mMap?.addMarker(
                                    MarkerOptions()
                                        .position(latLng)
                                        .title(location)
                                )
                            }
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(context, "Error al obtener la ubicación: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(context, "Ubicación no encontrada", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Error al buscar ubicación: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarDialogoTomarBuseta(marker: Marker) {
        val conductorId = conductorMarkers.entries.find { it.value == marker }?.key
        
        if (conductorId != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("vehiculos").document(conductorId)
                .get()
                .addOnSuccessListener { document ->
                    val capacidadActual = document.getString("capacidad")?.toIntOrNull() ?: 0
                    val placa = document.getString("placa") ?: "Sin placa"
                    val color = document.getString("color") ?: "Sin color"
                    val entidad = document.getString("entidad") ?: "Sin entidad"
                    val ruta = document.getString("ruta") ?: "Sin ruta"
                    val pasajeros = document.getString("pasajeros")?.split(",")?.filter { it.isNotEmpty() } ?: listOf()
                    val userId = FirebaseAuth.getInstance().currentUser?.uid

                    // Obtener la información detallada de la ruta
                    val infoRuta = when (ruta) {
                        "Ruta 1" -> """
                            RUTA 1
                            Las Quintas
                            Supermercado Zapatoca
                            Plaza de Mercado
                            Almacenes Éxito
                            Universidad Cundinamarca
                            Ecopetrol
                        """.trimIndent()
                        "Ruta 2" -> """
                            RUTA 2
                            Las Quintas
                            Plaza de Mercado
                            Barrio Girardot
                            San Benito
                            Parque Santa Rita
                            Supermercado Metro
                            Brasilia
                        """.trimIndent()
                        "Ruta 3" -> """
                            RUTA 3
                            Brasilia
                            Clínica Santa Ana
                            Comando de Policía
                            Portal de María
                            San Benito
                        """.trimIndent()
                        else -> "Sin información de ruta"
                    }

                    if (userId != null && pasajeros.contains(userId)) {
                        val builder = AlertDialog.Builder(requireContext())
                        builder.setTitle("Información de la Buseta")
                            .setMessage("""
                                Placa: $placa
                                Color: $color
                                Entidad: $entidad
                                Cupos disponibles: $capacidadActual
                                Estado: Ya has tomado esta buseta
                                
                                $infoRuta
                            """.trimIndent())
                            .setPositiveButton("Dejar Buseta") { dialog, _ ->
                                dejarBusetaManual(conductorId)
                                dialog.dismiss()
                            }
                            .setNegativeButton("Cerrar") { dialog, _ ->
                                dialog.dismiss()
                            }
                            .setCancelable(true)
                        
                        builder.create().show()
                    } else {
                        val builder = AlertDialog.Builder(requireContext())
                        builder.setTitle("Tomar esta buseta?")
                            .setMessage("""
                                Placa: $placa
                                Color: $color
                                Entidad: $entidad
                                Cupos disponibles: $capacidadActual
                                
                                $infoRuta
                                
                                ¿Deseas tomar esta buseta?
                            """.trimIndent())
                            .setPositiveButton("Sí") { dialog, _ ->
                                if (capacidadActual > 0) {
                                    val nuevosPasajeros = pasajeros.toMutableList()
                                    userId?.let { nuevosPasajeros.add(it) }
                                    
                                    db.collection("vehiculos").document(conductorId)
                                        .update(
                                            "capacidad", (capacidadActual - 1).toString(),
                                            "pasajeros", nuevosPasajeros.joinToString(",")
                                        )
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "Has tomado la buseta. Cupo actualizado", Toast.LENGTH_SHORT).show()
                                            val ubicacion = document.get("ubicacion") as? Map<*, *>
                                            val lat = (ubicacion?.get("lat") as? Double) ?: 0.0
                                            val lng = (ubicacion?.get("lng") as? Double) ?: 0.0
                                            guardarBusetaTomada(conductorId, LatLng(lat, lng))
                                            val info = marker.snippet?.split("\n")?.toMutableList()
                                            info?.let {
                                                val capacidadIndex = it.indexOfFirst { line -> line.startsWith("Capacidad:") }
                                                if (capacidadIndex != -1) {
                                                    it[capacidadIndex] = "Capacidad: ${capacidadActual - 1}"
                                                    marker.snippet = it.joinToString("\n")
                                                }
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(context, "Error al actualizar el cupo: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                } else {
                                    Toast.makeText(context, "No hay cupos disponibles", Toast.LENGTH_SHORT).show()
                                }
                                dialog.dismiss()
                            }
                            .setNegativeButton("No") { dialog, _ ->
                                dialog.dismiss()
                            }
                            .setCancelable(true)
                        
                        builder.create().show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error al obtener datos de la buseta: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun guardarBusetaTomada(id: String, latLng: LatLng) {
        busetaTomadaId = id
        busetaTomadaLatLng = latLng
        mostroNotificacion10m = false
    }

    private fun verificarDistanciaABuseta(location: Location) {
        val busetaId = busetaTomadaId
        val busetaLatLng = busetaTomadaLatLng
        if (busetaId != null && busetaLatLng != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                location.latitude, location.longitude,
                busetaLatLng.latitude, busetaLatLng.longitude,
                results
            )
            val distancia = results[0]
            
            // Mostrar notificación cuando esté a 10 metros
            if (distancia <= 20 && !mostroNotificacion10m) {
                mostrarNotificacion10Metros()
                mostroNotificacion10m = true
            }
            
            // Si se aleja más de 4020 metros, dejar la buseta
            if (distancia > 4020) {
                dejarBusetaAutomaticamente(busetaId)
                busetaTomadaId = null
                busetaTomadaLatLng = null
                mostroNotificacion10m = false
            }
        }
    }

    private fun mostrarNotificacion10Metros() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("¡Buseta Cercana!")
            .setMessage("La buseta está a 10 metros de tu ubicación.")
            .setPositiveButton("Aceptar") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
        
        builder.create().show()
    }

    private fun dejarBusetaAutomaticamente(busetaId: String) {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("vehiculos").document(busetaId)
            .get()
            .addOnSuccessListener { document ->
                val pasajeros = document.getString("pasajeros")?.split(",")?.filter { it.isNotEmpty() }?.toMutableList() ?: mutableListOf()
                val capacidadActual = document.getString("capacidad")?.toIntOrNull() ?: 0
                if (pasajeros.contains(userId)) {
                    pasajeros.remove(userId)
                    db.collection("vehiculos").document(busetaId)
                        .update(
                            "capacidad", (capacidadActual + 1).toString(),
                            "pasajeros", pasajeros.joinToString(",")
                        ).addOnSuccessListener {
                            if (!mostroMensajeAutoSalida) {
                                Toast.makeText(context, "Te has alejado de la buseta y has dejado tu cupo automáticamente.", Toast.LENGTH_LONG).show()
                                mostroMensajeAutoSalida = true
                            }
                        }
                }
            }
    }

    private fun dejarBusetaManual(busetaId: String) {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("vehiculos").document(busetaId)
            .get()
            .addOnSuccessListener { document ->
                val pasajeros = document.getString("pasajeros")?.split(",")?.filter { it.isNotEmpty() }?.toMutableList() ?: mutableListOf()
                val capacidadActual = document.getString("capacidad")?.toIntOrNull() ?: 0
                if (pasajeros.contains(userId)) {
                    pasajeros.remove(userId)
                    db.collection("vehiculos").document(busetaId)
                        .update(
                            "capacidad", (capacidadActual + 1).toString(),
                            "pasajeros", pasajeros.joinToString(",")
                        ).addOnSuccessListener {
                            Toast.makeText(context, "Has dejado la buseta.", Toast.LENGTH_SHORT).show()
                            busetaTomadaId = null
                            busetaTomadaLatLng = null
                            mostroMensajeAutoSalida = false
                        }
                }
            }
    }
} 
