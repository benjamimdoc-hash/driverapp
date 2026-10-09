# DriverApp (nome provisório)

Assistente para motoristas de aplicativo: lê a oferta de corrida na tela (Uber e 99),
calcula R$/km, R$/hora, custo de combustível e resultado estimado, e acompanha a jornada.
Funciona offline, direto no celular. **Android 12 ou superior.**

## Como baixar o APK de teste

1. No celular, abra este repositório no GitHub → **Releases** (lado direito / mais abaixo na página).
2. Toque na versão mais recente e baixe o arquivo `driverapp-teste-N.apk`.
3. Abra o arquivo. Se o Android pedir, permita "instalar apps desta fonte".

Cada versão nova instala **por cima** da anterior (a chave de teste é fixa).

## Estrutura

| Pasta | O que tem | Testável sem celular? |
|---|---|---|
| `calculo/` | Fórmulas: combustível, despesas, metas, análise de corrida, classificação | Sim (`./gradlew :calculo:test`) |
| `leitores/` | Texto da tela da Uber/99 → dados da oferta | Sim (`./gradlew :leitores:test`) |
| `app/dados/` | Banco local (Room): configuração, despesas, jornadas, corridas | Não — testar no celular |
| `app/jornada/` | Serviço de GPS em primeiro plano, notificações, aviso após reiniciar | Não — testar no celular |
| `app/leitura/` | Serviço de leitura das ofertas, card sobreposto, análise com os custos do cadastro | Parcial (`./gradlew :app:testDebugUnitTest`) |
| `app/ui/` | Sistema de design (temas), cadastro, painel, histórico, ajustes, leitura, calculadora | Não — testar no celular |
| `.github/workflows/build.yml` | Compilação automática do APK | — |

## Regras importantes do projeto

- O app **nunca** aceita, recusa ou solicita corridas sozinho. Só lê e calcula.
- Dado que não foi lido com segurança aparece como **indisponível**, nunca é inventado.
- Faturamento bruto, custos, resultado e lucro estimado são sempre mostrados separados.
- As faixas de cor (vermelho/amarelo/verde) são **valores iniciais sugeridos e editáveis**.

## Fases

- [x] Fase 1 — Estrutura, motor de cálculo, leitores Uber/99, calculadora de teste, APK automático
- [x] Fase 2 — Cadastro inicial, custos, metas, dashboard, jornada com GPS, histórico
- [x] Fase 3 — Leitura automática da tela (só leitura, via Acessibilidade), validação, card da corrida, limites por categoria
  - [x] Leitura das telas de saldo/carteira (Uber: total do dia; 99: total da semana), sem duplicar
- [x] Fase 4 — Painel flutuante da jornada (ícone arrastável, opacidade, controles da jornada)
- [ ] Fase 5 — Licença por token e mensagens motivacionais
- [ ] Fase 6 — Modo Segurança, mais plataformas, relatórios
