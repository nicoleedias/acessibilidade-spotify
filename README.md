# Nodify

![Status do Projeto](https://img.shields.io/badge/Status-MVP%20Funcional-green)
![Plataforma](https://img.shields.io/badge/Plataforma-Android-3DDC84)
![Linguagem](https://img.shields.io/badge/Linguagem-Kotlin-7F52FF)
![Min SDK](https://img.shields.io/badge/Android-10%2B%20(API%2029)-orange)

O Sistema de Acessibilidade Cefálica, Nodify, usa visão computacional para traduzir movimentos da cabeça em comandos no Spotify. Focado em pessoas com deficiência motora, promove autonomia com processamento **100% local**, garantindo privacidade e baixa latência sem enviar imagens a servidores.

## Visão Geral
A acessibilidade digital é fundamental para a autonomia, mas interfaces baseadas no toque ainda excluem pessoas com limitações motoras severas. Este projeto é um Produto Mínimo Viável (MVP) capaz de identificar movimentos faciais e traduzi-los em comandos de navegação no celular, focado no controle do aplicativo Spotify.

O objetivo é democratizar o acesso ao lazer digital com uma solução estritamente de software, substituindo hardwares caros ou assistentes de voz que sofrem interferência de ruído.

## Funcionalidades e Mapeamento de Gestos
O aplicativo captura imagens da câmera frontal em tempo real e mapeia gestos cefálicos para ações via integração com a Spotify Web API. **O mapeamento é totalmente personalizável** — cada usuário tem capacidades motoras diferentes, então nenhuma combinação gesto→ação é fixada em código.

**O mapeamento começa vazio:** nenhum gesto dispara ação até o usuário escolher. Cada gesto pode ter no máximo uma ação e cada ação só pode estar em um gesto (exclusividade garantida na tela de configuração). A opção **Restaurar padrões** oferece uma sugestão pronta:

| Gesto | Ação sugerida (Restaurar padrões) |
| ----- | --------------------------------- |
| Inclinar a cabeça para a direita | Aumentar volume |
| Inclinar a cabeça para a esquerda | Diminuir volume |
| Virar o rosto para a direita | Próxima faixa |
| Virar o rosto para a esquerda | Faixa anterior |
| Aceno vertical (sim) | Play / Pause |

Gestos disponíveis: inclinar (direita, esquerda, cima, baixo), virar o rosto (direita, esquerda), aceno vertical e piscar (olho direito, olho esquerdo).

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

## Como instalar

Escolha **uma** das duas opções:

| | Opção A — Instalar o APK | Opção B — Rodar pelo código |
| - | ------------------------ | --------------------------- |
| Para quem | Quem só quer **testar** o app | Quem quer **mexer no código** |
| Precisa de | Só um celular Android | Computador + Android Studio + app próprio no Spotify |
| Tempo | ~5 min | 30–40 min |

## Opção A — Instalar o APK (só testar)

> **Antes de tudo:** o app está em *modo de desenvolvimento* no Spotify, então **só consegue logar quem foi cadastrado pelo dono do app**. Mande o **e-mail da sua conta Spotify** para o Pedro e espere ele confirmar que cadastrou. Sem isso o login falha.

1. **Baixe o APK** que o Pedro enviou (`Nodify-debug.apk`) direto no celular.
2. Toque no arquivo para instalar. O Android vai pedir para permitir **"Instalar apps desconhecidos"** para o app que você usou para abrir o arquivo (Drive, Chrome, WhatsApp…) — permita.
3. Se aparecer o aviso do **Play Protect** ("app não verificado"), toque em **Instalar mesmo assim**. É esperado: o APK é de teste e não vem da Play Store.
4. Instale e entre no **app do Spotify** no mesmo celular e **dê play em uma música** (a Web API só controla um dispositivo com sessão ativa).
5. Abra o **Nodify**, conceda a permissão de **câmera** e faça o [Primeiro uso](#passo-9--primeiro-uso): login → calibração → configurar gestos → player.

**Requisitos do celular:** Android 10+, câmera frontal, processador de 64 bits (arm64) — praticamente todo celular lançado nos últimos anos.

Problemas? Veja a [Solução de problemas](#solução-de-problemas).

---

## Opção B — Rodar pelo código (Android Studio)


> **Tempo estimado:** 30–40 min na primeira vez (a maior parte é download do Android Studio e do Gradle).
> Siga **na ordem**. Se algo falhar, veja [Solução de problemas](#solução-de-problemas).

### O que você precisa ter

| Item | Detalhe |
| ---- | ------- |
| Computador | Windows, macOS ou Linux, ~10 GB livres, internet |
| Celular Android | Android **10+**, câmera frontal, cabo USB **de dados** |
| Conta Spotify | Gratuita ou Premium (veja a [limitação](#limitação-conhecida-do-spotify) abaixo) |
| Ambiente | Rosto bem iluminado e enquadrado durante o uso |

> Use um **celular físico**. O emulador não serve para testar os gestos de forma confiável (a câmera virtual não reproduz movimentos de cabeça).

### Passo 1 — Instalar o Android Studio
1. Baixe em <https://developer.android.com/studio> (versão **Ladybug 2024.2 ou mais nova**) e instale com as opções padrão.
2. Na primeira abertura, deixe o assistente instalar o **Android SDK**.
3. Depois: **More Actions → SDK Manager** (ou **Tools → SDK Manager**) → aba **SDK Platforms** → marque **Android 15 (API 35)** → **Apply**.

O JDK 17 já vem dentro do Android Studio e o Gradle 8.13 é baixado sozinho pelo projeto — **não instale nenhum dos dois à parte**.

### Passo 2 — Baixar o projeto
Com Git instalado:
```bash
git clone https://github.com/nicoleedias/acessibilidade-spotify.git
cd acessibilidade-spotify
```
Sem Git: no GitHub, botão verde **Code → Download ZIP**, extraia e use a pasta extraída.

> No Windows, clone/extraia em um caminho **curto** (ex.: `C:\dev\nodify`). Caminhos muito longos podem quebrar o build.

### Passo 3 — Criar o seu app no Spotify (Client ID)
Cada pessoa cria o **próprio** app no Spotify. É grátis, leva ~2 minutos e evita depender de uma lista de usuários de outra pessoa.

1. Entre em <https://developer.spotify.com/dashboard> com a sua conta Spotify.
2. Clique em **Create app**. Nome e descrição livres (ex.: "Nodify teste").
3. Em **Redirect URIs**, adicione **exatamente** `sac://callback` e clique em **Add** (sem espaço e sem `/` no final).
4. Em *Which API/SDKs are you planning to use?*, marque **Web API**.
5. Aceite os termos e clique em **Save**.
6. Abra o app criado → **Settings** → copie o **Client ID**.

> O **Client Secret não é usado** — o app usa PKCE, que dispensa segredo. Não cole o secret em lugar nenhum.

### Passo 4 — Criar o `local.properties`
Na **raiz do projeto**, copie o template e edite:
```bash
# macOS / Linux / Git Bash
cp local.properties.template local.properties
# Windows PowerShell
Copy-Item local.properties.template local.properties
```
O conteúdo final deve ficar assim:
```properties
SPOTIFY_CLIENT_ID=cole_aqui_o_client_id_do_passo_3
SPOTIFY_REDIRECT_URI=sac://callback
```
- A linha `sdk.dir=...` o Android Studio cria sozinho ao abrir o projeto; não precisa mexer.
- Este arquivo é **ignorado pelo Git** de propósito. **Nunca o commite nem o envie em grupo/chat.**

### Passo 5 — Abrir e sincronizar
1. Android Studio → **File → Open** → selecione a **pasta raiz** do projeto (a que contém `settings.gradle.kts`).
2. Aguarde o **Gradle Sync** (barra de progresso embaixo; na primeira vez pode levar vários minutos).
3. Na primeira sincronização, o projeto **baixa sozinho** o modelo `face_landmarker.task` (~3 MB) para `app/src/main/assets/`. Isso exige internet.
4. Se aparecer *"Install missing SDK"*, clique para instalar.

Terminou bem quando o painel **Build** mostra `BUILD SUCCESSFUL` e não há erros em vermelho.

### Passo 6 — Preparar o celular
1. **Configurações → Sobre o telefone** → toque **7 vezes** em *Número da versão* (ativa o modo desenvolvedor).
2. **Configurações → Sistema → Opções do desenvolvedor** → ative **Depuração USB**.
3. Conecte o cabo ao computador. No celular, aceite **"Permitir depuração USB?"** (marque *Sempre permitir*).
4. No Android Studio, o nome do celular deve aparecer no seletor de dispositivos (barra superior).

> **Alternativa sem cabo:** Opções do desenvolvedor → *Depuração sem fio* → **Parear dispositivo**; no Android Studio use **Device Manager → Pair devices using Wi-Fi**.

### Passo 7 — Instalar e abrir o app
1. Escolha o celular no seletor e clique em **Run ▶** (`Shift+F10`).
2. O app abre sozinho no celular. Conceda a permissão de **câmera** (o app explica que o processamento é local).

Pelo terminal, o equivalente é `./gradlew installDebug` (Windows: `.\gradlew installDebug`).

### Passo 8 — Preparar o Spotify no celular
1. Instale e entre no **app do Spotify** no mesmo celular.
2. **Dê play em qualquer música** e deixe o Spotify tocando. A Web API só controla um dispositivo com **sessão ativa**; sem isso os comandos falham com "nenhum dispositivo ativo".

### Passo 9 — Primeiro uso
1. **Login:** toque em conectar e autorize a sua conta Spotify.
2. **Calibração (6 passos):** inclinar e virar para cada lado, cima e baixo. Fique parado na pose neutra; vá até o limite **confortável** e confirme cada direção. Leva ~2 min.
3. **Configurar gestos:** **o mapeamento começa vazio** — nenhum gesto faz nada até você atribuir. Escolha uma ação para cada gesto que consegue fazer (cada gesto e cada ação só podem ser usados uma vez) ou use **Restaurar padrões**.
4. **Player ativo:** mantenha o rosto enquadrado e **fique parado ~2 s** para o baseline travar. Faça um gesto e veja o Spotify reagir.

> Trocou de celular ou perdeu precisão? **Recalibre.** A calibração é salva por aparelho.

### Limitação conhecida do Spotify
Apps novos ficam em **modo de desenvolvimento**: só funcionam com contas autorizadas pelo dono do app. Por isso o Passo 3 manda **cada um criar o seu app** — assim a sua conta já é a autorizada. Além disso, alguns endpoints de reprodução (`/me/player/*`) exigem **Spotify Premium**; com conta gratuita, alguns comandos podem ser recusados. É uma restrição do Spotify, não um defeito do app.

Se preferir usar o app de outra pessoa, o dono precisa ir em *Settings → User Management* e cadastrar o seu e-mail do Spotify (limite de 25 usuários).

### Solução de problemas

| Sintoma | Causa provável | O que fazer |
| ------- | -------------- | ----------- |
| Celular não aparece no seletor | Cabo só de carga, depuração desligada ou driver | Troque o cabo; reative a Depuração USB; no Windows instale o *Google USB Driver* (SDK Manager → SDK Tools) |
| `SDK location not found` | Falta `sdk.dir` | Abra o projeto pelo Android Studio (ele cria) ou adicione `sdk.dir=C\:\\Users\\VOCE\\AppData\\Local\\Android\\Sdk` ao `local.properties` |
| Build falha: `Failed to find target android-35` | SDK 35 ausente | Passo 1.3 |
| Login do Spotify: `INVALID_CLIENT: Invalid redirect URI` | Redirect URI diferente | No painel do Spotify deve ser exatamente `sac://callback` |
| Login do Spotify: *User not registered in the Developer Dashboard* | Conta não autorizada no app | Use o seu próprio app (Passo 3) ou peça ao dono para cadastrar seu e-mail |
| Login falha de imediato / Client ID vazio | `local.properties` ausente ou sem as chaves | Refaça o Passo 4 e rode **Build → Rebuild Project** |
| Comandos sem efeito ("sem dispositivo ativo") | Spotify fechado ou parado | Passo 8: dê play no Spotify |
| Erro 403 nos comandos de reprodução | Conta gratuita | Limitação do Spotify (exige Premium) |
| Gestos disparam sozinhos ou não disparam | Pouca luz, rosto cortado, calibração antiga | Melhore a luz, enquadre o rosto, recalibre |
| `face_landmarker.task` não foi baixado | Sem internet durante o sync | Baixe de <https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/latest/face_landmarker.task> e coloque em `app/src/main/assets/` |
| Sync lento ou travado | Primeira execução baixa muita coisa | Aguarde; se travar, **File → Invalidate Caches → Invalidate and Restart** |

Ainda com problema? Abra uma *issue* no repositório com a mensagem de erro do painel **Build** e o modelo do seu celular.

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
