// generated by diplomat-tool
import wasm from "./diplomat-wasm.mjs";
import * as diplomatRuntime from "./diplomat-runtime.mjs";

const Indices_box_destroy_registry = new FinalizationRegistry((ptr) => {
    wasm.Indices_destroy(ptr);
});

export class Indices {
    // Internal ptr reference:
    #ptr = null;

    // Lifetimes are only to keep dependencies alive.
    // Since JS won't garbage collect until there are no incoming edges.
    #selfEdge = [];

    #internalConstructor(symbol, ptr, selfEdge) {
        if (symbol !== diplomatRuntime.internalConstructor) {
            console.error("Indices is an Opaque type. You cannot call its constructor.");
            return;
        }
        this.#ptr = ptr;
        this.#selfEdge = selfEdge;

        // Are we being borrowed? If not, we can register.
        if (this.#selfEdge.length === 0) {
            Indices_box_destroy_registry.register(this, this.#ptr);
        }

        return this;
    }
    get ffiValue() {
        return this.#ptr;
    }


    static splitNewlines(input) {
        let functionGarbageCollectorGrip = new diplomatRuntime.GarbageCollectorGrip();
        const inputSlice = diplomatRuntime.DiplomatBuf.str16(wasm, input);
        // This lifetime edge depends on lifetimes 'a
        let aEdges = [inputSlice];


        const result = wasm.Indices_split_newlines(...inputSlice.splat());

        try {
            return new Indices(diplomatRuntime.internalConstructor, result, []);
        }

        finally {
            functionGarbageCollectorGrip.releaseToGarbageCollector();

        }
    }

    getIndices() {
        const diplomatReceive = new diplomatRuntime.DiplomatReceiveBuf(wasm, 8, 4, false);

        // This lifetime edge depends on lifetimes 'a
        let aEdges = [this];


        const result = wasm.Indices_get_indices(diplomatReceive.buffer, this.ffiValue);

        try {
            return Array.from(new diplomatRuntime.DiplomatSlicePrimitive(wasm, diplomatReceive.buffer, "i64", aEdges).getValue());
        }

        finally {
            diplomatReceive.free();
        }
    }

    constructor(symbol, ptr, selfEdge) {
        return this.#internalConstructor(...arguments)
    }
}