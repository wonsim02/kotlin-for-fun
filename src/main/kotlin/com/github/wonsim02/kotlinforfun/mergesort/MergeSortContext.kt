package com.github.wonsim02.kotlinforfun.mergesort

class MergeSortContext<T : Comparable<T>>(private val list: List<T>) {

    fun newInstance(): Instance<T> = Instance(list)

    class Instance<T : Comparable<T>>(private val _list: List<T>) {

        private val _consumed: MutableList<BooleanWrapper> = MutableList(_list.size) { BooleanWrapper(false) }

        fun consumed(index: Int): Boolean = _consumed[index].value

        operator fun get(index: Int): T {
            val wrapper = _consumed[index]
            return synchronized(wrapper) {
                if (wrapper.value) throw NoSuchElementException()
                _list[index].also { wrapper.value = true }
            }
        }
    }

    private class BooleanWrapper(var value: Boolean)
}
