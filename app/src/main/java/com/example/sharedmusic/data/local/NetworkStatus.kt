package com.example.sharedmusic.data.local

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Проверка текущего типа сети.
 *
 * Нужна настройке «скачивать только по Wi-Fi»: загрузка трека комнаты
 * в мобильной сети не начинается, чтобы не тратить трафик пользователя.
 * Разрешение ACCESS_NETWORK_STATE уже объявлено в манифесте.
 */
object NetworkStatus {

    /** true, если активное соединение безлимитное (Wi-Fi или Ethernet) */
    fun isUnmetered(context: Context): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as? ConnectivityManager ?: return false

        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}
