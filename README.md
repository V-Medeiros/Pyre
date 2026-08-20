# VESTA

## Sobre o projeto

Vesta e um SaaS de produtividade gamificado baseado em sessoes de foco, tarefas e acompanhamento de consistencia. A ideia principal e registrar o tempo focado de forma visual: cada sessao concluida contribui para a evolucao da chama, para o streak diario e para o historico de progresso.

O produto fica entre aplicativos de foco, rastreadores de habito e experiencias gamificadas leves. A proposta nao e criar um RPG completo nem um gerenciador de tarefas complexo, mas sim um ritual simples, visual e recompensador para manter consistencia.

## Proposta de valor

- Transformar tempo focado em uma experiencia visual e emocional.
- Mostrar o progresso atual por meio da chama visual preservada da V1.
- Representar o esforco acumulado em uma fogueira historica.
- Incentivar retorno e consistencia sem culpa excessiva.
- Funcionar inicialmente sem conta, sem internet e sem configuracao complexa.

## Publico-alvo

Vesta foi pensado para estudantes, desenvolvedores, profissionais criativos e pessoas que querem construir consistencia sem transformar produtividade em pressao.


## Funcionalidades do MVP

### Timer configuravel

- Duracoes pre-definidas: 15, 25, 45 e 60 minutos.
- Campo de duracao livre entre 5 e 120 minutos.
- Display em formato `MM:SS`.
- Estados da sessao: `idle`, `running`, `paused`, `completed` e `abandoned`.
- Controles para iniciar, pausar, retomar e abandonar.
- Encerramento imediato de uma sessao em andamento.
- Timer calculado por timestamp para resistir a troca de abas.
- Mensagem tematica ao concluir uma sessao.

### Chama visual

A chama central usa a imagem `vesta-flame-symbol-redesign-512.png`, preservada
da V1. O estado do timer e mostrado pelos controles e pelo texto da sessao;
o progresso de streak aparece no indicador com os niveis Spark, Kindling,
Flame, Torch e Bonfire.

### Tarefas vinculadas ao foco
- Criar tarefas por campo de texto.
- Selecionar uma tarefa ao iniciar uma sessao.
- Permitir sessoes sem tarefa vinculada.
- Sugerir marcar uma tarefa como concluida ao finalizar uma sessao vinculada.
- Contar sessoes por tarefa.
- Manter tarefas concluidas visiveis com menor destaque.
- Excluir tarefas imediatamente, preservando o historico de sessoes ja salvo.

Fora do MVP: subtarefas, labels, prioridades, prazos, anexos, comentarios e colaboracao.

### Streak diario

O streak representa a sequencia de dias com pelo menos uma sessao concluida.

Regras:

- Uma sessao concluida no dia mantem ou aumenta o streak.
- Varias sessoes no mesmo dia contam como um unico dia.
- Sessoes abandonadas nao contam para streak.
- Abandonar nao quebra o streak.
- Se nenhum foco for concluido em um dia, o streak e zerado na proxima abertura do app.
- O recorde `longestEver` e atualizado quando o streak atual supera o maior valor anterior.

### Niveis de streak

O indicador de streak usa os niveis abaixo. A imagem central da chama permanece
a mesma imagem da V1.

| Nivel | Streak | Nome |
| --- | --- | --- |
| 1 | 0 dias | Spark |
| 2 | 1 a 3 dias | Kindling |
| 3 | 4 a 7 dias | Flame |
| 4 | 8 a 20 dias | Torch |
| 5 | 21+ dias | Bonfire |

### Fogueira historica

O historico deve ser visual, nao apenas uma lista.

No MVP:

- Mostrar cada dia como brasa, chama ou cinza.
- Dias com sessoes concluidas aparecem acesos.
- Dias sem sessao aparecem apagados.
- Dias apenas com sessoes abandonadas aparecem como cinzas.
- Clicar em um dia mostra as sessoes daquele dia.
- Cada sessao exibe duracao, tarefa vinculada e horario aproximado.

## Design

O Vesta deve parecer escuro, concentrado e vivo. A interface deve valorizar contraste, silencio visual e movimento controlado.

### Buscar

- Sensacao de ritual.
- Poucos elementos, bem hierarquizados.
- Tipografia forte para timer e marca.
- Microinteracoes sutis.
- Historico visual facil de entender.

### Evitar

- Aparencia de dashboard corporativo.
- Excesso de cards.
- Excesso de metricas no primeiro contato.
- Gamificacao infantilizada.
- Efeitos visuais pesados demais no MVP.

### Paleta

| Papel | Cor |
| --- | --- |
| Background primario | `#0A0A0A` |
| Background secundario | `#111111` |
| Acento fogo | `#E8540A` |
| Acento ambar | `#F5A623` |
| Dourado | `#FFB347` |
| Violeta profundo | `#6D3BFF` |
| Azul-branco | `#DDEBFF` |
| Texto primario | `#FFFFFF` |
| Texto secundario | `#A0A0A0` |
| Cinza apagado | `#2B2B2B` |

## Arquitetura de dados

No MVP, os dados ficam no navegador usando `localStorage`.

| Entidade | Chave | Campos principais |
| --- | --- | --- |
| Tarefas | `vesta_tasks` | `id`, `text`, `completed`, `sessionsCount`, `createdAt`, `updatedAt` |
| Sessoes | `vesta_sessions` | `id`, `date`, `durationMinutes`, `taskId`, `status`, `startedAt`, `endedAt` |
| Streak | `vesta_streak` | `current`, `lastSessionDate`, `longestEver` |
| Configuracoes | `vesta_settings` | `defaultDuration`, `soundEnabled` |
| Sessao ativa | `vesta_active_session` | Estado da sessao em andamento ou pausada |

Exemplo de sessao:

```json
{
  "id": "session_001",
  "date": "2026-06-16",
  "durationMinutes": 25,
  "taskId": "task_001",
  "status": "completed",
  "startedAt": "2026-06-16T14:00:00.000Z",
  "endedAt": "2026-06-16T14:25:00.000Z"
}
```

## Stack tecnica recomendada

| Camada | Tecnologia |
| --- | --- |
| Frontend | React + TypeScript |
| Build | Vite |
| Estilizacao | Tailwind CSS ou CSS Modules |
| Animacoes | CSS keyframes e Framer Motion opcional |
| Chama visual | Imagem PNG preservada da V1 |
| Som | Web Audio API |
| Persistencia | localStorage |
| Timer | Date.now() + setInterval |
| Deploy | Vercel, Netlify ou GitHub Pages |

Para o MVP, a recomendacao e evitar backend. O objetivo inicial e provar a experiencia central antes de adicionar autenticacao, sincronizacao, pagamentos ou analytics avancado.

## Fora do escopo do MVP

- Autenticacao.
- Backend.
- Sincronizacao entre dispositivos.
- App blocker.
- Modo Pomodoro com pausas automaticas.
- Analytics detalhados.
- Calendario completo.
- Temas customizaveis.
- Moedas, loja, inventario ou conquistas complexas.
- Funcionalidades sociais.
- Colaboracao em tempo real.
- Aplicativo mobile nativo.

## Roadmap

| Fase | Objetivo | Funcionalidades |
| --- | --- | --- |
| v1.0 MVP | Provar a experiencia central | Timer, chama, tarefas, streak, fogueira de 14 dias e localStorage |
| v1.1 | Melhorar leitura do progresso | Historico mensal, filtros por tarefa e totais semanais |
| v1.2 | Personalizacao leve | Sons, frases customizadas e presets de duracao |
| v2.0 | Conta e continuidade | Autenticacao, sincronizacao em nuvem e uso multi-dispositivo |
| v2.x | Expansao de produto | Analytics avancado, app blocker, calendario e social opcional |

## Criterios de sucesso do MVP

O MVP sera considerado bem-sucedido se:

- O usuario entender em menos de 10 segundos que deve iniciar uma sessao para alimentar a chama.
- Completar uma sessao gerar sensacao clara de progresso.
- O estado da sessao e o progresso de streak serem compreensiveis na interface.
- O usuario conseguir criar tarefas e vincular sessoes sem friccao.
- O streak for compreensivel sem explicacao longa.
- A fogueira historica fizer o usuario querer voltar para ver o que construiu.
- O app funcionar sem conta, sem internet e sem configuracao complexa.

## Evolucao futura com backend

Apos o MVP, o Vesta pode evoluir para uma arquitetura SaaS com backend. Nessa fase, Spring Boot passa a ser uma boa opcao para autenticar usuarios, sincronizar dados entre dispositivos, persistir sessoes em banco de dados e preparar recursos pagos.

Uma stack futura possivel:

- Frontend: React + TypeScript.
- Backend: Spring Boot.
- Banco de dados: MySQL.
- Autenticacao: Spring Security com JWT ou login social.
- Deploy: frontend em Vercel/Netlify e backend em Render, Railway, Fly.io ou AWS.

## Status

Os modos Pomodoro e Stopwatch estao funcionais no frontend React. Pomodoro
inclui tarefas, streak, fogueira de 14 dias e configuracoes; Stopwatch inclui
pausa, retomada, voltas e persistencia local. A aba Timer continua exibindo uma
pagina minima de progresso enquanto seu modo dedicado e desenvolvido.

O backend opcional esta em `vesta-api`: Spring Boot 3, PostgreSQL, Flyway,
autenticacao com refresh token rotativo, tarefas, preferencias, sessoes,
historico, streak derivado, importacao idempotente e sincronizacao incremental.
O frontend continua funcionando sem conta e oferece conexao com a nuvem sem
apagar os dados locais antes da confirmacao do servidor. Consulte
`vesta-api/README.md` para executar a API.
