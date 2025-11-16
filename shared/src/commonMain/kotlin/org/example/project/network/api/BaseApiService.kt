package org.example.project.network.api

object BaseApiService {

    fun getBaseUrl(): String {
        // Android 模拟器使用 10.0.2.2 访问主机 localhost
        // Android 真机需要使用开发机器的实际 IP 地址（如 192.168.115.242）
        return "http://192.168.0.107:8080/"
    }

}