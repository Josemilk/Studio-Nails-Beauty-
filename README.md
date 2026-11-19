# Studio Nails & Beauty

Proyecto Android (Kotlin y Jetpack Compose).

La APK de release se firma siempre con la misma clave. Huella pública:

```
SHA-256: 13:1A:C8:4A:42:40:B0:AD:92:1C:A7:00:F0:DB:D9:98:4B:75:54:FB:99:F4:0F:96:C9:7F:08:E9:5C:AC:95:2A
```

La clave privada no está en el repositorio. GitHub Actions la toma de los secretos del repo.

## Compilar

Actions → **Android** → **Run workflow**.

El artefacto `studio-nails-1.0` es la APK firmada. El job falla si el SHA-256 no coincide.

- Paquete: `com.studionails.beauty`
- Mínimo: Android 8
