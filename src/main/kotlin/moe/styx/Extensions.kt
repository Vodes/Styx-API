package moe.styx

fun Boolean.toInt() = if (this) 1 else 0
fun Int.toBoolean() = this > 0

interface IDatabaseObject {
    abstract fun save(newID: String? = null): Boolean
    abstract fun delete(): Boolean
}