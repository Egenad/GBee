package es.atm.gbee.modules

const val CHNL_1_SWEEP : Int        = 0xFF10
const val CHNL_1_LENGTH : Int       = 0xFF11
const val CHNL_1_VOLUME : Int       = 0xFF12
const val CHNL_1_PERIOD_LOW : Int   = 0xFF13
const val CHNL_1_PERIOD_HIGH : Int  = 0xFF14

const val CHNL_2_LENGTH : Int       = 0xFF16
const val CHNL_2_VOLUME : Int       = 0xFF17
const val CHNL_2_PERIOD_LOW : Int   = 0xFF18
const val CHNL_2_PERIOD_HIGH : Int  = 0xFF19

const val CHNL_3_PERIOD_HIGH : Int  = 0xFF1E

const val CHNL_4_LENGTH : Int       = 0xFF20
const val CHNL_4_VOLUME : Int       = 0xFF21
const val CHNL_4_FREQUENCY : Int    = 0xFF22
const val CHNL_4_CONTROL : Int      = 0xFF23

const val MASTER_VOLUME : Int       = 0xFF24
const val SOUND_PANNING : Int       = 0xFF25
const val MASTER_CONTROL : Int      = 0xFF26

const val WAVE_PATTERN_START : Int  = 0xFF30
const val WAVE_PATTERN_END : Int    = 0xFF3F

object Audio {
}