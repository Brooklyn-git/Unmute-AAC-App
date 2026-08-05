package com.unmute.app.domain.model

/** Bundled core words used as prediction candidates when they aren't on any card. */
object CommonWords {
    val ENGLISH: List<String> = listOf(
        "a", "about", "after", "again", "all", "am", "an", "and", "any", "are",
        "as", "at", "away", "back", "be", "because", "been", "before", "big", "bit",
        "but", "bye", "call", "can", "come", "could", "day", "do", "down", "each",
        "eat", "every", "far", "feel", "few", "find", "for", "from", "get", "give",
        "go", "good", "got", "had", "has", "have", "he", "her", "here", "him",
        "his", "home", "how", "i", "if", "in", "into", "is", "it", "its",
        "just", "know", "like", "little", "long", "look", "love", "make", "many", "may",
        "me", "more", "most", "much", "must", "my", "near", "new", "no", "not",
        "now", "of", "off", "on", "one", "only", "or", "other", "our", "out",
        "over", "own", "please", "put", "say", "school", "see", "she", "should", "small",
        "so", "some", "something", "still", "stop", "such", "take", "tell", "than", "that",
        "the", "their", "them", "then", "there", "these", "they", "thing", "think", "this",
        "those", "through", "time", "to", "today", "too", "two", "up", "us", "use",
        "very", "want", "was", "we", "well", "went", "were", "what", "when", "where",
        "which", "who", "why", "will", "with", "would", "yes", "you", "your",
    )

    val SPANISH: List<String> = listOf(
        "a", "abajo", "agua", "ahí", "ahora", "algo", "allí", "alto", "amigo", "aquí",
        "así", "aunque", "bien", "bueno", "cada", "casa", "casi", "cómo", "con", "contigo",
        "cosas", "cuándo", "cuánto", "cual", "cualquier", "cuando", "dar", "de", "decir", "del",
        "desde", "después", "día", "dice", "donde", "dos", "el", "él", "ella", "ellas",
        "ellos", "en", "entonces", "eres", "es", "ese", "eso", "esos", "esta", "está",
        "estar", "estas", "este", "esto", "estos", "estoy", "fin", "fue", "gente", "gracias",
        "grande", "gusta", "haber", "hacer", "hacia", "hasta", "hay", "he", "hoy", "hola",
        "ir", "irse", "la", "las", "le", "les", "lo", "los", "luego", "mal",
        "más", "me", "mejor", "menos", "mi", "mío", "mucho", "muy", "nada", "ni",
        "no", "nos", "nosotros", "nuestra", "nuestro", "o", "otra", "otro", "para", "pero",
        "poco", "poder", "podemos", "por", "porque", "primero", "puede", "puedes", "qué", "quién",
        "quiere", "quiero", "se", "sea", "ser", "si", "sí", "siento", "sin", "sobre",
        "soy", "su", "sus", "tan", "también", "tampoco", "tener", "tengo", "te", "ti",
        "tiempo", "tiene", "todos", "todo", "tomar", "tu", "tú", "un", "una", "uno",
        "unos", "ustedes", "va", "vamos", "van", "vas", "ve", "ver", "vez", "y",
        "ya", "yo",
    )

    fun forLanguage(language: String): List<String> =
        if (language == "es") SPANISH else ENGLISH
}
