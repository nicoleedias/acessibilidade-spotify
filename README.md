# acessibilidade-spotify
O Sistema de Acessibilidade Cefálica usa Machine Learning para traduzir movimentos da cabeça em comandos no Spotify. Focado em pessoas com deficiência motora, promove autonomia com processamento 100% local, garantindo privacidade e baixa latência sem enviar imagens a servidores.
# Sistema de Acessibilidade Cefálica para Spotify 

![Status do Projeto](https://img.shields.io/badge/Status-Em%20Desenvolvimento-green)
![Plataforma](https://img.shields.io/badge/Plataforma-Android-3DDC84)
![Linguagem](https://img.shields.io/badge/Linguagem-Kotlin-7F52FF)

##  Visão Geral
[cite_start]A acessibilidade digital é fundamental para a autonomia, mas interfaces baseadas no toque ainda excluem pessoas com limitações motoras severas[cite: 35]. [cite_start]Este projeto é um Produto Mínimo Viável (MVP) de um sistema baseado em Machine Learning capaz de identificar movimentos faciais e traduzi-los em comandos de navegação no celular, focado inicialmente no controle do aplicativo Spotify[cite: 36, 37]. 

[cite_start]O objetivo é democratizar o acesso ao lazer digital com uma solução estritamente de software, substituindo hardwares caros ou assistentes de voz que sofrem interferência de ruído[cite: 70, 72].

##  Funcionalidades e Mapeamento de Gestos
[cite_start]O aplicativo captura imagens da câmera frontal em tempo real e mapeia gestos cefálicos para ações específicas via integração com a Spotify Web API[cite: 44, 158].

* [cite_start]**Inclinar a cabeça para a direita:** Aumentar volume[cite: 46].
* [cite_start]**Inclinar a cabeça para a esquerda:** Diminuir volume[cite: 46].
* [cite_start]**Virar o rosto para a direita:** Próxima faixa[cite: 46].
* [cite_start]**Virar o rosto para a esquerda:** Faixa Anterior[cite: 47].
* [cite_start]**Aceno vertical (Sim):** Play / Pause[cite: 47].

##  Arquitetura e Tecnologias
[cite_start]O projeto foi estruturado em camadas para facilitar a manutenção e garantir o isolamento de responsabilidades[cite: 155]:

* [cite_start]**Camada de Apresentação:** Construída em **Kotlin** utilizando **Jetpack Compose** para exibir a interface de calibração e o feedback visual[cite: 156, 161].
* [cite_start]**Visão Computacional e ML:** Utiliza **OpenCV** para o tratamento dos frames e **MediaPipe Face Mesh** para o rastreamento em tempo real dos marcos faciais (Landmarks)[cite: 157, 159, 167].
* [cite_start]**Integração Externa:** Comunicação via REST API com a **Spotify Web API**[cite: 158, 167].
* [cite_start]**Persistência de Dados:** **SQLite** para armazenar as preferências do usuário, mapeamento de gestos e sensibilidade[cite: 160, 167].

## Privacidade e Desempenho
* [cite_start]**Edge Computing:** Todo o processamento de imagem é realizado estritamente de forma local no dispositivo[cite: 130]. [cite_start]Nenhum frame de vídeo é armazenado ou enviado à nuvem, em conformidade com a LGPD[cite: 129, 136].
* [cite_start]**Baixa Latência:** O sistema é otimizado para que o tempo de resposta entre o gesto e a ação não ultrapasse 500ms[cite: 131].

##  Pré-requisitos (Hardware Mínimo)
* [cite_start]Smartphone com sistema operacional Android 10 ou superior[cite: 148].
* [cite_start]Câmera frontal com resolução mínima de 720p[cite: 147].
* [cite_start]Conexão ativa com internet (4G/5G ou Wi-Fi) para chamadas à API do Spotify[cite: 150].
* [cite_start]Conta ativa no Spotify[cite: 106].

##  Equipe de Desenvolvimento
[cite_start]Projeto desenvolvido como parte acadêmica na **Universidade Católica de Brasília (UCB)**[cite: 4, 100].
* [cite_start]Nicole Cardoso Dias [cite: 15]
* [cite_start]Pedro Cauã [cite: 15]
* [cite_start]Samuel Gomes [cite: 15]
* [cite_start]Rodrigo Barbosa [cite: 15]
* [cite_start]Ricardo Oliveira [cite: 15]
* [cite_start]Victor Salvador [cite: 15]
