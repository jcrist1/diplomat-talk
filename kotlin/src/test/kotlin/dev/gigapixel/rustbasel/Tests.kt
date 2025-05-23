package dev.gigapixel.rustbasel

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class Tests {

    @Test
    fun test_splits() {
        val str = "ab\nde\nfg"
        val splits = Indices.splitNewlines(str).getIndices()
        val idxSplits = splits.asList().chunked(2).map { pair ->
            str.slice(
                pair[0].toInt() ..< pair[1].toInt()
            )
        }
        val kSplit = str.split("\n")
        assertEquals(kSplit, idxSplits)

        val strSpliterator = LineSplitter.new_(str).spliterator().asSequence().toList()

        assertEquals(kSplit, strSpliterator)


    }
}