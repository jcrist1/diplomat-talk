package dev.gigapixel.rustbasel;
import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure

internal interface LineSplitterLib: Library {
    fun LineSplitter_destroy(handle: Pointer)
    fun LineSplitter_new(input: Slice): Pointer
    fun LineSplitter_spliterator(handle: Pointer): Pointer
}

class LineSplitter internal constructor (
    internal val handle: Pointer,
    // These ensure that anything that is borrowed is kept alive and not cleaned
    // up by the garbage collector.
    internal val selfEdges: List<Any>,
)  {

    internal class LineSplitterCleaner(val handle: Pointer, val lib: LineSplitterLib) : Runnable {
        override fun run() {
            lib.LineSplitter_destroy(handle)
        }
    }

    companion object {
        internal val libClass: Class<LineSplitterLib> = LineSplitterLib::class.java
        internal val lib: LineSplitterLib = Native.load("rustbasel", libClass)
        
        fun new_(input: String): LineSplitter {
            val (inputMem, inputSlice) = PrimitiveArrayTools.readUtf8(input)
            
            val returnVal = lib.LineSplitter_new(inputSlice);
            val selfEdges: List<Any> = listOf()
            val handle = returnVal 
            val returnOpaque = LineSplitter(handle, selfEdges)
            CLEANER.register(returnOpaque, LineSplitter.LineSplitterCleaner(handle, LineSplitter.lib));
            if (inputMem != null) inputMem.close()
            return returnOpaque
        }
    }
    
    fun spliterator(): Spliterator {
        
        val returnVal = lib.LineSplitter_spliterator(handle);
        val selfEdges: List<Any> = listOf()
        val aEdges: List<Any?> = listOf(this)
        val handle = returnVal 
        val returnOpaque = Spliterator(handle, selfEdges, aEdges)
        CLEANER.register(returnOpaque, Spliterator.SpliteratorCleaner(handle, Spliterator.lib));
        return returnOpaque
    }

}