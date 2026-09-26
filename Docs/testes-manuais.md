# Registro de Testes Manuais — Nodify

Documento de registro das execuções manuais realizadas em dispositivo real. Cada seção nova deve ser adicionada no topo, com data.

Os testes automatizados estão descritos em [estrategia-de-testes.md](estrategia-de-testes.md); este arquivo cobre apenas o que **não** dá para automatizar: latência real, acurácia em iluminação variável, comportamento com o Spotify em segundo plano e navegação com TalkBack ligado.

---

## Modelo de registro

### Sessão — DD/MM/AAAA

| Campo | Valor |
| --- | --- |
| Build / commit | |
| Dispositivo | |
| Versão do Android | |
| Conta Spotify | Free / Premium |
| Iluminação | |
| Executor | |

#### Latência gesto → ação (RNF01, meta ≤ 500 ms)

Medir com gravação de tela a 60 fps, contando os quadros entre o início do movimento e a mudança visível.

| # | Gesto | Ação esperada | Latência (ms) | Dentro da meta? |
| --- | --- | --- | --- | --- |
| 1 | | | | |
| 2 | | | | |
| 3 | | | | |
| 4 | | | | |
| 5 | | | | |

**Média:** ___ ms

#### Acurácia por gesto (RNF02, meta > 90 %)

10 repetições de cada gesto mapeado, em iluminação adequada.

| Gesto | Tentativas | Reconhecidos | Falsos positivos | Acurácia |
| --- | --- | --- | --- | --- |
| Inclinar para Direita | 10 | | | |
| Inclinar para Esquerda | 10 | | | |
| Virar para Direita | 10 | | | |
| Virar para Esquerda | 10 | | | |
| Aceno (sim) | 10 | | | |

**Acurácia global:** ___ %

#### Calibração (RNF03, meta < 2 min)

| Campo | Valor |
| --- | --- |
| Tempo total | |
| Etapas que precisaram ser repetidas | |
| Polaridade aprendida ficou correta? (direita = direita) | |

#### Condições adversas

| Condição | Comportamento observado | Aceitável? |
| --- | --- | --- |
| Iluminação fraca | | |
| Contraluz (janela atrás do usuário) | | |
| Usuário de óculos | | |
| Rosto parcialmente fora do enquadramento | | |
| Spotify em segundo plano | | |
| Sem conexão de rede | | |
| Conta Free tentando controlar reprodução | | |

#### Privacidade (RNF05)

| Verificação | Resultado |
| --- | --- |
| Nenhum arquivo de imagem/vídeo criado em `/Android/data/com.sac.acessibilidade/` após a sessão | |
| Nenhuma requisição de rede além de `accounts.spotify.com` e `api.spotify.com` | |
| Logcat não contém coordenadas de landmarks | |
| Revogar dados de calibração pelo app apaga as preferências | |

#### Navegação com TalkBack

| Tela | Todos os elementos anunciados? | Ordem de foco lógica? | Problemas |
| --- | --- | --- | --- |
| Login | | | |
| Início | | | |
| Calibração | | | |
| Configurar Gestos | | | |
| Rastreamento Ativo | | | |

#### Problemas encontrados nesta sessão

| # | Descrição | Gravidade | Issue / commit de correção |
| --- | --- | --- | --- |
| 1 | | | |

---

## Histórico de sessões

_Nenhuma sessão registrada ainda._
