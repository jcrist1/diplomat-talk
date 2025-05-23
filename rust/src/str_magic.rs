use std::{borrow::Cow, iter::once};

use itertools::Itertools;
use rand::{Rng, distr::Uniform};
use rustc_hash::FxHashMap;

pub struct NGramStats<const N: usize> {
    transition: FxHashMap<[u8; N], FxHashMap<[u8; N], usize>>,
}
pub type Markov4 = Markov<4>;

#[derive(Debug, Clone)]
pub struct Markov<const N: usize> {
    index_lookup: FxHashMap<[u8; N], usize>,
    pub indexes: Vec<[u8; N]>,
    cdist: Vec<Vec<(usize, f32)>>,
    #[allow(unused)]
    probs: Vec<Vec<(usize, f32)>>,
}

impl<const N: usize> NGramStats<N> {
    pub fn new(input: &str) -> Self {
        let mut transition = FxHashMap::<[u8; N], FxHashMap<[u8; N], usize>>::default();

        let mut l = [0xA; N];
        let mut r = [0xA; N];
        for (left, right) in input.as_bytes().windows(N).tuple_windows::<(_, _)>() {
            l.as_mut_slice().copy_from_slice(left);
            if right.len() < N {
                break;
            }
            r.as_mut_slice().copy_from_slice(right);
            let entry = transition.entry(l);
            let hm = entry.or_insert_with(|| [(r, 0)].into_iter().collect());
            *hm.entry(r).or_insert(0) += 1;
        }
        NGramStats { transition }
    }

    pub fn markov(&self) -> Markov<N> {
        let indexes = self
            .transition
            .iter()
            .flat_map(|(st, hm)| once(*st).chain(hm.iter().map(|(e, _)| *e)))
            .unique()
            .collect_vec();
        let hash_index = indexes
            .iter()
            .enumerate()
            .map(|(i, ar)| (*ar, i))
            .collect::<FxHashMap<_, _>>();
        let (probs, cum_dist) = indexes
            .iter()
            .map(|l| {
                let hash_index = &hash_index;
                let Some(t) = self.transition.get(l) else {
                    return (Vec::new(), Vec::new());
                };
                let total: usize = t.iter().map(|(_, v)| *v).sum();
                let probs = t
                    .iter()
                    .map(move |(r, f)| {
                        let col_idx = hash_index
                            .get(r)
                            .expect("Failed to get trigram index for col");
                        (*col_idx, *f as f32 / total as f32)
                    })
                    .collect_vec();
                let cum_probs = probs
                    .iter()
                    .scan(0f32, |state, (idx, p)| {
                        let c_prob = *state;
                        *state += p;
                        Some((*idx, c_prob))
                    })
                    .collect_vec();
                (probs, cum_probs)
            })
            .unzip::<_, _, Vec<_>, Vec<_>>();
        let degenerate = cum_dist.iter().filter(|row| row.is_empty()).count();

        Markov {
            index_lookup: hash_index,
            indexes,
            probs,
            cdist: cum_dist,
        }
    }
}

impl<const N: usize> Markov<N> {
    pub fn generate<'a, R: Rng>(
        &self,
        rng: &mut R,
        start: &'a str,
        steps: usize,
    ) -> Result<Cow<'a, str>, ()> {
        let distr = Uniform::new(0., 1.0).map_err(|_| ())?;
        let mut init = [0xAu8; N];
        let return_str = Cow::from(start);
        let init_len = start.len();
        if init_len < N {
            // this could be unsafe
            return Ok(return_str);
        }
        init.copy_from_slice(&start.as_bytes()[(init_len - N)..]);

        let Some(init_idx) = self.index_lookup.get(&init) else {
            // Should we error?
            eprintln!(
                "Ngram {init:?} not found in ngram lookup {:#?}",
                self.index_lookup
            );
            return Ok(return_str);
        };
        let mut return_str = match return_str {
            Cow::Borrowed(b) => b.as_bytes().to_vec(),
            Cow::Owned(s) => s.into_bytes(),
        };

        let mut current_idx = init_idx;
        let accum = &mut return_str;
        for next_sample in rng.sample_iter(distr).take(steps) {
            // wait
            let Some(current_cdist) = self.cdist.get(*current_idx) else {
                // eprintln!(
                //     "Nothing to sample at index {current_idx} for {:#?}",
                //     self.cdist
                // );
                break;
            };
            let Some((next_idx, _)) = current_cdist
                .iter()
                .filter(|(_, cprob)| cprob <= &next_sample)
                .last()
            else {
                // eprintln!(
                //     "Nothin is below probability threshold for {next_sample} for {:#?}",
                //     current_cdist
                // );
                break;
            };
            current_idx = next_idx;
            let Some(next_n_gram) = self.indexes.get(*next_idx) else {
                eprintln!(
                    "Couldn't find ngram at index {next_idx} from {:#?}",
                    self.indexes
                );
                break;
            };
            accum.push(next_n_gram[N - 1]);
        }
        match String::from_utf8(return_str) {
            Ok(str) => Ok(Cow::Owned(str)),
            Err(err) => Ok(Cow::Owned(
                String::from_utf8_lossy(err.as_bytes()).to_string(),
            )),
        }
    }
}

#[cfg(test)]
mod test {

    use itertools::Itertools;
    use rustc_hash::FxHashMap;

    use crate::LIPSUM;

    use super::NGramStats;

    const RUST_TEXT: &str = include_str!("rust_text.txt");
    #[test]
    fn test_init() {
        let data = "abcdef";
        let left: [u8; 3] = [b'a', b'b', b'c'];
        let right: [u8; 3] = [b'd', b'e', b'f'];
        let stats = NGramStats::<3>::new(data);
        let trans = stats.transition;
        assert_eq!(
            [(left, [(right, 1)].into_iter().collect::<FxHashMap<_, _>>())]
                .into_iter()
                .collect::<FxHashMap<_, _>>(),
            trans
        );
    }

    #[test]
    fn test_init_2() {
        let init = "abcdefghijklmnopqrstuvwxyz";
        let stats = NGramStats::<3>::new(init);
        let trans = stats.transition;
        println!("transition is {trans:#?}");
    }

    #[test]
    fn test_lipsum() {
        let data = LIPSUM;
        let stats = NGramStats::<3>::new(data);
        let trans = stats.transition;
        let trans = trans.into_iter().collect_vec();
        let trans_str = trans
            .into_iter()
            .flat_map(|(l, hm)| {
                hm.into_iter().map(move |(r, count)| {
                    (
                        String::from_utf8_lossy(&l).to_string(),
                        String::from_utf8_lossy(&r).to_string(),
                        count,
                    )
                })
            })
            .collect_vec();

        println!("{trans_str:#?}");
    }

    #[test]
    fn test_jk() {
        let data = RUST_TEXT;
        let stats = NGramStats::<3>::new(data);
        let trans = stats.markov();

        println!("trans is {:#?}", trans.cdist);
    }

    #[test]
    fn test_gen() {
        let data = RUST_TEXT;
        let stats = NGramStats::<3>::new(data);
        let trans = stats.markov();
        let mut rng = rand::rng();
        let init = "Rust began";
        let generated = trans
            .generate(&mut rng, init, 1600)
            .expect("Should generate");

        println!("{generated}");
    }
}
