# Fabringo

- Mantén la documentación del proyecto actualizada en el mismo cambio que modifique requisitos, instalación, arquitectura o flujos de móvil y TV.
- Verifica versiones, compatibilidad e instrucciones de Flutter y Android en sus fuentes oficiales cuando una decisión dependa de esos datos. Documenta las versiones que el proyecto haya probado.
- Ejecuta las pruebas móviles en el Android físico del usuario.
- Mantén fuera de Git y de GitHub toda información sensible: llaves privadas y de firma Android, archivos de almacén de claves, contraseñas, tokens, claves de API, credenciales de cuentas de servicio de Google Play o Firebase, archivos de entorno reales y datos privados. Guarda los secretos en el entorno local o en un gestor de secretos. Los archivos de ejemplo deben contener únicamente valores ficticios.
- Antes de cada commit o push, revisa los nombres y el contenido de los archivos preparados, incluidos los que Git ya sigue. Si detectas un secreto, detén la publicación y retíralo del cambio sin mostrar su valor.
