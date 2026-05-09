# ADR-002: Adiar migração para ícones AutoMirrored

## Status
Aceita (temporária)

## Contexto
O compilador Kotlin emite 7 warnings sobre uso de:
- Icons.Filled.ArrowBack
- Icons.Filled.KeyboardArrowLeft
- Icons.Filled.KeyboardArrowRight

Recomendação: usar as variantes em Icons.AutoMirrored.Filled.*, que
espelham automaticamente em layouts RTL (right-to-left), como árabe
e hebraico.

## Decisão
Adiar a migração. Os ícones deprecados continuarão funcionando.
A migração será feita antes da defesa do TCC.

## Razões
1. Português brasileiro (idioma do app) é LTR — espelhamento não muda
   experiência do usuário-alvo no escopo do MVP.
2. Não bloqueia funcionalidade nem qualidade do build.
3. Outras frentes têm prioridade: refatoração MVVM, integração Spotify,
   pipeline de visão.

## Consequências
- Build emite 7 warnings (não erros). Aceitável temporariamente.
- Quando migrarmos, é trivial: substituir o import.
- Antes da defesa, atualizar para AutoMirrored e citar como
  "preparação para internacionalização futura".

## Referências
- https://developer.android.com/jetpack/compose/graphics/images/material#auto-mirrored
