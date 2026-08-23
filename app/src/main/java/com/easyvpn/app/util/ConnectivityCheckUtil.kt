package com.easyvpn.app.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/**
 * The tunnel coming up (GoBackend reporting success) only means the local
 * network interface was created -- it does NOT mean the remote WireGuard
 * peer actually answered a handshake, or that the server can route traffic
 * to the real internet (this is exactly the "connects but no internet" class
 * of bug: subnet collisions, missing FORWARD rules, IP forwarding disabled).
 *
 * This performs a real, executed check rather than trusting the interface
 * state: opens a TCP socket explicitly bound to the VPN's own Network object
 * via Network.bindSocket() -- the officially documented way to force traffic
 * through a specific network (see developer.android.com/develop/connectivity/vpn),
 * needed because Android excludes the VPN app's own traffic from its tunnel
 * by default, so an unbound socket would silently test the real network
 * instead and always report success even when the tunnel is completely dead.
 */
object ConnectivityCheckUtil {

    suspend fun verifyInternetThroughVpn(context: Context, timeoutMs: Int = 10000): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    ?: return@withContext false

                val deadline = System.currentTimeMillis() + timeoutMs

                // GoBackend.setState() returning doesn't mean ConnectivityManager has
                // registered the new VPN network yet -- there's a short OS-level
                // propagation delay. Poll for it instead of checking once, otherwise
                // we bail out near-instantly (false failure) before the tunnel ever
                // had a real chance, which was causing every server in the failover
                // chain to be rejected within ~1 second regardless of timeoutMs.
                var vpnNetwork: android.net.Network? = null
                while (System.currentTimeMillis() < deadline) {
                    vpnNetwork = cm.allNetworks.firstOrNull { network ->
                        cm.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
                    }
                    if (vpnNetwork != null) break
                    delay(200)
                }
                if (vpnNetwork == null) return@withContext false

                val remaining = (deadline - System.currentTimeMillis()).toInt().coerceIn(1000, timeoutMs)
                Socket().use { socket ->
                    vpnNetwork.bindSocket(socket)
                    socket.connect(InetSocketAddress("1.1.1.1", 443), remaining)
                }
                true
            } catch (e: Exception) {
                false
            }
        }
}
