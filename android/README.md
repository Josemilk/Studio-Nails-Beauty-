# Studio Nails & Beauty — Android

Aplicación nativa en Kotlin y Jetpack Compose.

- Paquete: `com.studionails.beauty`
- Mínimo: Android 8
- Los datos quedan en el teléfono

## Firma

Todas las APK se firman con la misma clave. La huella pública, que no cambia entre versiones, es:

```
SHA-256: 13:1A:C8:4A:42:40:B0:AD:92:1C:A7:00:F0:DB:D9:98:4B:75:54:FB:99:F4:0F:96:C9:7F:08:E9:5C:AC:95:2A
```

La clave privada no va en el repositorio. Para firmar, coloca `keystore/studio-nails-release.keystore` y `keystore.properties` (hay un ejemplo en `keystore.properties.example`). Sin ese archivo, el proyecto igual compila, pero Android usa la firma de depuración de esa máquina y las actualizaciones no coinciden.

Para comprobar una APK:

```
keytool -printcert -jarfile app.apk
```

El SHA-256 tiene que ser el de arriba.
