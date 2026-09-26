# Protocolo de Teste de Usabilidade — Nodify

**Sistema de Acessibilidade Cefálica para Spotify**

| Campo | Valor |
| --- | --- |
| Versão do protocolo | 1.0 |
| Data de elaboração | 26/08/2026 |
| Responsáveis | Pedro Cauã, Nicole |
| Casos de uso avaliados | UC01, UC02, UC03 |

---

## 1. Objetivo

Avaliar se um usuário consegue, sem treinamento prévio, conectar-se ao Spotify, calibrar o sistema e controlar a reprodução musical usando apenas movimentos de cabeça — e medir os requisitos não-funcionais do projeto em uso real, não em bancada.

Este protocolo complementa os testes automatizados (unitários, de integração e funcionais). Os automatizados provam que o software **funciona conforme especificado**; este prova que ele **serve para o usuário**.

## 2. Requisitos não-funcionais sob avaliação

| ID | Requisito | Meta | Como medir |
| --- | --- | --- | --- |
| RNF01 | Latência gesto → ação | ≤ 500 ms | Cronômetro / gravação de tela a 60 fps, contando do início do movimento até a mudança visível no Spotify |
| RNF02 | Acurácia de reconhecimento | > 90 % | Gestos corretamente reconhecidos ÷ gestos tentados, em ambiente bem iluminado |
| RNF03 | Tempo de calibração | < 2 min | Cronômetro, do toque em "Calibrar Movimentos" até a tela de conclusão |
| RNF04 | Operação sem toque na tela | Tarefas T4 e T5 completáveis só com a cabeça | Observação direta |
| RNF05 | Privacidade | Nenhum frame gravado ou transmitido | Inspeção do armazenamento do dispositivo e do tráfego de rede após a sessão |

## 3. Participantes

### 3.1 Perfil desejado

| Critério | Especificação |
| --- | --- |
| Quantidade | 5 participantes (número suficiente para revelar a maioria dos problemas de usabilidade em avaliação qualitativa) |
| Perfil prioritário | Ao menos 1 pessoa com limitação motora de membros superiores |
| Perfil complementar | Pessoas sem limitação motora, para linha de base de tempo e acurácia |
| Pré-requisito | Conta Spotify ativa; familiaridade mínima com smartphone Android |
| Exclusão | Uso de óculos escuros ou objeto que oculte o rosto durante a sessão |

> **Limitação metodológica a declarar:** se não for possível recrutar participante com limitação motora real, isso deve ser registrado explicitamente no TCC. Testar acessibilidade apenas com usuários sem deficiência produz resultados otimistas e não valida o público-alvo.

### 3.2 Registro dos participantes

| ID | Idade | Perfil motor | Experiência com Android | Usa leitor de tela? | Data da sessão |
| --- | --- | --- | --- | --- | --- |
| P1 |  |  |  |  |  |
| P2 |  |  |  |  |  |
| P3 |  |  |  |  |  |
| P4 |  |  |  |  |  |
| P5 |  |  |  |  |  |

## 4. Ambiente e materiais

| Item | Especificação |
| --- | --- |
| Dispositivo | Android 10+ (API 29+), câmera frontal ≥ 720p |
| Iluminação | Ambiente interno bem iluminado, luz frontal ao rosto, sem contraluz |
| Posicionamento | Aparelho apoiado, rosto a 40–60 cm da câmera |
| Rede | Wi-Fi estável |
| Spotify | App instalado, logado e com reprodução ativa em outro dispositivo ou no próprio aparelho |
| Registro | Gravação de tela + cronômetro + formulário de observação |

> **Atenção:** alguns endpoints da Spotify Web API (`/me/player/*`) exigem conta **Premium** e um dispositivo ativo. Registrar o tipo de conta de cada participante — uma falha por conta Free não é falha de usabilidade.

## 5. Roteiro de tarefas

Cada tarefa é lida em voz alta ao participante, sem instruções adicionais. O facilitador só intervém se o participante ficar bloqueado por mais de 2 minutos (registrar como "tarefa não concluída sem ajuda").

### T1 — Conectar ao Spotify (UC01)

> "Abra o aplicativo e conecte a sua conta do Spotify."

| Métrica | Valor |
| --- | --- |
| Concluiu sem ajuda? (S/N) | |
| Tempo (mm:ss) | |
| Nº de tentativas | |
| Observações | |

### T2 — Calibrar os movimentos (UC02)

> "Ajuste o aplicativo aos seus movimentos de cabeça."

| Métrica | Valor | Meta |
| --- | --- | --- |
| Concluiu sem ajuda? (S/N) | | — |
| **Tempo total (mm:ss)** | | **< 2 min (RNF03)** |
| Precisou repetir alguma etapa? Qual? | | — |
| A instrução de cada etapa ficou clara? | | — |
| Observações | | — |

### T3 — Trocar um mapeamento de gesto (UC03)

> "Faça com que virar o rosto para a direita passe a diminuir o volume, em vez de pular a faixa."

| Métrica | Valor |
| --- | --- |
| Concluiu sem ajuda? (S/N) | |
| Tempo (mm:ss) | |
| Encontrou a tela de configuração sozinho? | |
| Entendeu que só pode haver uma ação por gesto? | |
| Observações | |

### T4 — Controlar a reprodução só com a cabeça (UC01)

> "Sem tocar na tela, pause a música, depois volte a tocar e em seguida pule para a próxima faixa."

Registrar **cada** tentativa de gesto:

| # | Gesto tentado | Reconhecido? | Ação executada foi a esperada? | Latência (ms) |
| --- | --- | --- | --- | --- |
| 1 | | | | |
| 2 | | | | |
| 3 | | | | |
| 4 | | | | |
| 5 | | | | |

| Métrica agregada | Valor | Meta |
| --- | --- | --- |
| **Acurácia** = reconhecidos ÷ tentados | | **> 90 % (RNF02)** |
| **Latência média** | | **≤ 500 ms (RNF01)** |
| Falsos positivos (ação sem gesto intencional) | | 0 |

> **Falso positivo importa mais que falha de detecção.** Pular uma faixa sem o usuário pedir é mais grave do que não reconhecer um gesto: quebra a confiança no sistema. Registrar cada ocorrência com o contexto (o que o usuário estava fazendo).

### T5 — Ajustar o volume só com a cabeça (UC01)

> "Sem tocar na tela, aumente o volume e depois diminua."

| Métrica | Valor |
| --- | --- |
| Concluiu sem ajuda? (S/N) | |
| Nº de tentativas até acertar | |
| O indicador visual do gesto ajudou? | |
| Observações | |

### T6 — Revogar os dados (LGPD)

> "Apague seus dados de calibração do aplicativo."

| Métrica | Valor |
| --- | --- |
| Encontrou a opção? (S/N) | |
| Tempo (mm:ss) | |
| Observações | |

## 6. Roteiro de navegação com TalkBack

Executado com o TalkBack **ligado**, em uma sessão à parte. Percorrer cada tela apenas com gestos de leitor de tela.

| Tela | Todos os elementos são anunciados? | Anúncio é compreensível? | Ordem de foco é lógica? | Observações |
| --- | --- | --- | --- | --- |
| Login | | | | |
| Início | | | | |
| Calibração | | | | |
| Configurar Gestos | | | | |
| Rastreamento Ativo | | | | |

Pontos de atenção específicos:

- O seletor de ação da tela de gestos anuncia **qual gesto** está sendo configurado, e não apenas o nome da ação?
- A pílula de feedback de gesto na tela de rastreamento é anunciada quando aparece?
- O botão "Parar Rastreamento" é alcançável sem passar por todos os controles de reprodução?

## 7. Questionário SUS (System Usability Scale)

Aplicado ao fim da sessão. Escala: **1 = Discordo totalmente … 5 = Concordo totalmente**.

| # | Afirmação | P1 | P2 | P3 | P4 | P5 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | Eu usaria este sistema com frequência. | | | | | |
| 2 | Achei o sistema desnecessariamente complexo. | | | | | |
| 3 | Achei o sistema fácil de usar. | | | | | |
| 4 | Precisaria de ajuda técnica para conseguir usar este sistema. | | | | | |
| 5 | As funções do sistema estão bem integradas. | | | | | |
| 6 | Há muita inconsistência neste sistema. | | | | | |
| 7 | A maioria das pessoas aprenderia a usar este sistema rapidamente. | | | | | |
| 8 | Achei o sistema atrapalhado de usar. | | | | | |
| 9 | Senti-me confiante ao usar o sistema. | | | | | |
| 10 | Precisei aprender muita coisa antes de conseguir usar o sistema. | | | | | |

**Cálculo do escore:** para os itens ímpares, subtraia 1 da nota; para os pares, subtraia a nota de 5. Some os 10 valores e multiplique por 2,5. O resultado vai de 0 a 100.

| Participante | Escore SUS |
| --- | --- |
| P1 | |
| P2 | |
| P3 | |
| P4 | |
| P5 | |
| **Média** | |

> **Referência de interpretação:** a média histórica em estudos com SUS fica em torno de 68; abaixo disso costuma indicar problemas de usabilidade relevantes. Usar como referência, não como aprovação/reprovação.

## 8. Perguntas abertas

1. O que foi mais difícil de fazer no aplicativo?
2. Em algum momento o aplicativo fez algo que você não pediu? Descreva.
3. Você usaria este aplicativo no seu dia a dia? Por quê?

| Participante | P1 | P2 | P3 |
| --- | --- | --- | --- |
| P1 | | | |
| P2 | | | |
| P3 | | | |
| P4 | | | |
| P5 | | | |

## 9. Termo de Consentimento Livre e Esclarecido

> Modelo a ser impresso e assinado antes da sessão. Adequar ao formato exigido pela instituição.

**Pesquisa:** Nodify — Sistema de Acessibilidade Cefálica para Spotify — Trabalho de Conclusão de Curso.

Você está sendo convidado(a) a participar de um teste de usabilidade. Leia com atenção:

1. **O que você vai fazer.** Usar um aplicativo Android que controla a reprodução musical do Spotify por movimentos de cabeça, executando de 5 a 6 tarefas curtas. A sessão dura cerca de 30 minutos.

2. **Tratamento de imagem — ponto central.** O aplicativo usa a câmera frontal do aparelho para detectar os movimentos da sua cabeça. **Todo o processamento acontece dentro do próprio aparelho.** Nenhuma imagem, vídeo ou ponto facial é gravado em arquivo, enviado pela internet ou compartilhado com o Spotify, com os pesquisadores ou com terceiros. As imagens existem apenas na memória do aparelho durante o uso e são descartadas quadro a quadro.

3. **O que é registrado.** Apenas as anotações do observador (tempos, número de tentativas, comentários) e, se você autorizar, uma gravação da **tela** do aparelho — que mostra o aplicativo, não o seu rosto.

   - ( ) Autorizo a gravação de tela  ( ) Não autorizo

4. **Dados pessoais coletados.** Idade, perfil de mobilidade e experiência prévia com smartphone. Esses dados são tratados de forma anonimizada (você será identificado apenas como "P1", "P2"…), usados exclusivamente para fins acadêmicos e mantidos pelo tempo necessário à conclusão e defesa do TCC.

5. **Base legal (LGPD, Lei 13.709/2018).** O tratamento se dá mediante o seu **consentimento**, nos termos do art. 7º, inciso I. Dados sobre condição de saúde/mobilidade são dados pessoais sensíveis (art. 5º, II) e recebem o mesmo tratamento anonimizado.

6. **Seus direitos.** Você pode interromper a participação a qualquer momento, sem justificativa e sem prejuízo. Pode solicitar acesso, correção ou eliminação dos seus dados a qualquer tempo, entrando em contato com os responsáveis pela pesquisa.

7. **Riscos e benefícios.** Não há riscos previstos além de eventual cansaço pelos movimentos repetidos de cabeça — você pode pausar quando quiser. Não há remuneração. O benefício é contribuir para o desenvolvimento de tecnologia assistiva.

Declaro que li e compreendi as informações acima e concordo em participar.

| Campo | |
| --- | --- |
| Nome do participante | |
| Assinatura | |
| Data | |
| Responsável pela aplicação | |

## 10. Consolidação dos resultados

Preencher após todas as sessões.

| RNF | Meta | Resultado medido | Atingiu? |
| --- | --- | --- | --- |
| RNF01 — Latência | ≤ 500 ms | | |
| RNF02 — Acurácia | > 90 % | | |
| RNF03 — Calibração | < 2 min | | |
| RNF04 — Operação sem toque | T4 e T5 concluídas | | |
| RNF05 — Privacidade | Nenhum frame persistido | | |

| Tarefa | Taxa de conclusão sem ajuda | Tempo médio |
| --- | --- | --- |
| T1 — Conectar | | |
| T2 — Calibrar | | |
| T3 — Remapear gesto | | |
| T4 — Controlar reprodução | | |
| T5 — Ajustar volume | | |
| T6 — Revogar dados | | |

### Problemas identificados

| # | Problema observado | Participantes afetados | Gravidade (baixa/média/alta) | Ação proposta |
| --- | --- | --- | --- | --- |
| 1 | | | | |
| 2 | | | | |
| 3 | | | | |

### Conclusão

_A ser redigida após a consolidação, respondendo: o sistema atinge seu objetivo de permitir controle musical acessível por gestos cefálicos? Quais ajustes são necessários antes de um uso real?_
