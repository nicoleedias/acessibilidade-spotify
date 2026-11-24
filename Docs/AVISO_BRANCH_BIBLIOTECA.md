# Aviso: branch `feat/biblioteca-gestos-configuraveis`

**Esta branch ainda NÃO foi compilada nem testada.** As alterações foram feitas
numa máquina sem JDK/Android SDK, então nada abaixo foi executado ainda.

## Pendente antes de gerar o APK / fazer merge

Rodar na máquina de desenvolvimento (com Android Studio configurado):

```bash
./gradlew ktlintCheck detekt test
./gradlew assembleDebug
```

O APK sai em `app/build/outputs/apk/debug/`.

Se algo falhar, corrigir nesta branch antes do merge na `main`.

## O que muda nesta branch

- Nova ação configurável **Voltar** (`SpotifyAction.BACK`), escolhida na tela de
  personalização de gestos.
- A biblioteca (playlists e faixas) deixa de usar gestos fixos e lê o
  mapeamento do usuário:
  - Próxima Faixa / Faixa Anterior: movem o foco entre playlists ou faixas.
  - Tocar / Pausar: abre a playlist em foco, ou toca a faixa em foco.
  - Voltar: sobe um nível (faixas para playlists).
- Aviso no topo da biblioteca quando alguma dessas ações está sem gesto.
- Continua sem gesto padrão: o usuário personaliza tudo.

## Pontos para checar no aparelho

- Configurar os quatro gestos e navegar playlists, faixas e voltar.
- Deixar uma ação sem gesto e ver o aviso na biblioteca.
- Confirmar que o gesto de Voltar não faz nada na tela do player.
- Quem usava os gestos fixos antigos precisa configurar de novo.

Apague este arquivo depois que a validação for feita.
