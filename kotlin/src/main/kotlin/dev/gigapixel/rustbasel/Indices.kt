package dev.gigapixel.rustbasel;
import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure

internal interface IndicesLib: Library {
    fun Indices_destroy(handle: Pointer)
    fun Indices_split_newlines(input: Slice): Pointer
    fun Indices_get_indices(handle: Pointer): Slice
}

class Indices internal constructor (
    internal val handle: Pointer,
    // These ensure that anything that is borrowed is kept alive and not cleaned
    // up by the garbage collector.
    internal val selfEdges: List<Any>,
)  {

    internal class IndicesCleaner(val handle: Pointer, val lib: IndicesLib) : Runnable {
        override fun run() {
            lib.Indices_destroy(handle)
        }
    }

    companion object {
        internal val libClass: Class<IndicesLib> = IndicesLib::class.java
        internal val lib: IndicesLib = Native.load("rustbasel", libClass)
        
        fun splitNewlines(input: String): Indices {
            val (inputMem, inputSlice) = PrimitiveArrayTools.readUtf16(input)
            
            val returnVal = lib.Indices_split_newlines(inputSlice);
            val selfEdges: List<Any> = listOf()
            val handle = returnVal 
            val returnOpaque = Indices(handle, selfEdges)
            CLEANER.register(returnOpaque, Indices.IndicesCleaner(handle, Indices.lib));
            if (inputMem != null) inputMem.close()
            return returnOpaque
        }
    }
    
    fun getIndices(): LongArray {
        
        val returnVal = lib.Indices_get_indices(handle);
            return PrimitiveArrayTools.getLongArray(returnVal)
    }

}