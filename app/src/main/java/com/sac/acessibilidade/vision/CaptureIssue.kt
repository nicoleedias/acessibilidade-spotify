package com.sac.acessibilidade.vision

/**
 * Problema de captura detectado no frame da câmera durante a calibração (UC02).
 *
 * [isBlocking] separa o que **impede** calibrar do que apenas **degrada** a precisão:
 * calibrar com dois rostos no quadro mediria a pessoa errada, então bloqueia; já a
 * iluminação fraca só avisa, porque bloquear deixaria o usuário travado sem saber o
 * que fazer num ambiente que ele talvez não consiga mudar.
 */
enum class CaptureIssue(val isBlocking: Boolean) {
    NONE(isBlocking = false),
    TOO_DARK(isBlocking = false),
    TOO_BRIGHT(isBlocking = false),
    NO_FACE(isBlocking = true),
    MULTIPLE_FACES(isBlocking = true),
    FACE_TOO_FAR(isBlocking = false),
    FACE_TOO_CLOSE(isBlocking = false),
}
