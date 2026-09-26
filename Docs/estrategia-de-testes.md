# Estratégia de Testes — Nodify

**Sistema de Acessibilidade Cefálica para Spotify**

Documento de referência para a defesa: o que é testado, em que nível, com que ferramenta e por quê.

| Campo | Valor |
| --- | --- |
| Última atualização | 25/09/2026 |
| Testes unitários | **186** (JVM, sem dispositivo) |
| Testes de integração | **33** (dispositivo/emulador) |
| Testes funcionais | **51** (dispositivo/emulador) |
| Teste de usabilidade | Protocolo em [testes-usabilidade.md](testes-usabilidade.md) + avaliação heurística (§6) |
| **Total automatizado** | **270** |

---

## 1. Os quatro níveis

| Nível | O que valida | Onde | Ferramenta | Precisa de dispositivo? |
| --- | --- | --- | --- | --- |
| **Unitário** | Uma classe isolada, com as dependências substituídas | `app/src/test/` | JUnit 4 + MockK + kotlinx-coroutines-test | Não |
| **Integração** | Duas ou mais camadas reais conversando (repositório + Room, repositório + Keystore) | `app/src/androidTest/` | JUnit 4 + AndroidJUnit4 + Room in-memory | Sim |
| **Funcional** | Uma tela inteira do ponto de vista de quem usa: renderiza, responde ao toque, anuncia ao leitor de tela | `app/src/androidTest/` | Compose UI Test | Sim |
| **Usabilidade** | Se pessoas reais conseguem realizar as tarefas e se as metas de latência/acurácia se sustentam em uso real | Sessões presenciais | Roteiro + SUS + cronômetro | Sim |

### Por que quatro níveis e não só um

Cada nível pega uma classe de defeito que os outros não pegam:

- O unitário prova que o cálculo do limiar de calibração está certo, mas não que a chave `roll_right_deg` foi gravada no lugar certo.
- O de integração pega a troca de chaves no `SharedPreferences`, mas não que o botão "Salvar" está desabilitado durante o salvamento.
- O funcional pega o botão, mas não que o usuário não encontra a tela de configuração.
- O de usabilidade pega isso — e só ele mede latência percebida em uso real.

## 2. Decisão: nenhuma dependência de teste nova

Todo o conjunto foi construído com o que **já estava declarado** no `libs.versions.toml`:

| Biblioteca | Versão | Já existia? |
| --- | --- | --- |
| `junit:junit` | 4.13.2 | Sim |
| `io.mockk:mockk` | 1.13.13 | Sim |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | 1.9.0 | Sim (estava declarada e sem uso) |
| `androidx.test.ext:junit` | 1.2.1 | Sim (estava declarada e sem uso) |
| `androidx.compose.ui:ui-test-junit4` | via BOM 2024.11.00 | Sim (estava declarada e sem uso) |

A única alteração no `build.gradle.kts` foi expor a `coroutines-test` também ao source set instrumentado:

```kotlin
androidTestImplementation(libs.coroutines.test)
```

**Justificativa da escolha:** Robolectric e MockWebServer resolveriam duas lacunas (ver §5), mas cada dependência nova precisa ser defendida na banca. O `androidTest` cobre os mesmos casos rodando no Android de verdade — que é uma evidência mais forte, ao custo de precisar de emulador. Optou-se pela evidência mais forte e pelo grafo de dependências menor.

### 2.1 Ferramenta escolhida por nível, e o que foi descartado

| Nível | Escolhido | Descartado | Por quê |
| --- | --- | --- | --- |
| Unitário | JUnit 4 | JUnit 5 (Jupiter) | O runner instrumentado (`AndroidJUnit4`) é construído sobre JUnit 4. Adotar o 5 exigiria um plugin Gradle de terceiros e deixaria os dois source sets em frameworks diferentes |
| Unitário | MockK | Mockito | Classes Kotlin são `final` por padrão e o Mockito precisaria do `mock-maker-inline`; o MockK também mocka função `suspend` sem configuração extra, o que aqui é a regra e não a exceção |
| Unitário | kotlinx-coroutines-test | `Thread.sleep` real | Dois ViewModels fazem polling infinito. Com tempo virtual o ciclo avança em microssegundos; com espera real cada teste levaria segundos e ficaria instável |
| Unitário | `StateFlow.value` + `runCurrent()` | Turbine | Todos os fluxos expostos são `StateFlow` (quentes, com valor corrente). O Turbine resolveria `Flow` frio, que não usamos — dependência nova sem problema correspondente |
| Integração | Dispositivo real (AndroidJUnit4) | Robolectric | O `EncryptedSharedPreferences` depende do Android Keystore, que é hardware-backed. Simular na JVM testaria o simulador, não a cifragem — e a cifragem é justamente a regra que precisa de prova |
| Integração | Room in-memory | Banco em arquivo | Isola cada teste sem resíduo de execução anterior e dispensa limpeza manual |
| Funcional | Compose UI Test | Espresso | A interface é 100 % Compose; os matchers do Espresso não enxergam nós Compose |
| Funcional | `createComposeRule` | `createAndroidComposeRule` | As telas são *state-hoisted*: recebem estado e lambdas. Sem Activity e sem Hilt no teste, ele fica mais rápido e isola a tela de verdade |
| Funcional | — | UI Automator | Só seria necessário para verificar a entrega do comando ao app do Spotify. O MVP fala com a Web API, então esse salto entre apps está fora de escopo |
| Usabilidade | Heurísticas de Nielsen | Só teste com usuário | Avaliação heurística acha problema sem depender de recrutar o público-alvo, que é difícil de recrutar. Roda antes e barateia a sessão com usuário real |
| Usabilidade | Sessão moderada + SUS | Teste remoto não moderado | Latência percebida e esforço motor precisam ser observados. O SUS ainda dá um número comparável (média de referência: 68) |

**Critério que guiou todas as linhas:** preferir a ferramenta que já estava no grafo de dependências e que produz a evidência mais forte. Cada biblioteca nova precisa ser defendida na banca, e "roda no Android de verdade" é um argumento melhor do que "roda mais rápido".

## 3. Cobertura por camada

### 3.1 Visão computacional — 57 testes unitários

| Classe | Testes | O que garante |
| --- | --- | --- |
| `CaptureQualityEvaluator` | 18 | Luz, enquadramento e nº de rostos viram uma mensagem acionável; iluminação tem prioridade sobre "rosto não detectado" porque é a causa raiz |
| `GestureClassifier` | 15 | Cada gesto do vocabulário é classificado; polaridade invertida é respeitada; piscada tem prioridade sobre pose |
| `HeadPoseEstimator` | 10 | Roll/pitch/yaw a partir de landmarks e da matriz de transformação; eixos desacoplados |
| `HeadGestureStabilizer` | 9 | Captura de baseline, histerese, re-armamento e frames de sustentação |
| `NodDetector` | 5 | Ciclo de aceno em janela deslizante e cooldown |

Todas essas classes são Kotlin puro e recebem a configuração pelo construtor — é o que as torna testáveis sem câmera.

`CaptureQualityEvaluator` ilustra bem a costura: quem **mede** o frame é o `CalibrationPoseAnalyzer` (Android, MediaPipe, Bitmap, não testável na JVM); quem **decide** o que dizer ao usuário recebe só três números e é 100 % testável. Separar medição de política foi o que permitiu cobrir toda a regra sem emulador.

### 3.2 Domínio — 28 testes unitários

| Classe | Testes | Destaque |
| --- | --- | --- |
| `CalibrationThresholdCalculator` | 11 | Razão de 75 %, mínimos por eixo, polaridade aprendida, piso e teto do aceno |
| `CalibrationThresholds` | 8 | Trava os valores padrão e a coerência com os mínimos de segurança |
| `GestureDisplayNames` | 9 | Round-trip rótulo ↔ ação e unicidade dos rótulos |

O teste de unicidade dos rótulos merece destaque: dois rótulos iguais fariam `toSpotifyActionOrNull` devolver sempre a primeira ação da lista, remapeando o gesto do usuário para o comando errado — sem erro de compilação e sem exceção em runtime.

### 3.3 Dados — 10 unitários + 21 de integração

| Classe | Unitário | Integração | Destaque |
| --- | --- | --- | --- |
| `GestureMappingRepositoryImpl` | 10 | 7 | `restoreDefaults` limpa **antes** de gravar (ordem invertida deixaria o usuário sem nenhum gesto) |
| `GestureMappingDao` | — | 6 | Upsert substitui em vez de duplicar; ação nula persiste |
| `CalibrationRepositoryImpl` | — | 8 | Round-trip dos 11 limiares com valores todos distintos, para flagrar troca de chaves |

### 3.4 Spotify — 36 unitários + 12 de integração

| Classe | Testes | Destaque |
| --- | --- | --- |
| `SpotifyCommandRepositoryImpl` | 11 | Mensagens exatas de 403 (falta Premium) e 404 (nenhum dispositivo) — são exibidas direto ao usuário |
| `AlbumItem.bestImageUrl` | 9 | Escolhe a capa de resolução mais próxima; baixar a maior desperdiça banda num app que já roda visão computacional |
| `SpotifyPlayerRepositoryImpl` | 7 | 204 significa "nada tocando", não erro |
| `SpotifyTokens` | 5 | Fronteira do `isExpired()` |
| `PkceUtils` | 4 | Verificador de 86 caracteres base64url; desafio é SHA-256 do verificador |
| `SpotifyTokenStore` | 12 (integração) | Cifragem real via Keystore; **`state` do OAuth é de uso único** (proteção contra replay) |

### 3.5 Apresentação — 55 unitários + 51 funcionais

| ViewModel | Unitários | Tela | Funcionais |
| --- | --- | --- | --- |
| `PlayerAtivoViewModel` | 24 | `PlayerAtivoScreen` | 16 |
| `GestureConfigViewModel` | 11 | `GestureConfigScreen` | 10 |
| `HomeViewModel` | 11 | `HomeScreen` | 9 |
| `LoginViewModel` | 9 | `LoginScreen` | 8 |
| — | — | Acessibilidade (4 telas) | 8 |

O teste mais importante do conjunto está em `PlayerAtivoViewModelTest`: valida que o gesto detectado é traduzido em comando **lendo o mapeamento do banco**, que um gesto sem ação mapeada é ignorado em silêncio, e que remapear em tempo de execução muda o comando disparado. É a prova de que a regra "não existe mapeamento fixo no código" se sustenta.

Os testes funcionais são possíveis sem Hilt porque todas as telas são *state-hoisted*: recebem `uiState` e lambdas, sem conhecer ViewModel.

## 4. Detalhes técnicos que valem citar

### 4.1 Polling infinito e tempo virtual

`HomeViewModel` e `PlayerAtivoViewModel` fazem polling infinito (`while (isActive) { … delay(n) }`). Nesses testes **não** se pode usar `advanceUntilIdle()`: a fila do scheduler nunca esvazia e o teste roda para sempre. O padrão usado é `runCurrent()` para executar o que está agendado agora, e `advanceTimeBy(n) + runCurrent()` para avançar um ciclo por vez.

### 4.2 Animação infinita e Compose Test

`PlayerAtivoScreen` tem um ponto verde pulsante (`rememberInfiniteTransition`). Com o avanço automático do relógio, `waitForIdle` nunca considera a tela ociosa. Os testes dessa tela desligam o avanço automático (`mainClock.autoAdvance = false`), o que mantém a recomposição funcionando sem rodar a animação.

### 4.3 Teste de acessibilidade automatizado

`AccessibilityTest` varre a árvore de semântica de cada tela e verifica duas regras objetivas:

1. todo nó clicável tem `contentDescription` ou texto não vazio (senão o TalkBack anuncia apenas "botão");
2. todo alvo de toque mede ao menos 48dp × 48dp.

O que não é automatizável — contraste percebido, ordem de foco fazer sentido, tempo real de navegação com leitor de tela — está no roteiro de usabilidade.

## 5. Lacunas conhecidas

Declarar na defesa, com a justificativa:

| Item | Por que não está coberto | Mitigação |
| --- | --- | --- |
| `SpotifyAuthInterceptor` (401 → refresh, 429 → `Retry-After`) | Exigiria MockWebServer; além disso o caminho 429 usa `Thread.sleep`, o que tornaria o teste lento por construção | Teste manual registrado em [testes-manuais.md](testes-manuais.md) |
| `CalibrationViewModel` | Instancia `CalibrationPoseAnalyzer` (MediaPipe nativo) no `init`, e toda a máquina de estados é `private` | Cobertura indireta via `CalibrationThresholdCalculator` (11 testes) e `CalibrationRepositoryImpl` (8 testes). Testável após extrair um `CalibrationSession` puro para `domain/` |
| `CalibrationScreen` | Dispara o diálogo de permissão de câmera num `LaunchedEffect`, que trava o teste instrumentado | Tarefa T2 do roteiro de usabilidade |
| `GestureProcessor`, `CalibrationPoseAnalyzer` | MediaPipe nativo não carrega na JVM e exige câmera real | Tarefas T4 e T5 do roteiro de usabilidade |
| Cobertura de código (JaCoCo/Kover) | Não configurada | Contagem por classe nesta tabela |

## 6. Avaliação heurística e melhorias identificadas

### 6.1 Heurísticas de Nielsen — resultado

Avaliação conduzida sobre as cinco telas implementadas. Sete heurísticas atendidas, três parciais, uma não atendida.

| # | Heurística | Atende | Evidência resumida |
| --- | --- | --- | --- |
| 1 | Visibilidade do status | Sim | Indicador de carregamento, selo "Spotify Conectado", ponto pulsante de rastreamento, pílula do gesto reconhecido, badge de qualidade da captura |
| 2 | Sistema e mundo real | Sim | Vocabulário em português natural; *roll*, *pitch* e *yaw* não aparecem na interface |
| 3 | Controle e liberdade | **Parcial** | Não há desfazer após salvar um mapeamento (M01) |
| 4 | Consistência e padrões | Sim | *Design tokens* centralizados; ícones direcionais espelhados em RTL |
| 5 | Prevenção de erros | **Parcial** | Volume saturado, botão travado durante gravação, cooldown entre gestos — mas "Restaurar padrões" não pede confirmação (M01) |
| 6 | Reconhecimento vs. memorização | Sim | Os 9 gestos listados com ícone e ação vigente visível |
| 7 | Flexibilidade e eficiência | Sim | Mapeamento totalmente personalizável; botões manuais como caminho alternativo |
| 8 | Estético e minimalista | Sim | Tema escuro, hierarquia tipográfica clara, poucas ações por tela |
| 9 | Recuperação de erros | **Parcial** | Mensagens específicas para 403/404, mas falha ao buscar a faixa é silenciosa (M02) |
| 10 | Ajuda e documentação | **Não** | Sem tela de ajuda, onboarding ou aviso de pré-requisitos (M03) |

As heurísticas 3 e 7 pesam mais neste produto: o usuário tem limitação motora e pode não conseguir desfazer um erro com um toque rápido.

### 6.2 Melhorias identificadas

Consolidado dos quatro níveis. Prioridade pelo custo de não corrigir.

| ID | Melhoria | Origem | Prioridade |
| --- | --- | --- | --- |
| **M08** | **Exclusão dos dados de calibração pelo app.** O projeto declara o direito de eliminação (LGPD) como regra inegociável e o roteiro de usabilidade já tem a tarefa T6 para exercitá-lo, mas a função não existe: só há `SpotifyTokenStore.clearAll()`, sem caminho na interface | Requisito × código | **Alta** — requisito legal declarado e não cumprido; a tarefa T6 não pode ser executada hoje |
| **M01** | Diálogo de confirmação antes de "Restaurar padrões", que hoje descarta todas as personalizações num toque, sem desfazer | Heurísticas 3 e 5 | Alta |
| **M07** | Executar os 84 testes instrumentados. Estão implementados e compilam, mas nunca rodaram por falta de dispositivo/emulador | Cobertura | Alta |
| **M02** | Tratar o caminho de falha ao buscar a faixa atual, hoje descartado em silêncio | Heurística 9 | Média |
| **M03** | Onboarding com os pré-requisitos: Spotify aberto e ativo, conta Premium para controle, boa iluminação | Heurística 10 | Média |
| **M04** | Extrair um `CalibrationSession` puro para `domain/`, tornando a máquina de estados da calibração testável na JVM | Lacuna §5 | Média |
| **M09** | Levar o feedback de iluminação para a tela de rastreamento — hoje só existe na calibração, mas a luz cai igual durante o uso | Cobertura | Média |
| **M05** | Cobrir `SpotifyAuthInterceptor` (401 → refresh, 429 → `Retry-After`); exigiria MockWebServer | Lacuna §5 | Baixa |
| **M06** | Configurar cobertura de código (JaCoCo ou Kover) para substituir a contagem manual por classe | Lacuna §5 | Baixa |

M08 é a mais séria: não é um defeito encontrado por teste, e sim um requisito declarado no próprio projeto que nenhum teste cobria porque a funcionalidade não foi implementada. Foi a avaliação cruzada entre o roteiro de usabilidade e o código que expôs a lacuna — um argumento concreto, para a defesa, de por que os quatro níveis não se substituem.

## 7. Como executar

```bash
# Gate obrigatório antes de qualquer merge
./gradlew ktlintCheck detekt test

# Só os unitários, com relatório HTML
./gradlew testDebugUnitTest
# → app/build/reports/tests/testDebugUnitTest/index.html

# Integração + funcionais (exige emulador ou aparelho, API 29+, ABI x86_64 ou arm64-v8a)
./gradlew connectedDebugAndroidTest
# → app/build/reports/androidTests/connected/index.html
```

> O `abiFilters` do projeto inclui apenas `arm64-v8a` e `x86_64` (limitação dos binários do MediaPipe). Um emulador `x86` de 32 bits não roda o app.

A CI (`.github/workflows/android.yml`) executa `ktlintCheck`, `detekt` e `test` a cada push. Os testes instrumentados são executados manualmente — subir emulador na CI seria uma mudança de infraestrutura fora do escopo atual.
