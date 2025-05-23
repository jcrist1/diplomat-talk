extern crate rustbasel;

use std::iter::repeat;

use criterion::{Criterion, black_box, criterion_group, criterion_main};
use diplomat_runtime::DiplomatWrite;
use itertools::Itertools;
use rustbasel::{ffi::MarkovKt, str_magic::NGramStats};
const LIPSUM: &str = include_str!("lipsum.txt");
pub fn spliterator(c: &mut Criterion) {
    use rustbasel::ffi::LineSplitter;

    let ls = LineSplitter::new(LIPSUM.as_bytes());
    let splits = ls.spliterator().splits;

    c.bench_function("ws_split", |b| {
        let mut rng = rand::thread_rng();
        let spaces = " \n\t";
        b.iter(|| {
            let text = (0..(splits.len()))
                .map(|_| rand::random_range(..splits.len()))
                .map(|i| splits[i])
                .join(" ");

            let ls = LineSplitter::new(text.as_bytes());
            ls.spliterator();

            black_box(ls)
        })
    });
}

pub fn markov(c: &mut Criterion) {
    c.bench_function("train_markov", |b| {
        b.iter(|| {
            let markov = MarkovKt::train(LIPSUM.as_bytes());
            black_box(markov)
        })
    });
}

pub fn markov_generate(c: &mut Criterion) {
    let ngram_stats = NGramStats::<4>::new(LIPSUM);
    let mut markov = ngram_stats.markov();

    let mut rng = rand::rng();
    c.bench_function("markov_generate", |b| {
        let rng = &mut rng;
        let markov = &mut markov;
        b.iter(|| {
            let generated = markov.generate(rng, "Lorem ipsum", 4000);
            black_box(generated)
        })
    });
}
criterion_group!(benches, spliterator, markov, markov_generate);
criterion_main!(benches);
