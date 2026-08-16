package es.atm.gbee.core.data.boot_roms

import es.atm.gbee.modules.BGP
import es.atm.gbee.modules.CHNL_1_LENGTH
import es.atm.gbee.modules.CHNL_1_VOLUME
import es.atm.gbee.modules.CPU
import es.atm.gbee.modules.GB_X_TOTAL_TILES
import es.atm.gbee.modules.GB_Y_RESOLUTION
import es.atm.gbee.modules.LY_ADDR
import es.atm.gbee.modules.MASTER_CONTROL
import es.atm.gbee.modules.MASTER_VOLUME
import es.atm.gbee.modules.Memory
import es.atm.gbee.modules.SCY
import es.atm.gbee.modules.SOUND_PANNING
import es.atm.gbee.modules.TM_1_START
import es.atm.gbee.modules.VRAM_START

object DMGBoot : BootSequence {
    override var phase = Phase.LOGO_SCROLL
    private const val INITIAL_SCROLL_Y = 104
    private const val LOGO_WAIT_FRAMES = 60
    private var scrollY = INITIAL_SCROLL_Y
    private var waitFrames = 0
    private var previousLY = 0

    override fun init() {
        phase = Phase.LOGO_SCROLL
        scrollY = INITIAL_SCROLL_Y
        waitFrames = 0
        previousLY = Memory.getByteOnAddress(LY_ADDR).toInt() and 0xFF
        CPU.setBootstrapPending(true)

        Memory.cleanVRAM()
        loadLogo()
        initSettings()
    }

    override fun tick(): Boolean {
        val ly = Memory.getByteOnAddress(LY_ADDR).toInt() and 0xFF
        val enteringVBlank = GB_Y_RESOLUTION in (previousLY + 1)..ly
        previousLY = ly

        if (enteringVBlank) {
            tickFrame()
        }

        return phase == Phase.FINISHED
    }

    private fun initSettings() {
        // -- Sound --
        Memory.writeByteOnAddress(MASTER_CONTROL, 0x80.toByte())
        Memory.writeByteOnAddress(CHNL_1_LENGTH, 0x80.toByte())
        Memory.writeByteOnAddress(CHNL_1_VOLUME, 0xF3.toByte())
        Memory.writeByteOnAddress(SOUND_PANNING, 0xF3.toByte())
        Memory.writeByteOnAddress(MASTER_VOLUME, 0x77.toByte())
        // -- Color --
        Memory.writeByteOnAddress(BGP, 0xE4.toByte())
        // -- Scroll --
        Memory.writeByteOnAddress(SCY, scrollY.toByte())
    }

    private fun loadLogo() {
        DmgBootLogo.tileData.forEachIndexed { offset, value ->
            Memory.writeByteOnAddress(VRAM_START + offset, value)
        }

        DmgBootLogo.tileMap.forEachIndexed { index, tile ->
            val row = index / DmgBootLogo.WIDTH_TILES
            val column = index % DmgBootLogo.WIDTH_TILES
            val address = TM_1_START + (DmgBootLogo.MAP_Y + row) * GB_X_TOTAL_TILES + DmgBootLogo.MAP_X + column
            Memory.writeByteOnAddress(address, tile)
        }
    }

    private fun tickFrame() {
        when (phase) {
            Phase.LOGO_SCROLL -> {
                scrollY--
                Memory.writeByteOnAddress(SCY, scrollY.toByte())
                if (scrollY == 0) {
                    phase = Phase.LOGO_WAIT
                }
            }
            Phase.LOGO_WAIT -> {
                waitFrames++
                if (waitFrames >= LOGO_WAIT_FRAMES) {
                    finishBoot()
                }
            }
            Phase.FINISHED -> Unit
        }
    }

    private fun finishBoot() {
        CPU.A = 0x01
        CPU.F = 0xB0.toByte()
        CPU.B = 0x00
        CPU.C = 0x13
        CPU.D = 0x00
        CPU.E = 0xD8.toByte()
        CPU.H = 0x01
        CPU.L = 0x4D
        CPU.SP = 0xFFFE
        CPU.PC = 0x0100

        Memory.writeByteOnAddress(SCY, 0)
        Memory.writeByteOnAddress(BGP, 0xFC.toByte())
        CPU.setBootstrapPending(false)
        phase = Phase.FINISHED
    }
}
