Creating the kotlin code
```sh
diplomat-tool -e rust/src/lib.rs -c rust/config.toml kotlin kotlin/
```
this will overwrite the `kotlin/build.gradle.kt` so checkout again

Build the library with
```sh
cd rust && cargo build --release && cd ..
```
copy the library to the kotlin directory.
```sh
cp rust/target/release/librustbasel.dylib kotlin/
```
Build the presentation with 
```sh
typst compile main.typ
```


