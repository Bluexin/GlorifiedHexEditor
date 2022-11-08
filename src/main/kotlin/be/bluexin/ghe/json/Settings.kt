package be.bluexin.ghe.json

import com.fasterxml.jackson.annotation.JsonIgnore
import org.jetbrains.skiko.currentNanoTime

data class Settings(
    val metadata: String,
    val clientFiles: String?,
    @JsonIgnore
    val nanos: Long = currentNanoTime()
)