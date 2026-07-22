package com.omnilabs.omfiles.ui.navigation

object NavRoutes {
    const val HOME = "home"
    const val FILES = "files/{path}"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val RECYCLE = "recycle"
    const val PREVIEW = "preview/{path}"

    fun files(path: String): String = "files/${java.net.URLEncoder.encode(path, "UTF-8")}"
    fun preview(path: String): String = "preview/${java.net.URLEncoder.encode(path, "UTF-8")}"
}
