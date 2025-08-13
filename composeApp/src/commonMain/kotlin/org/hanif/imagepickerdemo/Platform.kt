package org.hanif.imagepickerdemo

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform