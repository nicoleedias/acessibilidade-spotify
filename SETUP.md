# Guia rápido de setup (máquina nova)

Checklist para rodar o **SAC** num desktop/telefone novo. Para detalhes, ver o [README](README.md).

> O `local.properties` (com o Client ID) **não vem no `git clone`** — é ignorado pelo Git por segurança. Você o recria pelo template.

## No desktop novo

1. **Instalar** Android Studio (Ladybug 2024.2+; já traz o JDK 17) e o **Android SDK 35** (Tools → SDK Manager).
2. **Clonar** o projeto:
   ```bash
   git clone https://github.com/nicoleedias/acessibilidade-spotify.git
   cd acessibilidade-spotify
   ```
3. **Criar o `local.properties`** a partir do template:
   ```bash
   # copie o template e edite os valores
   cp local.properties.template local.properties
   ```
   Preencha:
   ```properties
   SPOTIFY_CLIENT_ID=<seu_client_id>
   SPOTIFY_REDIRECT_URI=sac://callback
   ```
   - O `sdk.dir` o Android Studio preenche sozinho ao abrir.
   - O **Client ID** está no painel do Spotify: <https://developer.spotify.com/dashboard> → seu app → **Settings**.
   - No app do Spotify, confirme que o **Redirect URI** `sac://callback` está cadastrado.
4. **Abrir no Android Studio** → aguardar o **Gradle Sync**. Na primeira vez, a tarefa `downloadFaceLandmarkerModel` baixa o modelo `face_landmarker.task` (~3,7 MB) automaticamente — precisa de internet.
5. **Run ▶** com o aparelho conectado.

## No telefone novo

1. Ative a **Depuração USB** (Configurações → Opções do desenvolvedor).
2. **Run ▶** no Android Studio para instalar.
3. Conceda a permissão de **câmera**.
4. **Refaça a calibração** — ela é salva por aparelho, então o telefone novo começa do zero.

## Validar que está tudo certo

```bash
# Windows: .\gradlew ...
./gradlew ktlintCheck detekt test   # gate completo deve passar (BUILD SUCCESSFUL)
./gradlew installDebug              # instala no dispositivo conectado
```

## O que NÃO vem no clone (recriar/baixar)

| Item | Como obter |
| ---- | ---------- |
| `local.properties` | copiar de `local.properties.template` e preencher o Client ID |
| `face_landmarker.task` | baixado automaticamente no primeiro build (internet) |
| Android SDK / Android Studio | instalar na máquina |
| Calibração do usuário | refeita no app, por aparelho |
