package com.omnilabs.omfiles.ui.navigation

object NavRoutes {
    const val HOME = "home"
    const val FILES = "files/{path}"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val RECYCLE = "recycle"

    fun files(path: String): String = "files/${java.net.URLEncoder.encode(path, "UTF-8")}"
}
