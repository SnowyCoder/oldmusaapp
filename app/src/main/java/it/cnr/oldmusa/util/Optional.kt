package it.cnr.oldmusa.util

sealed class Optional<T> {
    fun asNullable(): T? {
        return when(this) {
            is Some -> data
            is None -> null
        }
    }

    companion object {
        fun<T> ofNullable(x: T?): Optional<T> {
            return x?.let { Some(it) } ?: None()
        }
    }
}
data class Some<T>(val data: T) : Optional<T>()
class None<T> : Optional<T>()
