package com.sac.acessibilidade.domain.gesture

fun Gesture.displayName(): String =
    when (this) {
        Gesture.TILT_HEAD_RIGHT -> "Inclinar para Direita"
        Gesture.TILT_HEAD_LEFT -> "Inclinar para Esquerda"
        Gesture.TILT_HEAD_UP -> "Inclinar para Cima"
        Gesture.TILT_HEAD_DOWN -> "Inclinar para Baixo"
        Gesture.TURN_FACE_RIGHT -> "Virar para Direita"
        Gesture.TURN_FACE_LEFT -> "Virar para Esquerda"
        Gesture.NOD -> "Aceno (sim)"
        Gesture.BLINK_RIGHT_EYE -> "Piscar Olho Direito"
        Gesture.BLINK_LEFT_EYE -> "Piscar Olho Esquerdo"
    }

fun SpotifyAction.displayName(): String =
    when (this) {
        SpotifyAction.PLAY_PAUSE -> "Tocar / Pausar"
        SpotifyAction.NEXT_TRACK -> "Próxima Faixa"
        SpotifyAction.PREVIOUS_TRACK -> "Faixa Anterior"
        SpotifyAction.VOLUME_UP -> "Aumentar Volume"
        SpotifyAction.VOLUME_DOWN -> "Diminuir Volume"
    }

fun String.toSpotifyActionOrNull(): SpotifyAction? = SpotifyAction.entries.firstOrNull { it.displayName() == this }

const val NO_ACTION_LABEL = "(Sem ação)"
