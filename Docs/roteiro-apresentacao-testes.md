# Apresentação dos testes — Nodify

## 1. O que você precisa

| | |
| --- | --- |
| Notebook com o projeto aberto | `C:\Users\pedro\acessibilidade-spotify` |
| Terminal | Git Bash ou PowerShell, na pasta do projeto |
| Internet | **Não** — as dependências já estão em cache |
| Celular / emulador | **Não** — os testes rodam na JVM |
| Spotify aberto | **Não** |

Só o notebook. Nada mais.

---

## 2. Comandos

**Antes de apresentar** (faça em casa, uma vez — demora ~1 minuto porque compila):

```bash
cd C:\Users\pedro\acessibilidade-spotify
./gradlew testDebugUnitTest -Pdemo
```

**Na frente do professor** (~11 segundos):

```bash
./gradlew testDebugUnitTest -Pdemo
```

O mesmo comando. Depois de compilado, roda em 11s e lista os 61 testes um a um na tela.

Resultado esperado no final:

```
BUILD SUCCESSFUL
```

### Se ele pedir a suíte completa

```bash
./gradlew test
```

186 testes, ~4 segundos.

### Se ele pedir a cobertura

```bash
./gradlew koverHtmlReportDebug
start app\build\reports\kover\htmlDebug\index.html
```

---

## 3. O que está sendo testado

61 testes, 4 classes, uma por etapa do produto:

| # | Etapa | Arquivo de teste | Arquivo testado |
| --- | --- | --- | --- |
| 1 | Detectar o gesto | `GestureClassifierTest.kt` | `vision/GestureClassifier.kt` |
| 2 | Calibrar o usuário | `CalibrationThresholdCalculatorTest.kt` | `domain/gesture/CalibrationThresholdCalculator.kt` |
| 3 | Customizar mapeamento | `GestureConfigViewModelTest.kt` | `ui/screens/GestureConfigViewModel.kt` |
| 4 | **Gesto vira comando** | `PlayerAtivoViewModelTest.kt` | `ui/screens/PlayerAtivoViewModel.kt` |

Caminhos completos:
- testes → `app/src/test/java/com/sac/acessibilidade/…`
- código → `app/src/main/java/com/sac/acessibilidade/…`

---

## 4. O código para mostrar

Abra **`app/src/main/java/com/sac/acessibilidade/ui/screens/PlayerAtivoViewModel.kt`**, linha 57:

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

**Aponte a linha 60** e diga:

> "A ação vem do mapa carregado do banco, não do código. Se o gesto não tem ação, sai em silêncio.
> É isso que permite o usuário usar só os gestos que ele consegue fazer."

Os testes que provam essa linha aparecem na tela quando você roda:
- `mapeamento customizado do usuario e respeitado`
- `remapeamento em tempo de execucao muda o comando disparado`
- `gesto sem acao mapeada e ignorado`

---

## 5. Perguntas prováveis

**"Como testam sem celular?"**
A camada de regra de negócio não importa nada de Android. O classificador recebe números e devolve
um gesto; o ViewModel recebe os repositórios por construtor, substituídos por simulações no teste.

**"Qual a cobertura?"**
71,4 % do núcleo lógico, 22 % do app inteiro. A diferença são 1.300 linhas de Compose, que descrevem
a tela e não têm lógica. Domínio, dados, integração com o Spotify e os 4 ViewModels estão em 100 %.

**"E a interface, é testada?"**
Sim — 51 testes de Compose UI Test e 33 de integração. Mas precisam de celular conectado e ainda não
foram executados. Está declarado na seção 5.3 do documento.

**"Por que MockK e não Mockito?"**
Classes Kotlin são `final` por padrão; o Mockito precisaria de configuração extra. O MockK também
simula função `suspend` sem adaptação.

---

## 6. Não faça

❌ **`./gradlew connectedAndroidTest`** — precisa de celular conectado. Falha na hora.

❌ Não rode pela primeira vez na frente dele. Compila e demora 1 minuto.
