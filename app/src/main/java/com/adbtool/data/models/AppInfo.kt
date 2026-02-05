package com.adbtool.data.models

data class AppInfo(
    val packageName: String,
    val appName: String = "",
    val versionName: String = "",
    val versionCode: String = ""
)