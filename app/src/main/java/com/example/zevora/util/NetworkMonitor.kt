package com.example.zevora.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.telephony.TelephonyManager
import com.example.zevora.model.NetworkQuality
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkMonitor(private val context: Context) {

  private val _networkQuality = MutableStateFlow(NetworkQuality.GOOD)
  val networkQuality: StateFlow<NetworkQuality> = _networkQuality.asStateFlow()

  private val _isOffline = MutableStateFlow(false)
  val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

  // Diagnostic override for testing 2G/3G slow network or offline mode as requested in CUJs
  private val _simulationMode = MutableStateFlow<NetworkQuality?>(null)
  val simulationMode: StateFlow<NetworkQuality?> = _simulationMode.asStateFlow()

  private val connectivityManager =
    context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

  private val networkCallback = object : ConnectivityManager.NetworkCallback() {
    override fun onAvailable(network: Network) {
      updateNetworkQuality()
    }

    override fun onLost(network: Network) {
      updateNetworkQuality()
    }

    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
      updateNetworkQuality()
    }
  }

  init {
    try {
      val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()
      connectivityManager?.registerNetworkCallback(request, networkCallback)
      updateNetworkQuality()
    } catch (_: Exception) {
      _networkQuality.value = NetworkQuality.GOOD
      _isOffline.value = false
    }
  }

  private fun updateNetworkQuality() {
    val sim = _simulationMode.value
    if (sim != null) {
      _networkQuality.value = sim
      _isOffline.value = sim == NetworkQuality.OFFLINE
      return
    }

    val cm = connectivityManager ?: run {
      _networkQuality.value = NetworkQuality.GOOD
      _isOffline.value = false
      return
    }

    val activeNetwork = cm.activeNetwork
    if (activeNetwork == null) {
      _networkQuality.value = NetworkQuality.OFFLINE
      _isOffline.value = true
      return
    }

    val capabilities = cm.getNetworkCapabilities(activeNetwork)
    if (capabilities == null || !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
      _networkQuality.value = NetworkQuality.OFFLINE
      _isOffline.value = true
      return
    }

    _isOffline.value = false

    val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    val isCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)

    val downstreamKbps = capabilities.linkDownstreamBandwidthKbps

    when {
      isWifi -> {
        _networkQuality.value = if (downstreamKbps > 15000) NetworkQuality.EXCELLENT else NetworkQuality.GOOD
      }
      isCellular -> {
        // High speed cellular vs low speed cellular
        when {
          downstreamKbps > 20000 -> _networkQuality.value = NetworkQuality.EXCELLENT // 5G
          downstreamKbps > 4000 -> _networkQuality.value = NetworkQuality.GOOD // 4G LTE
          else -> _networkQuality.value = NetworkQuality.SLOW // 2G/3G slow network
        }
      }
      else -> {
        _networkQuality.value = NetworkQuality.GOOD
      }
    }
  }

  fun setSimulatedNetwork(quality: NetworkQuality?) {
    _simulationMode.value = quality
    updateNetworkQuality()
  }
}
