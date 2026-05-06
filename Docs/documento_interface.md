# Documentação de Interface e Usabilidade 

**Projeto:** Sistema de Acessibilidade Cefálica (SAC) para Spotify
**Objetivo:** Especificar a estrutura visual, fluxos de navegação e diretrizes de design adotadas para garantir uma experiência acessível e autônoma para os usuários.

---

## 1. Identificação das Telas e Funcionalidades

O aplicativo possui uma jornada de usuário objetiva, dividida em cinco telas principais:

*   **1. Tela de Login / Conexão:**
    *   **Descrição:** Interface inicial simples para conectar a conta do usuário.
    *   **Funcionalidade:** Autenticar o usuário de forma segura através da API do Spotify (OAuth 2.0) utilizando o botão "Conectar com o Spotify".

*   **2. Tela Inicial (Home / Dashboard):**
    *   **Descrição:** Tela principal de acesso após a autenticação.
    *   **Funcionalidade:** Exibe o status da conexão e atua como ponto central de navegação, contendo botões rápidos para "Iniciar Rastreamento", "Calibrar Movimentos" e "Configurar Gestos".

*   **3. Tela de Calibração de Sensibilidade:**
    *   **Descrição:** Interface de configuração física que exibe o feed da câmera com uma máscara guia.
    *   **Funcionalidade:** Pede ao usuário para realizar movimentos (direita, esquerda, cima e baixo) para registrar a amplitude máxima e mínima confortável, calibrando o sistema.

*   **4. Tela de Mapeamento de Comandos:**
    *   **Descrição:** Formulário ou lista de configurações.
    *   **Funcionalidade:** Permite ao usuário associar gestos físicos específicos (ex: "Inclinar cabeça para a Direita") a ações de reprodução de música (ex: "Próxima Faixa").

*   **5. Tela de Rastreamento (Player Ativo):**
    *   **Descrição:** Interface de uso contínuo com design minimalista.
    *   **Funcionalidade:** Exibe informações da música atual (capa, título e artista), mostra o feed da câmera (podendo ser minimizado) e exibe um forte feedback visual sempre que um comando é reconhecido com sucesso.

---

## 2. Fluxo de Navegação (User Flow)

O percurso do usuário foi desenhado para ser direto, minimizando etapas.

**A. Fluxo Geral de Acesso e Configuração:**
1. O usuário abre o aplicativo e o sistema verifica a autenticação.
2. Se não estiver logado, passa pela **Tela de Login** e Autorização do Spotify.
3. Se logado, é direcionado à **Tela Inicial**, de onde pode navegar para a **Tela de Calibração** ou **Tela de Mapeamento**.

**B. Fluxo de Uso Contínuo (Controle de Mídia):**
1. O usuário inicia o rastreamento, ativando a câmera.
2. O sistema monitora constantemente. Se um movimento atinge o limiar calibrado, o gesto é identificado.
3. O comando mapeado é ativado, enviando uma requisição para a API do Spotify.
4. A tela exibe um **Feedback Visual de Sucesso** e continua o rastreamento normal.

---

## 3. Padrões de Layout e Usabilidade

Como o público-alvo possui limitações motoras severas, a interface foi construída sobre quatro pilares de acessibilidade:

*   **Alvos de Toque Ampliados:** Todos os botões e áreas clicáveis possuem tamanho mínimo de 48x48 dp e espaçamento generoso para evitar toques acidentais.
*   **Prevenção de Erros:** Exigência de confirmação antes de sobrescrever calibrações e exibição de avisos claros e grandes caso o rosto não seja detectado por falta de luz.
*   **Feedback Multissensorial:** Ações bem-sucedidas geram retorno visual (tela pisca, muda de cor ou mostra ícone) e háptico (vibração), garantindo que o usuário saiba que o comando funcionou.
*   **Consistência Cognitiva:** A estrutura segue os padrões mentais já conhecidos de players de música (ex: capa centralizada e controles na parte inferior).

---

## 4. Design System e Identidade Visual

O visual do aplicativo inspira-se no Spotify para criar familiaridade, adotando alto contraste e foco em legibilidade.

*   **Cores e Tematização:** O **Dark Theme** é o padrão para evitar cansaço visual. Utiliza fundo Preto e superfícies em Cinza Chumbo, com textos em Branco/Cinza Médio, usando o Verde Spotify (`#1DB954`) como cor primária de destaque.
*   **Tipografia:** Fontes sem serifa (Inter ou Roboto) com forte hierarquia de tamanhos (Títulos grandes entre 28sp e 32sp) e botões com formatação *Sentence case* para leitura facilitada.
*   **Componentes Visuais:** Botões com cantos arredondados e feed de câmera estilizado com uma máscara de sobreposição escura com centro oval para guiar o enquadramento do usuário.
