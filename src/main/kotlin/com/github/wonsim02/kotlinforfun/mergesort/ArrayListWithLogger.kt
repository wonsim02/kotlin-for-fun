package com.github.wonsim02.kotlinforfun.mergesort

class ArrayListWithLogger<T>(
    private val delegate: MutableList<T>
) : MutableList<T> by delegate {

    override fun add(element: T): Boolean {
        MergeSortSequence.logger.debug { "Adding element $element." }
        return delegate.add(element)
    }
}
