package dev.gigapixel.rustbasel;
import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure

internal interface SpliteratorLib: Library {
    fun Spliterator_destroy(handle: Pointer)
    fun Spliterator_next(handle: Pointer): OptionSlice
}
typealias SpliteratorIteratorItem = String

class Spliterator internal constructor (
    internal val handle: Pointer,
    // These ensure that anything that is borrowed is kept alive and not cleaned
    // up by the garbage collector.
    internal val selfEdges: List<Any>,
    internal val aEdges: List<Any?>,
): Iterator<String> {

    internal class SpliteratorCleaner(val handle: Pointer, val lib: SpliteratorLib) : Runnable {
        override fun run() {
            lib.Spliterator_destroy(handle)
        }
    }

    companion object {
        internal val libClass: Class<SpliteratorLib> = SpliteratorLib::class.java
        internal val lib: SpliteratorLib = Native.load("rustbasel", libClass)
    }
    
    internal fun nextInternal(): String? {
        
        val returnVal = lib.Spliterator_next(handle);
        
        val intermediateOption = returnVal.option() ?: return null
            return PrimitiveArrayTools.getUtf8(intermediateOption)
                                
    }

    var iterVal = nextInternal()

    override fun hasNext(): Boolean {
       return iterVal != null
    }

    override fun next(): String{
        val returnVal = iterVal
        if (returnVal == null) {
            throw NoSuchElementException()
        } else {
            iterVal = nextInternal()
            return returnVal
        }
    }

}