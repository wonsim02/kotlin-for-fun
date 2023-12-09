package com.github.wonsim02.kotlinforfun.mergesort

import mu.KotlinLogging

sealed class MergeSortSequence<T : Comparable<T>> : Sequence<T> {

    abstract val context: MergeSortContext<T>
    abstract val range: Pair<Int, Int>

    protected abstract fun sortedIterator(instance: MergeSortContext.Instance<T>): Iterator<T>
    final override fun iterator(): Iterator<T> = sortedIterator(context.newInstance())

    final override fun toString(): String {
        return "MergeSortSequence(range=$range)"
    }

    class Singleton<T : Comparable<T>>(
        override val context: MergeSortContext<T>,
        val index: Int,
    ) : MergeSortSequence<T>() {

        override val range = index to index + 1

        override fun sortedIterator(
            instance: MergeSortContext.Instance<T>,
        ): Iterator<T> = object : IteratorWithLogging<T>() {

            override val objectName = this@Singleton.toString()
            override fun hasNext(): Boolean = !instance.consumed(index)
            override fun nextWithoutLogging(): T = instance[index]
        }
    }

    class Merged<T : Comparable<T>>(
        override val context: MergeSortContext<T>,
        val left: MergeSortSequence<T>,
        val right: MergeSortSequence<T>,
        from: Int,
        until: Int,
    ) : MergeSortSequence<T>() {

        override val range = from to until

        override fun sortedIterator(
            instance: MergeSortContext.Instance<T>,
        ): Iterator<T> = object : IteratorWithLogging<T>() {

            private val leftIterator: BufferedIterator<T> = BufferedIterator(left.sortedIterator(instance))
            private val rightIterator: BufferedIterator<T> = BufferedIterator(right.sortedIterator(instance))
            override val objectName = this@Merged.toString()

            override fun hasNext(): Boolean = leftIterator.hasNext() || rightIterator.hasNext()

            override fun nextWithoutLogging(): T {
                return if (!hasNext()) {
                    throw NoSuchElementException()
                } else if (!leftIterator.hasNext()) {
                    rightIterator.next()
                } else if (!rightIterator.hasNext()) {
                    leftIterator.next()
                } else if (leftIterator.previewNext() <= rightIterator.previewNext()) {
                    leftIterator.next()
                } else {
                    rightIterator.next()
                }
            }
        }
    }

    private abstract class IteratorWithLogging<T> : Iterator<T> {

        abstract val objectName: String
        abstract fun nextWithoutLogging(): T

        final override fun next(): T {
            logger.debug { "Calling $objectName.next()." }
            val emitted = nextWithoutLogging()
            logger.debug { "$objectName.next() returned $emitted." }
            return emitted
        }
    }

    companion object {

        val logger = KotlinLogging.logger { }

        fun <T : Comparable<T>> toMergeSortSequence(list: List<T>): MergeSortSequence<T>? {
            return recursiveToMergeSortSequence(
                context = MergeSortContext(list),
                fromIndex = 0,
                untilIndex = list.size,
            )
        }

        private fun <T : Comparable<T>> recursiveToMergeSortSequence(
            context: MergeSortContext<T>,
            fromIndex: Int,
            untilIndex: Int,
        ): MergeSortSequence<T>? {
            return if (fromIndex >= untilIndex) {
                null
            } else if (fromIndex == untilIndex - 1) {
                Singleton(context, fromIndex)
            } else {
                val middleIndex = (fromIndex + untilIndex) / 2
                val leftSequence = recursiveToMergeSortSequence(context, fromIndex, middleIndex)
                val rightSequence = recursiveToMergeSortSequence(context, middleIndex, untilIndex)

                if (leftSequence == null) {
                    rightSequence
                } else if (rightSequence == null) {
                    leftSequence
                } else {
                    Merged(context, leftSequence, rightSequence, fromIndex, untilIndex)
                }
            }
        }
    }
}
