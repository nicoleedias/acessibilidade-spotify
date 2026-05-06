# Documento de Arquitetura de Software

## Informações do Projeto

| Sigla - Nome do Projeto | Gestor / Orientador | Equipe de Desenvolvimento |
| :--- | :--- | :--- |
| SAC - Sistema de Acessibilidade Cefálica para Spotify | Alexandre S. D. Santos | Nicole, Pedro Caua, Samuel Gomes, Rodrigo, Ricardo, Victor |

## Objetivo deste Documento
Este documento tem como objetivo descrever as principais decisões de projeto tomadas pela equipe de desenvolvimento e os critérios considerados durante a tomada destas decisões. Suas informações incluem a parte de hardware e software do sistema.

## Histórico de Revisão

| Data | Demanda | Autor | Descrição | Versão |
| :--- | :--- | :--- | :--- | :--- |
| 18/04/2026 | Arquitetura | Equipe | Elaboração inicial do documento de arquitetura baseado nos requisitos. | 1.0 |

---

## 1. INTRODUÇÃO

### 1.1 Finalidade
Este documento fornece uma visão arquitetural abrangente do sistema Sistema de Acessibilidade Cefálica para Spotify, usando diversas visões de arquitetura para representar diferentes aspectos do sistema. O objetivo deste documento é capturar e comunicar as decisões arquiteturais significativas que foram tomadas em relação ao sistema. O documento irá adotar uma estrutura baseada na visão "4+1" de modelo de arquitetura.

### 1.2 Escopo
Este Documento de Arquitetura de Software se aplica ao Sistema de Acessibilidade Cefálica, que será desenvolvido pelos alunos da disciplina de Análise e Projeto de Software. O projeto se destina a desenvolver um sistema voltado para a acessibilidade no streaming de música. O aplicativo irá capturar imagens da câmera em tempo real para identificar gestos específicos da cabeça e traduzi-los em comandos de navegação.

### 1.3 Definições, Acrônimos e Abreviações
* **MVVM:** Model-View-ViewModel. Padrão arquitetural focado na separação de responsabilidades.
* **QoS:** Quality of Service, ou qualidade de serviço. Termo utilizado para descrever requisitos não-funcionais.
* **API:** Application Programming Interface.
* **REST:** Representational State Transfer.

### 1.4 Referências
* [KRU41]: The "4+1" view model of software architecture, Philippe Kruchten.
* [REQ01]: SISTEMA DE ACESSIBILIDADE CEFÁLICA PARA SPOTIFY Especificação de Casos de Uso.

---

## 2. REPRESENTAÇÃO ARQUITETURAL
Este documento irá detalhar as visões baseado no modelo "4+1". O padrão arquitetural adotado para a construção do software será o MVVM, devido à sua compatibilidade nativa com ferramentas declarativas de interface em Android.

**Tabela 1 - Visões e Público**

| Visão | Público | Área |
| :--- | :--- | :--- |
| Lógica | Analistas | Realização dos Casos de Uso |
| Processo | Integradores | Performance, Escalabilidade, Concorrência |
| Implementação | Programadores | Componentes de Software |
| Implantação | Gerência de Configuração | Nodos físicos |
| Caso de Uso | Todos | Requisitos funcionais |
| Dados | Especialistas em dados | Persistência de dados |

---

## 3. REQUISITOS E RESTRIÇÕES ARQUITETURAIS
Esta seção descreve os requisitos de software e restrições que têm um impacto significante na arquitetura.

**Tabela 2 - Restrições arquiteturais e tecnológicas**

| Requisito | Solução |
| :--- | :--- |
| Linguagem e UI | O sistema utilizará Kotlin e Jetpack Compose para a camada de apresentação. |
| Plataforma | Dispositivo com sistema operacional Android 10 ou superior, com câmera frontal de resolução mínima de 720p. |
| Segurança | Processamento local (Edge Computing); não armazenamento ou envio de imagens para a nuvem. |
| Persistência | Utilização de banco de dados relacional SQLite para salvar níveis de calibração e mapeamentos personalizados. |
| Conectividade | Conexão ativa com internet (4G/5G ou Wi-Fi) e conta ativa no Spotify. |

---

## 4. VISÃO DE CASOS DE USO
Esta seção lista as especificações centrais e significantes para a arquitetura do sistema. Casos de Uso significantes para a arquitetura:
* **UC01 - Controlar Reprodução de Mídia via Gestos:** O usuário realiza gestos cefálicos para acionar comandos de reprodução musical no Spotify.
* **UC02 Calibrar Sensibilidade e Movimentos:** O usuário utiliza uma interface para configurar a amplitude de seus movimentos antes de iniciar o uso.
* **UC03 - Customizar Mapeamento de Comandos:** O usuário acessa um painel para escolher qual gesto específico ativa cada função de reprodução.

**Diagramas:**

Diagrama com os casos de uso significativos e atores:
<img width="918" height="358" alt="image" src="https://github.com/user-attachments/assets/4ddffdbd-7090-40be-b7ce-0bd542da9f19" />

Diagrama de Camadas da Aplicação:
<img width="930" height="538" alt="image" src="https://github.com/user-attachments/assets/f662a68f-f763-40d6-ac73-ae314bb3def4" />

Diagrama de Classes:
<img width="916" height="548" alt="image" src="https://github.com/user-attachments/assets/d3020891-b85c-4fcb-8cf1-639d63af5f26" />

Diagrama de Sequência:
<img width="1642" height="434" alt="image" src="https://github.com/user-attachments/assets/4a9b0a68-a77f-4efa-9be7-c17eb0de03b1" />

Diagrama de Implantação:
<img width="1642" height="268" alt="image" src="https://github.com/user-attachments/assets/161dd919-7518-4187-a377-cb008d3f6c78" />

## 8. DIMENSIONAMENTO E PERFORMANCE

### 8.1 Volume
* **Usuários:** Estimado em 1 usuário principal por dispositivo físico.
* **Armazenamento de Imagens:** Zero. O sistema gerencia o buffer em tempo real sem salvar imagens em disco.

### 8.2 Performance
* **Latência:** O tempo de resposta deve ser inferior a 500ms entre a detecção do gesto e a ação executada no Spotify.

## 9. QUALIDADE

| Item | Descrição | Solução |
| :--- | :--- | :--- |
| **Usabilidade** | Rapidez na configuração inicial. | Interface intuitiva para calibração em menos de 2 minutos. |
| **Confiabilidade** | Precisão da visão computacional. | Taxa de acerto superior a 90% em ambientes iluminados. |
| **Segurança** | Privacidade biométrica. | Processamento estritamente local (Edge Computing). |
