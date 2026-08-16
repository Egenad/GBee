package es.atm.gbee

import es.atm.gbee.core.data.boot_roms.BootController
import es.atm.gbee.core.data.boot_roms.DMGBoot
import es.atm.gbee.modules.CPU
import es.atm.gbee.modules.PPU
import es.atm.gbee.modules.ROM

fun main(args: Array<String>){
    CPU.init()

    //ROM.load_rom_from_path("/home/angel/Documentos/Git/GBee/roms/GoldenSacra.gb")
    ROM.load_rom_from_path("/home/angel/Documentos/Git/GBee/roms/tetris.gb")

    PPU.init()
    BootController.start(DMGBoot)
    while (BootController.isRunning) {
        BootController.tick()
    }
}
