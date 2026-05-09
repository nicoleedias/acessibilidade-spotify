# ADR-001: Mapeamento gesto→ação é totalmente personalizável

## Status
Aceita

## Contexto
A documentação inicial do projeto (`documento_visao.md`) propunha um
mapeamento fixo entre gestos e ações do Spotify. O protótipo Figma
apresentou um conjunto diferente de gestos (incluindo piscar de olhos)
no dropdown da tela "Configurar Gestos".

A inconsistência levantou a questão: qual mapeamento adotar?

## Decisão
Não adotar nenhum mapeamento fixo. O usuário escolhe livremente qual
gesto dispara cada ação Spotify, dentro de um catálogo de:

- 9 gestos suportados (inclinações, virações, aceno, piscadas)
- 5 ações Spotify (play/pause, próxima, anterior, volume +/-)

O app traz um mapeamento default na primeira execução, mas todo gesto
é editável e persistido em SQLite.

## Consequências

### Positivas
- Acessibilidade real: usuários com diferentes capacidades motoras
  podem escolher gestos confortáveis para si.
- Conformidade com WCAG 2.1 SC 2.5.6 (Concurrent Input Mechanisms).
- Arquitetura mais limpa: classificador de gestos não conhece Spotify;
  mapeamento vive em camada separada (GestureActionMapper).

### Negativas
- Maior complexidade na UI da tela "Configurar Gestos" (validar
  duplicatas, lidar com gestos não mapeados).
- Maior superfície de teste (combinações de mapeamento).

## Alternativas consideradas
1. Mapeamento fixo do documento_visao.md (rejeitada — exclui usuários
   com limitações específicas).
2. Mapeamento fixo do Figma (rejeitada — mesma razão).
3. Dois "perfis" predefinidos (rejeitada — ainda exclui casos extremos).

## Referências
- W3C WCAG 2.1, Success Criterion 2.5.6
- CLAUDE.md, seção "Gesture-to-Command Mapping"
