
## -- Conversión de Byte a Int --

En Kotlin/Java, `Byte` tiene signo: va de `-128` a `127`. Pero los bytes de la Game Boy normalmente representan valores sin signo entre `0` y `255`.

Cuando se hace:

```kotlin
byte.toInt() and 0xFF
```

ocurre esto:

1. `toInt()` amplía el `Byte` a `Int`, conservando el signo.
2. `and 0xFF` elimina esa extensión de signo y conserva únicamente los 8 bits originales.

Por ejemplo:

```kotlin
val byte: Byte = 0xFF.toByte() // internamente vale -1

byte.toInt()          // -1
byte.toInt() and 0xFF // 255
```

Existe `UByte`, que va de `0` a `255`:

```kotlin
val value: UByte = byte.toUByte()
val number: Int = value.toInt() // 255
```

Pero en JVM, `ByteArray`, muchas APIs y seguramente tu `Memory.getByteOnAddress()` trabajan con `Byte`. Se peude cambiar el código para usar `UByte`, aunque suele resultar más incómodo. En un emulador es habitual almacenar como `Byte` y convertir al leer.

Para evitar repetirlo constantemente, se puede añadir una extensión:

```kotlin
private fun Byte.toUnsignedInt(): Int = toInt() and 0xFF
```
