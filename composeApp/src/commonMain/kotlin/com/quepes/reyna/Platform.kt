package com.quepes.reyna

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform