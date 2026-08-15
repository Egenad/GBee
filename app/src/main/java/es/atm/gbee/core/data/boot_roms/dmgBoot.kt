package es.atm.gbee.core.data.boot_roms

import es.atm.gbee.modules.CHNL_1_LENGTH
import es.atm.gbee.modules.CHNL_1_VOLUME
import es.atm.gbee.modules.CPU
import es.atm.gbee.modules.MASTER_CONTROL
import es.atm.gbee.modules.MASTER_VOLUME
import es.atm.gbee.modules.Memory
import es.atm.gbee.modules.SOUND_PANNING

object DMGBoot : BootSequence {
    override fun execute() {
        Memory.cleanVRAM()
        setSoundSettings()
    }

    private fun setSoundSettings(){
        Memory.write(MASTER_CONTROL, 0x80.toByte())
        Memory.write(CHNL_1_LENGTH, 0x80.toByte())
        Memory.write(CHNL_1_VOLUME, 0xF3.toByte())
        Memory.write(SOUND_PANNING, 0xF3.toByte())
        Memory.write(MASTER_VOLUME, 0x77.toByte())
    }

}