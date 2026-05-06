# Documento Visão
**Versão 1.0**

| Versão | Data | Autores | Observações |
| :--- | :--- | :--- | :--- |
| 1.0| 18/03/2026| Nicole Cardoso Dias, Pedro Caua, Samuel Gomes, Rodrigo Barbosa, Ricardo Oliveira, Victor Salvador | Primeira versão do projeto, ideia inicial e escopo.|

## 1. Introdução
A acessibilidade digital é fundamental para a autonomia na sociedade atual, mas as interfaces móveis baseadas no toque ainda excluem pessoas com limitações motoras severas. Para transpor essa barreira, este Documento de Visão propõe o desenvolvimento de um sistema baseado em Machine Learning capaz de identificar movimentos faciais e traduzi-los em comandos de navegação no celular. Visando validar a tecnologia e entregar valor de forma ágil, nosso Produto Mínimo Viável (MVP) aplicará esse modelo de inteligência artificial no controle do Spotify. Essa etapa inicial permitirá calibrar a precisão do reconhecimento facial e refinar a usabilidade da interface, garantindo que o dispositivo se adapte às necessidades do usuário e promova uma inclusão tecnológica real.

### 1.2 Finalidade
A finalidade deste documento é detalhar a visão de um sistema de software voltado para a acessibilidade no streaming de música. O foco é permitir que usuários com limitações motoras controlem o Spotify (e futuramente outros aplicativos) através de Visão Computacional e Machine Learning, utilizando movimentos da face como interface de comando.

### 1.2 Escopo
O aplicativo irá capturar imagens da câmera em tempo real para identificar gestos específicos da cabeça (como inclinar para os lados ou acenar verticalmente). Esses gestos serão mapeados para funções do Spotify, como:
* Inclinar para a direita: Aumentar volume
* Inclinar para a esquerda: Diminuir volume
* Virar para a direita: Próxima faixa
* Virar para a esquerda: Faixa Anterior
* Aceno vertical (Sim): Play/Pause.

Implementações Futuras:
* Reiniciar a música;
* Botão aleatório;
* Escolher um momento específico da música;
* Mapeamento Customizável;
* Possibilidade de o usuário escolher qual gesto faz cada ação;
* Disponível para iOS.

### 1.3 Definições, Acrônimos e Abreviações
* (ML) Machine Learning;
* (MVP) Produto Mínimo Viável.

### 1.4 Referências
#### 1.4.1 Especificações Suplementares
* Documento de Regra de Negócio
* Documento de Requisitos
* Documento Manual do Usuário
* Documento Dicionário de Dados

---

## 2. Contextualização
### 2.1 Descrição do Problema

| | |
| :--- | :--- |
| **Problemas** | Dificuldade em interagir com botões pequenos na UI/UX dos dispositivos. <br> Comandos de voz podem ser ineficazes ou frustrantes enquanto a música está tocando em volume alto. <br> Soluções atuais de acessibilidade podem ser caras ou de difícil instalação. |
| **Pessoas Atingidas** | Pessoas com deficiência motora<br> Pessoas com membros amputados <br> Pessoas que não podem interagir com a tela no geral |
| **Cujo impacto é** | Dependência de terceiros: O usuário perde a autonomia, precisando de ajuda para tarefas simples como trocar de música ou ajustar o volume. <br> Exclusão digital e social: A dificuldade de usar aplicativos populares de lazer isola a pessoa de tendências e serviços tecnológicos comuns. <br> Frustração e cansaço: Interfaces não adaptadas exigem um esforço físico e mental exaustivo, tornando o que deveria ser um lazer algo estressante. <br> Barreira financeira: Sem uma solução via software, usuário fica refém de hardwares de acessibilidade caros e inacessíveis. |
| **Uma solução bem-sucedida traria** | Uma solução bem-sucedida traria mais inclusão e acessibilidade, permitindo que pessoas com deficiência usem o Spotify com autonomia por meio de movimentos da cabeça. |

### 2.2 Sentença de Posição do Produto

| | |
| :--- | :--- |
| **Para** | Pessoas com mobilidade reduzida ou deficiências motoras |
| **O**| Sistema de Acessibilidade Cefálica para Spotify|
| **Que** | Proporciona autonomia no controle de reprodução musical sem uso das mãos. |
| **Diferente de** | Softwares de rastreamento ocular caros ou assistentes de voz limitados pelo ruído ambiental |
| **Nosso produto** | É uma solução baseada puramente em software que utiliza IA para democratizar o acesso ao lazer digital. |

---

## 3. Descrição dos Stakeholders e dos Usuários
### 3.1 Principais Stakeholders e Usuários

| Identificação | Responsabilidades | Stakeholders |
| :--- | :--- | :--- |
| Equipe de Desenvolvimento | Implementação do modelo de ML, integração com a API do Spotify, desenvolvimento da interface do app. | Nicole Cardoso Dias, Pedro Cauã, Samuel Gomes, Rodrigo Barbosa, Ricardo Oliveira e Victor Salvador. |
| Usuários Finais | Pessoas que buscam maior autonomia na navegação de aplicativos de áudio. | PCDs |
| Avaliadores | Validar a qualidade técnica e a viabilidade do projeto na UCB | Professores da disciplina e usuários de teste. |

### 3.2 Necessidades Chave dos Stakeholders e dos Usuários

| No. | Descrição | Prioridade do Cliente (Crítico, Útil e Importante) | Observações |
| :--- | :--- | :--- | :--- |
| 1 | Independência na navegação de mídia: Capacidade de trocar faixas, pausar e controlar o volume sem auxílio de terceiros. | Crítico | É o objetivo central do sistema para promover autonomia. |
| 2 | Precisão na detecção de gestos: O modelo de ML deve distinguir entre movimentos naturais e comandos intencionais. | Crítico | Evita disparos acidentais que frustram a experiência do usuário. |
| 3 | Baixa latência (Resposta em tempo real): O comando deve ser executado no Spotify instantaneamente após o gesto. | Crítico | Essencial para que a interface não pareça travada ou lenta. |
| 4 | Privacidade e proteção de dados: Garantia de que as imagens da câmera são processadas localmente e não armazenadas. | Crítico | Dado sensível (biometria facial) exige conformidade com leis de dados. |
| 5 | Facilidade de calibração: O sistema deve se ajustar rapidamente à fisionomia e iluminação do ambiente do usuário. | Importante | Melhora a usabilidade para usuários que não possuem perfil técnico. |
| 6 | Estabilidade na integração com a API: Conexão robusta com o serviço do Spotify para garantir o fluxo da música. | Importante | Depende da disponibilidade do servico de terceiros. |
| 7 | Feedback Visual/Sonoro: Indicação clara de que um comando de cabeça foi reconhecido com sucesso. | Útil | Ajuda o usuário a entender se ele realizou o movimento corretamente. |

---

## 4. Visão Geral do Produto
### 4.1 Perspectiva do Produto
O sistema é uma ferramenta de acessibilidade baseada em software que atua como uma interface intermediária entre o usuário e o aplicativo Spotify. Ele não substitui o player de música, mas adiciona uma camada de controle inteligente via webcam

**Resumo das funcionalidades do Produto**

| Necessidades | Funcionalidades Correspondentes |
| :--- | :--- |
| 1. Independência na navegação de mídia | 1.1 Controle de reprodução: Mapeamento de gestos cefálicos para as funções de Play, Pause e Pular Faixa no Spotify. |
| 2. Precisão na detecção de gestos | 2.1 Classificador de ML: Modelo treinado para distinguir movimentos voluntários de comandos acidentais. |
| 3. Baixa latência (Resposta rápida) | 3.1 Processamento em Tempo Real: Pipeline de visão computacional otimizado para execução local. |
| 4. Garantia de privacidade do usuário | 4.1 Edge Computing: Processamento de vídeo feito estritamente na máquina do usuário, sem envio de imagens para a nuvem. |
| 5. Facilidade de uso por diferentes perfis | 5.1 Módulo de Calibração: Interface para o usuário configurar a amplitude de seus movimentos antes de iniciar o uso. |
| 6. Estabilidade na comunicação com o player | 6.1 Integração Web API: Módulo de comunicação direta com os servidores do Spotify via API oficial. |
| 7. Confirmação de comandos | 7.1 HUD de Feedback: Interface visual que indica na tela quando um gesto foi reconhecido com sucesso. |
| 8. Flexibilidade de comandos | 8.1 Mapeamento Customizável: Painel que permite ao usuário escolher qual movimento (ex: girar vs. inclinar) ativa cada função. |
| 9. Uso em ambientes variados | 9.1 Normalização de Luz: Filtros de imagem que garantem o rastreamento em ambientes com pouca ou muita claridade. |
| 10. Facilidade de instalação | 10.1 Empacotamento Standalone: Software pronto para rodar sem exigir a instalação manual de dependências técnicas complexas. |

### 4.2 Estimativa de Custo Inicial
Como se trata de um projeto acadêmico da UCB, o custo financeiro direto é reduzido, focando em:
* Hardware: Smartphones com câmera integrada.
* Software: Bibliotecas de código como o MediaPipe, OpenCV, Kotlin e a Spotify Web API (gratuita para desenvolvedores).
* Mão de Obra: Horas de desenvolvimento e treinamento do modelo de Machine Learning pela equipe.

### 4.3 Suposições e dependências
* O usuário deve possuir uma conta ativa no Spotify para que a API funcione.
* A precisão do sistema depende de condições mínimas de iluminação para que a câmera identifique o rosto.
* O dispositivo deve estar conectado à internet para enviar os comandos de troca de música ao servidor do Spotify.

### 4.4 Premissas e Dependências
O sistema deverá utilizar os seguintes padrões de premissas e dependências:
* Máquina do Desenvolvedor: Android Studio
* Máquina do Servidor: Aplicação, Banco de Dados

---

## 5. Precedência e Prioridades
* Crítico: Mapeamento de marcos faciais (Landmarks)
* Crítico: Integração com API do Spotify (Play/Pause/Skip)
* Importante: Calibração automática de sensibilidade de movimento
* Útil: Interface gráfica para feedback de comando


## 6. Requisitos Não-Funcionais do Produto
* Usabilidade: O sistema deve ser intuitivo o suficiente para que um usuário PCD consiga calibrar os movimentos em menos de 2 minutos.
* Confiabilidade: O modelo de ML deve ter uma taxa de acerto superior a 90% na detecção dos gestos em ambientes iluminados.
* Segurança (Privacidade): Nenhum frame de vídeo da câmera deve ser armazenado ou enviado para servidores externos; o processamento de imagem deve ser estritamente local (Edge Computing).
* Desempenho: O tempo entre o gesto da cabeça e a execução da ação no Spotify não deve ultrapassar 500ms (baixa latência).

---

## 7. Restrições Técnicas
### 7.1 Padrões Aplicáveis
O projeto deve seguir as diretrizes da LGPD (Lei Geral de Proteção de Dados) por lidar com dados biométricos faciais.

### 7.2 Restrições Aplicáveis
O backend deve ser desenvolvido preferencialmente em Kotlin, pois é a linguagem oficial e a mais recomendada pelo Google, além possuir maior integração com o sistema Android.

### 7.3 Hardware Mínimo
* Câmera com resolução de pelo menos 720p para garantir a detecção dos pontos do rosto.
* Smartphone com sistema operacional Android 10 ou superior.
* Câmera frontal com capacidade de captura de vídeo estável em condições de iluminação ambiente.
* Conexão ativa com internet (4G/5G ou Wi-Fi) para comunicação com os servidores do Spotify.

---

## 8. Manual de Arquitetura
### 8.1 Visão de Implementação
O sistema será estruturado em uma arquitetura de camadas para garantir a separação de responsabilidades e facilitar a manutenção do código:
* Camada de Apresentação: Desenvolvida em Kotlin com Jetpack Compose, será responsável pela interface do usuário e pela exibição do feedback visual dos comandos.
* Camada de Processamento: Utilizará a biblioteca MediaPipe Face Mesh para o rastreamento em tempo real dos pontos do rosto e a lógica de classificação de gestos.
* Camada de Integração: Responsável pela comunicação com a Spotify Web API através de requisições REST, traduzindo os gestos detectados em comandos de reprodução (play, pause, skip).
* Camada de Processamento: Utilizará a biblioteca OpenCV para a captura e tratamento de frames da câmera, integrada ao MediaPipe Face Mesh para o rastreamento em tempo real dos pontos do rosto e lógica de classificação de gestos.
* Camada de Dados: Utilizará o SQLite para persistir configurações de usuário, como o mapeamento personalizado de gestos e níveis de sensibilidade da câmera.
* View (Jetpack Compose): Interface onde o usuário visualiza o feed da câmera e calibra os movimentos.
* Processamento (OpenCV + MediaPipe): O OpenCV fará a gestão do buffer da câmera do celular (Android Camera2 API), convertendo os frames para o MediaPipe realizar o Face Mesh.
* Controller/Service: Um serviço de acessibilidade (Accessibility Service) ou integração via Broadcast para garantir que os comandos cheguem ao Spotify mesmo com o app em segundo plano.

### 8.2 Mecanismo Arquiteturais

| | | |
| :--- | :--- | :--- |
| Persistência | Banco de Dados Relacional | SQLite |
| Visão Computacional | Redes Neurais Convolucionais | MediaPipe Face Mesh |
| Integração Externa | REST API | Spotify Web API/ Spotipy |

---
