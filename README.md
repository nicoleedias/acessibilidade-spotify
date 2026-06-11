# Sistema de Acessibilidade Cefálica para Spotify (SAC)

![Status do Projeto](https://img.shields.io/badge/Status-MVP%20Funcional-green)
![Plataforma](https://img.shields.io/badge/Plataforma-Android-3DDC84)
![Linguagem](https://img.shields.io/badge/Linguagem-Kotlin-7F52FF)
![Min SDK](https://img.shields.io/badge/Android-10%2B%20(API%2029)-orange)

O Sistema de Acessibilidade Cefálica (SAC) usa visão computacional para traduzir movimentos da cabeça em comandos no Spotify. Focado em pessoas com deficiência motora, promove autonomia com processamento **100% local**, garantindo privacidade e baixa latência sem enviar imagens a servidores.

## Visão Geral
A acessibilidade digital é fundamental para a autonomia, mas interfaces baseadas no toque ainda excluem pessoas com limitações motoras severas. Este projeto é um Produto Mínimo Viável (MVP) capaz de identificar movimentos faciais e traduzi-los em comandos de navegação no celular, focado no controle do aplicativo Spotify.

O objetivo é democratizar o acesso ao lazer digital com uma solução estritamente de software, substituindo hardwares caros ou assistentes de voz que sofrem interferência de ruído.

## Funcionalidades e Mapeamento de Gestos
O aplicativo captura imagens da câmera frontal em tempo real e mapeia gestos cefálicos para ações via integração com a Spotify Web API. **O mapeamento é totalmente personalizável** — cada usuário tem capacidades motoras diferentes, então nenhuma combinação gesto→ação é fixada em código.

Mapeamento padrão (alterável na tela de configuração):

| Gesto | Ação padrão |
| ----- | ----------- |
| Inclinar a cabeça para a direita | Aumentar volume |
| Inclinar a cabeça para a esquerda | Diminuir volume |
| Virar o rosto para a direita | Próxima faixa |
| Virar o rosto para a esquerda | Faixa anterior |
| Aceno vertical (sim) | Play / Pause |

## Arquitetura e Tecnologias
Projeto estruturado em camadas (MVVM) para isolar responsabilidades:

* **Apresentação:** Kotlin + **Jetpack Compose** (telas de login, calibração, configuração e player).
* **Visão Computacional:** **MediaPipe Face Mesh** (rastreamento de landmarks) + CameraX para o buffer da câmera; classificação de gestos por pose da cabeça (roll/pitch/yaw) com baseline adaptativo e calibração por usuário.
* **Integração Externa:** **Spotify Web API** (REST) com OAuth 2.0 + PKCE.
* **Persistência:** **Room/SQLite** para o mapeamento de gestos; `EncryptedSharedPreferences` para os tokens.
* **DI / Async:** Hilt + Kotlin Coroutines/Flow.

## Privacidade e Desempenho
* **Edge Computing:** todo o processamento de imagem é local. Nenhum frame de vídeo é armazenado ou enviado à nuvem (conformidade com a LGPD).
* **Baixa Latência:** otimizado para que o tempo entre o gesto e a ação não ultrapasse 500 ms.

---

## Pré-requisitos

### Hardware (dispositivo de execução)
* Smartphone Android **10 ou superior** (API 29+).
* Câmera frontal com resolução mínima de **720p**.
* Conexão ativa com a internet (4G/5G ou Wi-Fi) para a API do Spotify.
* Conta ativa no Spotify (algumas ações de reprodução exigem **Premium**).

### Ambiente de desenvolvimento
* **Android Studio** (Ladybug 2024.2 ou mais recente).
* **JDK 17** (já embutido no Android Studio).
* **Android SDK 35** (compileSdk/targetSdk = 35; minSdk = 29).
* Gradle 8.13 (provido pelo wrapper — não precisa instalar manualmente).

---

## Instalação e Configuração

### 1. Clonar o repositório
```bash
git clone https://github.com/nicoleedias/acessibilidade-spotify.git
cd acessibilidade-spotify
```

### 2. Criar um app no Spotify Developer Dashboard
1. Acesse <https://developer.spotify.com/dashboard> e faça login.
2. Clique em **Create app**.
3. Em **Redirect URI**, adicione exatamente: `sac://callback`
4. Marque a API **Web API** e salve.
5. Copie o **Client ID** gerado.

### 3. Criar o arquivo `local.properties`
Na raiz do projeto, crie (ou edite) o arquivo `local.properties` — ele é ignorado pelo Git e **nunca deve ser commitado**:

```properties
# Caminho do Android SDK (o Android Studio preenche automaticamente ao abrir)
sdk.dir=C\:\\Users\\SEU_USUARIO\\AppData\\Local\\Android\\Sdk

# Credenciais do Spotify (passo 2)
SPOTIFY_CLIENT_ID=cole_aqui_seu_client_id
SPOTIFY_REDIRECT_URI=sac://callback
```

> O `Client ID` é exposto ao app via `BuildConfig` em tempo de build — não fica em código-fonte.

### 4. Abrir e sincronizar no Android Studio
1. **File → Open** e selecione a pasta do projeto.
2. Aguarde o **Gradle Sync** terminar. Na primeira sincronização, uma tarefa Gradle baixa automaticamente o modelo `face_landmarker.task` (~3 MB) para `app/src/main/assets/`.
3. Se o SDK 35 não estiver instalado, o Android Studio oferecerá instalá-lo (**Tools → SDK Manager**).

### 5. Executar no dispositivo
1. Ative a **Depuração USB** no celular (Configurações → Opções do desenvolvedor) **ou** crie um emulador com câmera (Android 10+).
2. Selecione o dispositivo na barra superior e clique em **Run ▶** (`Shift+F10`).
3. Conceda a permissão de **câmera** quando solicitada (uso local, explicado em tela).

---

## Primeiro Uso (fluxo recomendado)
1. **Login:** conecte sua conta Spotify (OAuth 2.0 PKCE). Mantenha o app do Spotify aberto e tocando em algum dispositivo — a Web API exige uma sessão ativa.
2. **Calibração:** siga os 6 passos (inclinar e virar para cada lado, cima e baixo). Fique parado ao capturar a pose neutra; vá ao limite confortável e confirme cada direção. Os limiares são medidos do **seu** movimento real e a direção é aprendida na própria calibração.
3. **Configurar gestos** (opcional): personalize o mapeamento gesto→ação ou use **Restaurar padrões**.
4. **Player ativo:** mantenha o rosto enquadrado; ao abrir, fique parado ~2 s para o baseline travar. Os gestos passam a controlar o Spotify.

> Recalibre sempre que mudar de dispositivo ou notar perda de precisão.

---

## Comandos Úteis (Gradle)
```bash
# Instalar no dispositivo conectado
./gradlew installDebug          # Windows: .\gradlew installDebug

# Testes unitários (rápido, sem dispositivo)
./gradlew test

# Formatar e validar estilo / análise estática
./gradlew ktlintFormat
./gradlew ktlintCheck detekt

# Gate completo antes de commitar
./gradlew ktlintCheck detekt test
```

## Documentação
Os documentos de Visão, Arquitetura, Casos de Uso, Interface e Prototipagem estão na pasta [`Docs/`](Docs/). As decisões técnicas relevantes ficam registradas em [`Docs/decisions/`](Docs/decisions/) no formato ADR.

## Equipe de Desenvolvimento
Projeto acadêmico da **Universidade Católica de Brasília (UCB)**.
* Nicole Cardoso Dias
* Pedro Cauã
* Samuel Gomes
* Rodrigo Barbosa
* Ricardo Oliveira
* Victor Salvador
