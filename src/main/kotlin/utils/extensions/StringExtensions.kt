package utils.extensions

fun String.toLowerCamelCase(): String {
    val sb = StringBuilder(length)
    var hump = false
    forEach { c ->
        when {
            sb.isEmpty() -> if (c != '_') sb.append(c.lowercaseChar())
            c == '_' -> hump = true
            c.isDigit() -> {
                hump = true
                sb.append(c)
            }
            hump -> {
                hump = c.isUpperCase()
                sb.append(c.uppercaseChar())
            }
            else -> sb.append(c)
        }
    }
    return sb.toString()
}

fun String.toSnakeCase(): String = fold(StringBuilder(length)) { acc, c ->
    when {
        c.isUpperCase() || c.isDigit() -> {
            if (acc.isNotEmpty() && acc.last() != '_') acc.append('_')
            acc.append(c.lowercaseChar())
        }
        c != '_' || acc.isNotEmpty() -> acc.append(c)
        else -> acc
    }
}.toString()
