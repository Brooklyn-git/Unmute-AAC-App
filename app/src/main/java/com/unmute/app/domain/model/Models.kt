package com.unmute.app.domain.model

enum class ImageType {
    SYMBOL,
    PHOTO,
    EMOJI,
}

/** Sentinel value for "let the system pick the output device". */
object AudioOutputIds {
    const val AUTO = "auto"
}

enum class AppLanguage {
    SYSTEM,
    EN,
    ES,
}

enum class CardFontSize {
    SMALL,
    NORMAL,
    LARGE,
}
