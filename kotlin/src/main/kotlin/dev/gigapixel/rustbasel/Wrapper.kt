package dev.gigapixel.rustbasel;
import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure

internal interface WrapperLib: Library {
    fun Wrapper_destroy(handle: Pointer)
    fun Wrapper_new(): Pointer
    fun Wrapper_return_inner(handle: Pointer): Slice
    fun Wrapper_owned_bytes(handle: Pointer, write: Pointer): ResultUnitUnit
}

class Wrapper internal constructor (
    internal val handle: Pointer,
    // These ensure that anything that is borrowed is kept alive and not cleaned
    // up by the garbage collector.
    internal val selfEdges: List<Any>,
)  {

    internal class WrapperCleaner(val handle: Pointer, val lib: WrapperLib) : Runnable {
        override fun run() {
            lib.Wrapper_destroy(handle)
        }
    }

    companion object {
        internal val libClass: Class<WrapperLib> = WrapperLib::class.java
        internal val lib: WrapperLib = Native.load("rustbasel", libClass)
        
        fun new_(): Wrapper {
            
            val returnVal = lib.Wrapper_new();
            val selfEdges: List<Any> = listOf()
            val handle = returnVal 
            val returnOpaque = Wrapper(handle, selfEdges)
            CLEANER.register(returnOpaque, Wrapper.WrapperCleaner(handle, Wrapper.lib));
            return returnOpaque
        }
    }
    
    fun returnInner(): String {
        
        val returnVal = lib.Wrapper_return_inner(handle);
            return PrimitiveArrayTools.getUtf8(returnVal)
    }
    
    fun ownedBytes(): Result<String> {
        val write = DW.lib.diplomat_buffer_write_create(0)
        val returnVal = lib.Wrapper_owned_bytes(handle, write);
        if (returnVal.isOk == 1.toByte()) {
            
            val returnString = DW.writeToString(write)
            return returnString.ok()
        } else {
            return UnitError().err()
        }
    }

}