package es.atm.gbee.modules

const val SB = 0xFF01 // Serial Transfer Data
const val SC = 0xFF02 // Serial Transfer Control

object IO {

    private var serialData = ByteArray(2)

    fun readFromIO(address: Int) : Byte{

        if(address == SB){
            return serialData[0]
        }

        if(address == SC){
            return serialData[1]
        }

        if(address in DIV..TAC){
            return Timer.readFromTimer(address)
        }

        if(address == IF){
            return Memory.read(IF)
        }

        println("IO - Unsupported read on address: $address")
        return 0
    }

    fun writeToIO(address: Int, value: Byte){
        if(address == SB){
            serialData[0] = value
            Memory.write(SB, value)
            return
        }

        if(address == SC){
            serialData[1] = value
            Memory.write(SC, value)
            return
        }

        if(address in DIV..TAC){
            Timer.writeToTimer(address, value)
        }

        if(address == IF){
            Interrupt.set_IF(value.toInt())
        }

        println("IO - Unkown write on address: $address")
    }
}