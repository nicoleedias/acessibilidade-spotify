# Documentos de Suporte à Prototipação

**Projeto:** SAC - Sistema de Acessibilidade Cefálica para Spotify

**Objetivo:** Especificar a estrutura visual, fluxos de navegação e diretrizes de design para a construção das interfaces do aplicativo.

---

## 1. Identificação das Principais Telas e Funcionalidades

O sistema foi dividido em cinco telas principais, focadas em manter a jornada do usuário curta e objetiva:

**1. Tela de Login / Conexão:**
* **Funcionalidade:** Autenticar o usuário utilizando a API do Spotify (OAuth 2.0).
* **Descrição:** Interface simples com um botão de "Conectar com o Spotify". Redireciona para o fluxo seguro de login da plataforma e retorna ao app após o sucesso.

**2. Tela Inicial (Home / Dashboard):**
* **Funcionalidade:** Ponto central de navegação e início rápido do rastreamento.
* **Descrição:** Exibe o status da conexão com o Spotify, botões de acesso rápido para "Iniciar Rastreamento", "Calibrar Movimentos" e "Configurar Gestos".

**3. Tela de Calibração de Sensibilidade:**
* **Funcionalidade:** Registrar a amplitude máxima e mínima dos movimentos cefálicos do usuário.
* **Descrição:** Exibe o feed da câmera frontal com uma máscara guia. Pede ao usuário para realizar movimentos para a direita, esquerda, cima e baixo, registrando os limites confortáveis para ele.

**4. Tela de Mapeamento de Comandos:**
* **Funcionalidade:** Associar gestos físicos a ações do reprodutor de música.
* **Descrição:** Uma lista ou formulário simples onde o usuário define que o gesto "Inclinar cabeça para a Direita", por exemplo, executará a ação "Próxima Faixa".

**5. Tela de Rastreamento (Player Ativo):**
* **Funcionalidade:** Tela principal de uso contínuo onde a detecção ocorre.
* **Descrição:** Design minimalista. Mostra a música que está tocando no momento (capa do álbum, título e artista), o feed da câmera (que pode ser minimizado) e exibe um forte feedback visual (animação ou ícone) no momento em que um gesto é reconhecido e executado.

---

## 2. Fluxo de Navegação entre as Telas (User Flow)

Os diagramas abaixo mapeiam o caminho que o usuário percorre entre as telas para completar as tarefas no sistema.

### 2.1 Fluxo Geral e Configuração
Mapeia o primeiro acesso e a configuração do sistema.

<img width="400" height="600" alt="image" src="https://github.com/user-attachments/assets/4b9b2ff8-a8f1-4139-8780-0bcc614bae40" />


### 2.2 Fluxo de Uso (Controle de Mídia)
Mapeia a jornada principal de ativação da câmera e controle da música.

<img width="440" height="600" alt="image" src="https://github.com/user-attachments/assets/265076fe-68f2-41d3-a67e-64ee796d1952" />


---

## 3. Padrões de Layout e Usabilidade Adotados

Como o público-alvo inclui pessoas com deficiência motora ou mobilidade reduzida severa, a usabilidade e a acessibilidade são os pilares do layout:

* **Alvos de Toque Ampliados (Touch Targets):** Todos os botões, links e áreas clicáveis devem ter um tamanho mínimo de 48x48 dp. O espaçamento entre os elementos deve ser generoso para evitar toques acidentais.
* **Prevenção e Recuperação de Erros:** O sistema deve pedir confirmação antes de sobrescrever uma calibração existente. Se houver falha de iluminação (rosto não detectado), a tela de rastreamento deve pausar e exibir um aviso claro em texto grande.
* **Feedback Contínuo e Multissensorial:** Quando um usuário realiza um gesto com a cabeça e ele é computado, a interface deve piscar, mudar de cor ou exibir um ícone grande (ex: símbolo de "Pause"), além de fornecer feedback háptico (vibração do celular), para que ele saiba que não precisa repetir o esforço.
* **Consistência Cognitiva:** O layout seguirá os padrões mentais já estabelecidos por aplicativos de música (ex: botões de controle na parte inferior, capa do álbum centralizada).

---

## 4. Documentos de Suporte à Prototipação (Design System)

A documentação visual segue o modelo de alto contraste, inspirando-se na identidade visual do Spotify para criar uma experiência integrada.

### 4.1 Cores e Tematização (Dark Theme)
O tema escuro foi escolhido como padrão irrevogável para reduzir o cansaço visual e destacar o feed da câmera.

* **Cor Primária (Destaques e Sucesso):** Verde Spotify (#1DB954)
* **Cor de Fundo (Background):** Preto (#121212)
* **Cor de Superfície (Cards, Modais):** Cinza Chumbo (#282828)
* **Texto Principal:** Branco Nevasca (#FFFFFF)
* **Texto Secundário / Desabilitado:** Cinza Médio (#B3B3B3)
* **Alertas e Erros:** Vermelho Coral (#E22134)

### 4.2 Tipografia
Prioridade total para legibilidade.

* **Família de Fontes:** Inter ou Roboto (Sem serifa).
* **Hierarquia:**
  * **H1 (Títulos de Telas):** Bold, 28sp a 32sp.
  * **H2 (Subtítulos):** SemiBold, 20sp a 24sp.
  * **Corpo (Instruções e Nomes de Músicas):** Regular, 16sp a 18sp.
  * **Botões:** Bold, 16sp, letras em formato Sentence case (Apenas primeira maiúscula para facilitar leitura).

### 4.3 Comportamento de Componentes

* **Botões Primários:** Fundo Verde (#1DB954), texto Preto ou Branco puro, cantos arredondados (Radius 8dp a 16dp).
* **Cards de Lista (Mapeamentos):** Fundo Cinza Chumbo (#282828), elevação sutil.
* **Feed da Câmera:** Deve possuir uma máscara de sobreposição (overlay) escura nas bordas, com um formato oval transparente no centro para guiar o enquadramento do rosto do usuário.

---

## 5. Protótipos de Alta Fidelidade

* **Link de acesso ao Figma da equipe:** *https://www.figma.com/design/b0lkIdz4Kjl1fqQVdFQ8wT/SAC?node-id=0-1&t=z2uimDr30RHlnaiC-1*
