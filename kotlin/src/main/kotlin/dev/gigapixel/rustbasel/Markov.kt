package dev.gigapixel.rustbasel

import kotlin.random.Random

class Markov(
    val fourGrams: List<List<Byte>>,
    val indexLookup: Map<List<Byte>, Int>,
    val cDist: List<List<Pair<Int, Float>>>
) {
    companion object {
        fun transitions(data: String): MutableMap<List<Byte>, MutableMap<List<Byte>, Int>> {
            val map: MutableMap<List<Byte>, MutableMap<List<Byte>, Int>> = mutableMapOf()
            val folded = data.encodeToByteArray().toList().windowed(4, 1).windowed(2, 1).fold(map) { accum, pair ->
                val left = pair[0]
                val right = pair[1]
                val entry = accum[left]
                if (entry == null) {
                    val new = mutableMapOf(Pair(right, 1))
                    accum[left] = new
                    accum
                } else {
                    val existing = entry[right]
                    if (existing != null) {
                        entry[right] = existing + 1
                    } else {
                        entry[right] = 1
                    }
                    accum[left] = entry
                    accum
                }
            }
            return folded

        }

        fun markovFromTransitionStats(transitions: MutableMap<List<Byte>, MutableMap<List<Byte>, Int>>): Markov {
            val allEntries: List<List<Byte>> =
                transitions.toList().flatMap { transition -> transition.second.toList().map { freq -> freq.first } }
                    .distinct()
            val indexLookup = allEntries.zip(allEntries.indices).toMap()


            val transitionProbs: List<List<Pair<Int, Float>>> = allEntries.fold(listOf()) { accum, entry ->
                val map = transitions.getOrElse(entry) { mutableMapOf() }

                val listEntries = map.toList()
                val total = listEntries.sumOf { entry -> entry.second }.toFloat()

                fun mapPair(pair: Pair<List<Byte>, Int>): List<Pair<Int, Float>> {
                    val index = indexLookup[pair.first]
                    if (index != null) {
                        val fullReturn = listOf(Pair(index, pair.second.toFloat() / total))
                        return fullReturn
                    }
                    val emptyReturn: List<Pair<Int, Float>> = listOf()
                    return emptyReturn
                }

                val transitions: List<Pair<Int, Float>> = listEntries.flatMap { pair -> mapPair(pair) }
                val append = listOf(transitions)
                accum + append
            }
            val cDist: List<List<Pair<Int, Float>>> = transitionProbs.map { probs ->
                val init: List<Pair<Int, Float>> = listOf()
                val newProbs: Pair<List<Pair<Int, Float>>, Float> = probs.fold(Pair(init, 0.0F)) { accum, pair ->
                    val newList = accum.first
                    val currentCDist = accum.second
                    Pair(newList + listOf(Pair(pair.first, currentCDist)), currentCDist + pair.second)
                }
                newProbs.first
            }
            return Markov(
                allEntries,
                indexLookup,
                cDist
            )
        }

        fun train(data: String): Markov {
            val stats = transitions(data)
            return markovFromTransitionStats(stats)
        }


    }

    fun generate(init: String, bytes: Int): String {

        val first = init.slice((init.length - 4)..<init.length).encodeToByteArray().toList()
        val firstIdx = this.indexLookup[first]
        if (firstIdx != null) {
            val bytes: List<Byte> = (0..<bytes).fold(Pair(init.encodeToByteArray().toList(), firstIdx)) { accum, _ ->
                val sample = Random.nextFloat()
                val cDist = this.cDist[accum.second]
                val sampled = cDist.filter { pair ->
                    pair.second <= sample
                }.last()
                val next = sampled.first
                val nextFourGram = this.fourGrams[next]
                val nextChar = nextFourGram[3]
                Pair(accum.first + listOf(nextChar), next)
            }.first
            return String(bytes.toByteArray())
        }
        return init


    }
}
