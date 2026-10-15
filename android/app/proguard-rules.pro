# Reglas de R8 para la release (ADR 0013).
#
# La app no necesita reglas propias: Hilt, Room y OkHttp ya traen sus reglas de
# consumidor. Esto es lo único que hace falta:

# Tink (dependencia de androidx.security:security-crypto) referencia anotaciones de
# errorprone que no se empaquetan en el APK; sin este aviso R8 no llega a compilar.
-dontwarn com.google.errorprone.annotations.**

# Los crashes de la release se siguen leyendo: se conservan fichero y línea aunque
# R8 ofusque los nombres (el mapping queda en app/build/outputs/mapping/release/).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
