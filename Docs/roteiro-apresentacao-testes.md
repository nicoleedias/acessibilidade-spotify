# Roteiro de apresentação dos testes — Nodify

Guia para rodar os testes ao vivo e mostrar o código correspondente.
Cada etapa traz **o arquivo de teste**, **o arquivo testado** e **o trecho a abrir na tela**.

---

## Antes de começar

```bash
cd C:\Users\pedro\acessibilidade-spotify
./gradlew testDebugUnitTest -Pdemo
```

**61 testes · 4 classes · ~10 segundos · 100 % de sucesso.**

Cada teste aparece na tela com o nome por extenso, em português. O conjunto ignora o cache do
Gradle de propósito: sem isso o build responderia `FROM-CACHE` e pareceria que nada rodou.

> **Rode uma vez antes de apresentar**, com o projeto já aberto. A primeira execução do dia compila
> o projeto e leva ~25 s; as seguintes levam ~10 s.

> **Não rode `connectedAndroidTest`.** Os 84 testes instrumentados exigem celular conectado e
> falham na hora sem aparelho.

---

## A narrativa: quatro etapas da jornada do usuário

| # | Etapa | Classe de teste | Testes | UC |
| --- | --- | --- | --- | --- |
| 1 | Detectar o gesto | `GestureClassifierTest` | 15 | UC01 |
| 2 | Calibrar a amplitude | `CalibrationThresholdCalculatorTest` | 11 | UC02 |
| 3 | Customizar o mapeamento | `GestureConfigViewModelTest` | 11 | UC03 |
| 4 | **O gesto vira comando** | `PlayerAtivoViewModelTest` | 24 | UC01 |

---

## Etapa 1 — Detectar o gesto

| | |
| --- | --- |
| **Arquivo de teste** | `app/src/test/java/com/sac/acessibilidade/vision/GestureClassifierTest.kt` (211 linhas) |
| **Código testado** | `app/src/main/java/com/sac/acessibilidade/vision/GestureClassifier.kt` (81 linhas) |
| **Apoio** | `domain/gesture/Gesture.kt` · `domain/gesture/CalibrationThresholds.kt` |

**Trecho para mostrar — `GestureClassifier.kt`, linhas 19–30:**

```kotlin
fun classify(
    landmarks: List<NormalizedLandmark>,
    blendshapes: List<Category>?,
    thresholds: CalibrationThresholds,
): Gesture? {
    // Piscar tem prioridade — é intencional e não ambíguo com gestos de cabeça
    val blinkGesture = classifyBlink(blendshapes, thresholds.blinkThreshold)
    if (blinkGesture != null) return blinkGesture

    val pose = HeadPoseEstimator.estimate(landmarks) ?: return null
    return classifyHeadPose(pose, thresholds)
}
```

**O que dizer:** a classe não conhece câmera nem MediaPipe — recebe pontos do rosto e limiares, e
devolve um gesto. É isso que permite testá-la na JVM, sem celular.

**Testes que aparecem na tela:**
- `detecta TILT_HEAD_RIGHT quando o usuario inclina para a direita`
- `piscada tem prioridade sobre gesto de cabeca`
- `polaridade invertida classifica TILT_HEAD_RIGHT com roll negativo`

**Se perguntarem sobre a polaridade invertida:** a câmera frontal espelha a imagem, e o sinal do
eixo muda de aparelho para aparelho. Em vez de fixar no código, o sistema aprende o sinal durante a
calibração — e há teste para as duas polaridades.

---

## Etapa 2 — Calibrar a amplitude de cada usuário

| | |
| --- | --- |
| **Arquivo de teste** | `app/src/test/java/com/sac/acessibilidade/domain/gesture/CalibrationThresholdCalculatorTest.kt` (133 linhas) |
| **Código testado** | `app/src/main/java/com/sac/acessibilidade/domain/gesture/CalibrationThresholdCalculator.kt` (86 linhas) |
| **Apoio** | `domain/gesture/CalibrationThresholds.kt` |

**Trecho para mostrar — `CalibrationThresholdCalculator.kt`, linhas 60 e 74:**

```kotlin
): Float = (abs(signedPeak) * THRESHOLD_RATIO).coerceAtLeast(minimum)

const val THRESHOLD_RATIO = 0.75f
```

**O que dizer:** o limiar fica em 75 % do movimento máximo que a pessoa conseguiu fazer. Quem tem
pouca amplitude não precisa repetir o esforço máximo para o app reconhecer o gesto — é o núcleo da
acessibilidade do projeto. O `coerceAtLeast` garante um mínimo de segurança por eixo, senão um
movimento involuntário dispararia comando.

**Testes que aparecem na tela:**
- `aplica 75 por cento da magnitude do pico medido`
- `respeita o minimo de seguranca quando o pico e pequeno`
- `polaridade negativa quando o estimador inverte o eixo`

---

## Etapa 3 — Customizar o mapeamento gesto → ação

| | |
| --- | --- |
| **Arquivo de teste** | `app/src/test/java/com/sac/acessibilidade/ui/screens/GestureConfigViewModelTest.kt` (223 linhas) |
| **Código testado** | `app/src/main/java/com/sac/acessibilidade/ui/screens/GestureConfigViewModel.kt` (95 linhas) |
| **Apoio** | `domain/gesture/GestureMappingRepository.kt` · `domain/gesture/GestureDisplayNames.kt` |

**Trecho para mostrar — `GestureConfigViewModel.kt`, linhas 56–67:**

```kotlin
fun save(onDone: () -> Unit) {
    viewModelScope.launch {
        _uiState.update { it.copy(isSaving = true) }
        val domainMap =
            _uiState.value.mappings.associate { item ->
                item.gesture to item.selectedAction.toSpotifyActionOrNull()
            }
        repository.saveMappings(domainMap)
        _uiState.update { it.copy(isSaving = false) }
        onDone()
    }
}
```

**O que dizer:** o usuário escolhe qual gesto dispara qual comando. Isso não é conveniência: um
usuário que não consegue virar o rosto para a esquerda pode mapear esse comando para outro gesto.
Citar a **WCAG 2.1 SC 2.5.6** se perguntarem a justificativa.

**Testes que aparecem na tela:**
- `atualizar mapeamento altera apenas a linha do gesto informado`
- `salvar converte sem acao em nulo`
- `restaurar padroes delega ao repositorio`

---

## Etapa 4 — O gesto vira comando no Spotify  ← **o ponto alto**

| | |
| --- | --- |
| **Arquivo de teste** | `app/src/test/java/com/sac/acessibilidade/ui/screens/PlayerAtivoViewModelTest.kt` (485 linhas) |
| **Código testado** | `app/src/main/java/com/sac/acessibilidade/ui/screens/PlayerAtivoViewModel.kt` (192 linhas) |
| **Apoio** | `spotify/player/SpotifyCommandRepository.kt` · `spotify/player/SpotifyPlayerRepository.kt` · `vision/GestureProcessor.kt` |

**Trecho para mostrar — `PlayerAtivoViewModel.kt`, linhas 57–65:**

```kotlin
private fun collectGestures() {
    viewModelScope.launch {
        gestureProcessor.gestureFlow.collect { gesture ->
            val action = currentMapping[gesture] ?: return@collect
            showGestureFeedback(gesture, action)
            executeAction(action)
        }
    }
}
```

**O que dizer — abrir essa linha e apontar:**

```kotlin
val action = currentMapping[gesture] ?: return@collect
```

São oito palavras que sustentam a regra mais importante do projeto: **não existe mapeamento fixo no
código**. A ação vem do mapa carregado do banco. E o `?: return@collect` é o gesto sem ação
mapeada: sai em silêncio, sem disparar nada.

**Testes que aparecem na tela:**
- `aceno dispara tocar quando esta pausado`
- `virar rosto a direita avanca a faixa`
- `inclinar a direita aumenta o volume em cinco`
- `gesto sem acao mapeada e ignorado`
- `mapeamento customizado do usuario e respeitado`
- `remapeamento em tempo de execucao muda o comando disparado`
- `volume satura em cem`
- `falha de comando vira mensagem de erro na tela`

Os três do meio são a prova direta: o comando sai do banco, e trocar o mapeamento **durante a
execução** muda o comando disparado.

---

## Perguntas prováveis

**"Como vocês testam sem celular?"**
As camadas de regra de negócio não importam nada de Android. O `GestureClassifier` recebe números
e devolve um gesto; o `PlayerAtivoViewModel` recebe repositórios por construtor, substituídos por
simulações nos testes. Por isso os 186 testes rodam na JVM em menos de 4 segundos.

**"E a interface, não é testada?"**
É, por 51 testes de Compose UI Test, mais 33 de integração com banco e Keystore. São 84 testes que
exigem aparelho ou emulador conectado — estão implementados e compilam, mas não rodam aqui.

**"Qual a cobertura?"**
22 % do aplicativo inteiro, 71,4 % do núcleo lógico. A diferença são 1.300 linhas de Compose, que
descrevem a tela e não têm lógica para testar. As camadas de domínio, dados, integração com o
Spotify e os quatro ViewModels estão em **100 %**. Relatório:
`./gradlew koverHtmlReportDebug`

**"Por que MockK e não Mockito?"**
Classes Kotlin são `final` por padrão; o Mockito precisaria de configuração extra. O MockK também
simula função `suspend` sem adaptação, e aqui isso é a regra.

---

## Comandos de apoio

```bash
# Suíte completa — 186 testes
./gradlew test

# Relatório visual de testes
start app\build\reports\tests\testDebugUnitTest\index.html

# Relatório de cobertura
./gradlew koverHtmlReportDebug
start app\build\reports\kover\htmlDebug\index.html

# Portão de qualidade completo
./gradlew ktlintCheck detekt test
```
