package com.github.wonsim02.kotlinforfun.mergesort

class BufferedIterator<T : Any>(private val delegate: Iterator<T>) : Iterator<T> {

    private var buffer: T? = null

    fun previewNext(): T {
        if (!hasNext()) throw NoSuchElementException()
        return buffer
            ?: delegate.next().also { buffer = it }
    }

    override fun hasNext(): Boolean = buffer != null || delegate.hasNext()

    override fun next(): T {
        return buffer
            ?.also { buffer = null }
            ?: delegate.next()
    }
}
