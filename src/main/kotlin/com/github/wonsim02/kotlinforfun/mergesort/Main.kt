package com.github.wonsim02.kotlinforfun.mergesort

fun main(vararg args: String) {
    val numberOfElements = 10
    val list = (1..numberOfElements).toMutableList()
    list.shuffle()
    MergeSortSequence.logger.debug { "Original list is $list." }

    val mergeSortSequence = MergeSortSequence.toMergeSortSequence(list)!!
    val sortResult: MutableList<Int> = ArrayListWithLogger(ArrayList(numberOfElements))
    mergeSortSequence.toCollection(sortResult)

    assert(sortResult.size == numberOfElements)
    for ((index, element) in sortResult.withIndex()) {
        assert(element == index + 1)
    }
}
