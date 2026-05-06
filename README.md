# acessibilidade-spotify
O Sistema de Acessibilidade Cefálica usa Machine Learning para traduzir movimentos da cabeça em comandos no Spotify. Focado em pessoas com deficiência motora, promove autonomia com processamento 100% local, garantindo privacidade e baixa latência sem enviar imagens a servidores.
# Sistema de Acessibilidade Cefálica para Spotify 

![Status do Projeto](https://img.shields.io/badge/Status-Em%20Desenvolvimento-green)
![Plataforma](https://img.shields.io/badge/Plataforma-Android-3DDC84)
![Linguagem](https://img.shields.io/badge/Linguagem-Kotlin-7F52FF)

##  Visão Geral
A acessibilidade digital é fundamental para a autonomia, mas interfaces baseadas no toque ainda excluem pessoas com limitações motoras severas. Este projeto é um Produto Mínimo Viável (MVP) de um sistema baseado em Machine Learning capaz de identificar movimentos faciais e traduzi-los em comandos de navegação no celular, focado inicialmente no controle do aplicativo Spotify. 

O objetivo é democratizar o acesso ao lazer digital com uma solução estritamente de software, substituindo hardwares caros ou assistentes de voz que sofrem interferência de ruído.

##  Funcionalidades e Mapeamento de Gestos
O aplicativo captura imagens da câmera frontal em tempo real e mapeia gestos cefálicos para ações específicas via integração com a Spotify Web API.

* **Inclinar a cabeça para a direita:** Aumentar volume.
* **Inclinar a cabeça para a esquerda:** Diminuir volume.
* **Virar o rosto para a direita:** Próxima faixa.
* **Virar o rosto para a esquerda:** Faixa Anterior.
* **Aceno vertical (Sim):** Play / Pause.

##  Arquitetura e Tecnologias
O projeto foi estruturado em camadas para facilitar a manutenção e garantir o isolamento de responsabilidades:

* **Camada de Apresentação:** Construída em **Kotlin** utilizando **Jetpack Compose** para exibir a interface de calibração e o feedback visua.
* **Visão Computacional e ML:** Utiliza **OpenCV** para o tratamento dos frames e **MediaPipe Face Mesh** para o rastreamento em tempo real dos marcos faciais (Landmarks).
**Integração Externa:** Comunicação via REST API com a **Spotify Web API**.
**Persistência de Dados:** **SQLite** para armazenar as preferências do usuário, mapeamento de gestos e sensibilidade.

## Privacidade e Desempenho
* ** Todo o processamento de imagem é realizado estritamente de forma local no dispositivo.Nenhum frame de vídeo é armazenado ou enviado à nuvem, em conformidade com a LGPD.
* **Baixa Latência:** O sistema é otimizado para que o tempo de resposta entre o gesto e a ação não ultrapasse 500ms.

##  Pré-requisitos (Hardware Mínimo)
* Smartphone com sistema operacional Android 10 ou superior.
* Câmera frontal com resolução mínima de 720p.
* Conexão ativa com internet (4G/5G ou Wi-Fi) para chamadas à API do Spotify.
* Conta ativa no Spotify.

##  Equipe de Desenvolvimento
Projeto desenvolvido como parte acadêmica na **Universidade Católica de Brasília (UCB)**.
* Nicole Cardoso Dias
* Pedro Cauã 
* Samuel Gomes 
* Rodrigo Barbosa
* Ricardo Oliveira 
* Victor Salvador
