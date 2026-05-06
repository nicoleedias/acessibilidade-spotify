**Universidade Católica de Brasília**
**Disciplina:** Análise e Projeto de Software

# SISTEMA DE ACESSIBILIDADE CEFÁLICA PARA SPOTIFY
## Especificação de Casos de Uso

**Autores:** Nicole Cardoso Dias, Pedro Cauã, Samuel Gomes, Rodrigo Barbosa, Ricardo Oliveira, Victor Salvador


**Professor:** Alexandre S. D. Santos

**Brasília 2026**

---

## 1 Introdução
Este documento de especificação funcional apresenta as características em termo das funcionalidades do software a ser construído. O foco do projeto é a acessibilidade digital, propondo o desenvolvimento de um sistema baseado em Machine Learning capaz de identificar movimentos faciais e traduzi-los em comandos de navegação no celular.

## 2 Objetivo e escopo
O projeto se destina a desenvolver um sistema voltado para a acessibilidade no streaming de música. O aplicativo irá capturar imagens da câmera em tempo real para identificar gestos específicos da cabeça (como inclinar para os lados ou acenar verticalmente). Esses gestos serão mapeados para funções do Spotify, como aumentar/diminuir volume, próxima faixa, faixa anterior e play/pause.

## 3 Requisitos não-funcionais relacionados
Requisitos não-funcionais que fazem parte do sistema e que deverão ser considerados:
* **Usabilidade:** Intuitivo para calibração em menos de 2 minutos.
* **Confiabilidade:** Taxa de acerto superior a 90% na detecção dos gestos em ambientes iluminados.
* **Segurança:** Processamento local (Edge Computing); não armazenamento ou envio de imagens para a nuvem.
* **Desempenho:** Tempo de resposta inferior a 500ms (baixa latência) entre o gesto e a ação.

## 4 Arquitetura e sistemas relacionados
O sistema será estruturado nas seguintes camadas:
* **Apresentação:** Jetpack Compose (Kotlin).
* **Processamento:** MediaPipe Face Mesh e OpenCV.
* **Integração:** REST API e Spotify Web API/Spotipy.
* **Dados:** SQLite.

## 5 Premissas e restrições
* O usuário deve possuir uma conta ativa no Spotify.
* O sistema depende de condições mínimas de iluminação para reconhecimento facial. Conexão ativa com internet (4G/5G ou Wi-Fi) exigida.
* O dispositivo deve possuir sistema operacional Android 10 ou superior, com câmera frontal de resolução mínima de 720p.

---

## 6 Especificação Funcional (Casos de Uso)

### UC01 - Controlar Reprodução de Mídia via Gestos
* **Atores Envolvidos:** Usuários Finais (Pessoas com deficiência motora ou mobilidade reduzida).
* **Breve Descrição:** O usuário realiza gestos cefálicos (como inclinar a cabeça ou acenar verticalmente) para acionar comandos de reprodução musical no Spotify (Play, Pause, Pular Faixa, etc.) com autonomia.
* **Pré-condições:** O dispositivo deve possuir câmera com resolução mínima de 720p e conexão ativa com a internet. O ambiente precisa de condições mínimas de iluminação. O usuário deve ter uma conta ativa no Spotify.
* **Pós-condições:** O comando é executado no Spotify em até 500ms e o sistema apresenta um feedback visual ou sonoro de sucesso.
* **Fluxo Principal:**
  1. O sistema gerencia o buffer da câmera para iniciar a captura de vídeo em tempo real.
  2. A biblioteca MediaPipe Face Mesh realiza o rastreamento dos marcos faciais.
  3. O usuário realiza um gesto intencional mapeado.
  4. O modelo de Machine Learning classifica o gesto e o associa à função correspondente (ex: Próxima faixa).
  5. O sistema envia uma requisição REST para a Spotify Web API.
  6. A ação é executada no aplicativo Spotify.
  7. A interface gráfica exibe o feedback visual confirmando o comando.
* **Fluxos Alternativos (Erros e Exceções):**
  * **FA01 - Falso Positivo / Movimento Involuntário:** O modelo de Machine Learning classifica o movimento como não intencional. O sistema ignora o gesto e nenhuma ação é acionada.
  * **FA02 - Iluminação Inadequada (Erro):** A câmera não consegue identificar o rosto. O sistema tenta aplicar filtros de normalização de luz. Se a falha persistir, o sistema emite um alerta visual/sonoro solicitando melhor iluminação.
  * **FA03 - Falha de Conexão (Exceção):** O gesto é reconhecido, mas não há conexão com a internet. A requisição REST falha. O sistema exibe um aviso de "Sem Conexão" e cancela a ação.

### UC02 - Calibrar Sensibilidade e Movimentos
* **Atores Envolvidos:** Usuários Finais.
* **Breve Descrição:** O usuário utiliza uma interface específica para configurar a amplitude de seus movimentos antes de iniciar o uso, permitindo que o sistema se adapte à sua fisionomia.
* **Pré-condições:** O aplicativo deve estar aberto na interface View (Jetpack Compose) e o rosto do usuário deve estar enquadrado pela câmera.
* **Pós-condições:** Os níveis de sensibilidade calibrados são salvos localmente no banco de dados SQLite.
* **Fluxo Principal:**
  1. O usuário acessa o Módulo de Calibração na interface do aplicativo.
  2. O sistema exibe o feed da câmera para auxiliar o posicionamento.
  3. O sistema solicita que o usuário mantenha o rosto neutro para definir um estado base.
  4. O sistema solicita que o usuário faça o movimento desejado para as direções disponíveis.
  5. O MediaPipe Face Mesh registra as distâncias dos marcos faciais para cada extremo alcançado.
  6. O sistema processa os dados e salva os parâmetros de sensibilidade no banco de dados SQLite.
  7. O sistema exibe uma mensagem de calibração concluída com sucesso.
* **Fluxos Alternativos (Erros e Exceções):**
  * **FA01 - Perda de Rastreamento:** O rosto do usuário sai do enquadramento da câmera durante o processo de calibração. O sistema pausa a captura e exibe um aviso pedindo para recentralizar o rosto.
  * **FA02 - Amplitude Insuficiente:** O sistema detecta que o movimento feito é sutil demais para ser diferenciado de movimentos naturais. O sistema sugere realizar um movimento levemente mais amplo, respeitando os limites físicos do usuário.

### UC03 - Customizar Mapeamento de Comandos
* **Atores Envolvidos:** Usuários Finais.
* **Breve Descrição:** O usuário acessa um painel para escolher qual gesto específico (ex: inclinar ou girar) ativa cada função de reprodução do Spotify.
* **Pré-condições:** O aplicativo deve estar em execução em um smartphone com Android 10 ou superior.
* **Pós-condições:** O novo mapeamento personalizado é persistido no banco de dados relacional SQLite.
* **Fluxo Principal:**
  1. O usuário acessa o painel de Mapeamento Customizável no aplicativo.
  2. O sistema exibe a lista de funções disponíveis (Play/Pause, Pular Faixa, Alterar Volume).
  3. O usuário seleciona uma função que deseja configurar.
  4. O sistema lista os gestos mapeáveis (Inclinar, Virar, Aceno).
  5. O usuário seleciona o gesto correspondente à função.
  6. O sistema atualiza e salva as novas preferências de mapeamento no banco SQLite.
* **Fluxos Alternativos (Erros e Exceções):**
  * **FA01 - Gesto já atribuído:** O usuário tenta atribuir um gesto que já está vinculado a outra função. O sistema exibe um alerta de conflito e solicita que o usuário confirme a substituição da regra anterior ou escolha um novo gesto.
  * **FA02 - Falha de Gravação no Banco:** Ocorre um erro interno ao tentar persistir os dados no SQLite. O sistema exibe uma mensagem de erro genérica ("Não foi possível salvar as alterações") e mantém as configurações anteriores.

---

## 7 Riscos Identificados
* Mudanças nas políticas ou instabilidades na Spotify Web API.
* Dificuldade técnica no reconhecimento facial devido a variáveis de iluminação incontroláveis pelo software.

---

## 8 Diagrama de casos de uso
<img width="750" height="300" alt="image" src="https://github.com/user-attachments/assets/13156c95-a904-4c60-9982-8916245c5198" />

  
