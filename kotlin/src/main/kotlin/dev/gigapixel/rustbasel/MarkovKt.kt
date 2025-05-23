package dev.gigapixel.rustbasel;
import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure

internal interface MarkovKtLib: Library {
    fun MarkovKt_destroy(handle: Pointer)
    fun MarkovKt_train(data: Slice): Pointer
    fun MarkovKt_generate(handle: Pointer, init: Slice, bytesToGenerate: Int, write: Pointer): ResultUnitInt
}

class MarkovKt internal constructor (
    internal val handle: Pointer,
    // These ensure that anything that is borrowed is kept alive and not cleaned
    // up by the garbage collector.
    internal val selfEdges: List<Any>,
)  {

    internal class MarkovKtCleaner(val handle: Pointer, val lib: MarkovKtLib) : Runnable {
        override fun run() {
            lib.MarkovKt_destroy(handle)
        }
    }

    companion object {
        internal val libClass: Class<MarkovKtLib> = MarkovKtLib::class.java
        internal val lib: MarkovKtLib = Native.load("rustbasel", libClass)
        
        fun train(data: String): MarkovKt {
            val (dataMem, dataSlice) = PrimitiveArrayTools.readUtf8(data)
            
            val returnVal = lib.MarkovKt_train(dataSlice);
            val selfEdges: List<Any> = listOf()
            val handle = returnVal 
            val returnOpaque = MarkovKt(handle, selfEdges)
            CLEANER.register(returnOpaque, MarkovKt.MarkovKtCleaner(handle, MarkovKt.lib));
            if (dataMem != null) dataMem.close()
            return returnOpaque
        }
    }
    
    /** Not thread safe
    */
    fun generate(init: String, bytesToGenerate: Int): Result<String> {
        val (initMem, initSlice) = PrimitiveArrayTools.readUtf8(init)
        val write = DW.lib.diplomat_buffer_write_create(0)
        val returnVal = lib.MarkovKt_generate(handle, initSlice, bytesToGenerate, write);
        if (returnVal.isOk == 1.toByte()) {
            
            val returnString = DW.writeToString(write)
            return returnString.ok()
        } else {
            return IntError(returnVal.union.err).err()
        }
    }

}