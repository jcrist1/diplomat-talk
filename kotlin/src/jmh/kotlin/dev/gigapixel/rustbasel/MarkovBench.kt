package dev.gigapixel.rustbasel
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import kotlin.random.Random
import kotlin.random.nextInt

@State(Scope.Benchmark)
internal open class MarkovBench {
    val data = SplitBenchmark.testString

    @Benchmark
    fun benchTrainRs(bh: Blackhole) {
        val markovKt = MarkovKt.train(data);
        bh.consume(markovKt)
    }

    val markovKt = MarkovKt.train(data);
    @Benchmark
    fun benchGenerateRs(bh: Blackhole) {
        val generated = markovKt.generate("Lorem ipsum", 4000)
        bh.consume(generated)
    }

    @Benchmark
    fun benchTrainKt(bh: Blackhole) {
        val markov = Markov.train(data);
        bh.consume(markov)
    }

    val markov = Markov.train(data);
    @Benchmark
    fun benchGenerateKt(bh: Blackhole) {
        val generated = markov.generate("Lorem ipsum", 4000)
        bh.consume(generated)
    }
}