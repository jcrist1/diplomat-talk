package dev.gigapixel.rustbasel

import org.junit.jupiter.api.Test

class TestMarkov {
    val data = TestMarkovKt.data

    @Test
    fun testMarkov() {
        val markov = Markov.train(data)
        println(markov.generate("Lorem ipsum", 300))

    }
}