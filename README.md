# Mellona — monitoramento e proteção de apiários

O Mellona monitora e protege apiários em áreas rurais. Sensores na área do apiário e em cada colmeia mandam eventos para uma central no próprio apiário, que decide e aciona sirene e luz mesmo sem internet. Um serviço remoto guarda o histórico, avisa o apicultor pelo aplicativo e percebe quando um apiário fica em silêncio.

É um projeto único para três disciplinas do INF/UFG, feito individualmente.

| Item | Registro |
| --- | --- |
| Autora | Bárbara Nogueira · GitHub: [BarbaraNogueiraCS](https://github.com/BarbaraNogueiraCS) |
| Repositório | [BarbaraNogueiraCS/mellona](https://github.com/BarbaraNogueiraCS/mellona.git) |
| Disciplinas | Interação Humano-Computador (IHC), Software para Persistência de Dados (SPD) e Software para Sistemas Ubíquos (SSU) |
| Requisitos | [ERS do Mellona, versão 1.1.2](docs/ERS.md) |

## O projeto em cinco pontos

Os cinco itens que a Aula 01 de SSU pede no README. O detalhe está na [Atividade 01 de SSU](ssu/atividade-01.md).

| Item | No Mellona |
| --- | --- |
| Problema | Apiários ficam longe, e furtos, vandalismo e caixas tombadas só são descobertos dias depois, com a colônia perdida ou enfraquecida |
| Usuário | Apicultor (principal), técnico de cooperativa e administrador. Indiretos: auxiliares, donos das terras, vizinhança e as próprias colônias |
| Contexto | Presença na área, movimento e inclinação da colmeia, período do dia, visita autorizada, estado da proteção, saúde dos dispositivos e conectividade |
| Resposta | Presença e colmeia mexida no mesmo apiário, com até 60 s de diferença, acionam a sirene por 30 s, e à noite também a luz. O apicultor recebe o alerta no aplicativo e pode ativar o modo visita para trabalhar sem disparar a sinalização |
| Riscos | Principal: errar a decisão de segurança para os dois lados, com alarme falso ou silêncio durante um furto. Também: conectividade rural, energia em campo, sigilo da localização e bem-estar das abelhas |

## Por disciplina

| Disciplina | Pasta | Entregas | Situação |
| --- | --- | --- | --- |
| Interação Humano-Computador | [`ihc/`](ihc/) | PF1: planejamento e coleta · PF2: guia de estilo e protótipo · PF3: avaliação | PF1 e PF2 prontas; PF3 a fazer |
| Software para Persistência de Dados | [`spd/`](spd/) | Etapa 1: planejamento, modelagem e código inicial (Maven, ORMLite, Spring Data MongoDB) | Etapa 1 com documento e código inicial; integrador Redis na Etapa 2 |
| Software para Sistemas Ubíquos | [`ssu/`](ssu/) | Atividades 01, 02 e 03 | 01 e 02 prontas; 03 com código pronto, falta executar no Wokwi |
| Comum às três | [`docs/`](docs/) | ERS e diagramas UML (PlantUML) | Versão 1.1.2 |

## Como as três partes se ligam

- **O ERS é a fonte única** de histórias, regras (RN), parâmetros (PAR) e do contrato de eventos. As três disciplinas citam os mesmos identificadores.
- **SSU e SPD usam o mesmo evento.** O contrato da [Atividade 02 de SSU](ssu/atividade-02.md) (`eventId`, `deviceId`, `entityId`, `eventTime`, `sequence`, `quality`) é o que a API de monitoramento de SPD grava no MongoDB, com índice único em `eventId`.
- **IHC dá o vocabulário.** Os textos dos alertas, do modo visita e dos estados seguem o guia de estilo da PF2.

## Estrutura

```
.
├── README.md                     este arquivo
├── docs/
│   ├── ERS.md                    especificação de requisitos (fonte única)
│   └── diagramas/                modelo conceitual, casos de uso, atividades e estados (.puml e .png)
├── ihc/
│   ├── pf1-planejamento/         PF1 e diagramas de IHC
│   ├── pf2-prototipo/            guia de estilo e telas; protótipos e análise da PF2; imagens
│   └── pf3-avaliacao/            a fazer
├── spd/                          projeto Maven: gestão (JavaFX + SQLite), monitoramento (API + MongoDB), integrador
└── ssu/
    ├── atividade-01.md           análise inicial do sistema ubíquo
    ├── atividade-02.md           eventos, janela temporal e distribuição
    └── atividade-03/             protótipo ESP32 no Wokwi: decisão e atuação
```

## Convenções

- **Uma tag por entrega:** `ihc-pf1`, `ihc-pf2`, `ihc-pf3`, `spd-etapa-1`, `spd-etapa-2`, `ssu-atividade-01`, `ssu-atividade-02`, `ssu-atividade-03`. Uma Release na tag pode guardar o PDF ou o ZIP enviado ao SIGAA.
- **Commits com o prefixo da disciplina:** `ihc:`, `spd:`, `ssu:` e `docs:`. Para ver só uma disciplina: `git log -- spd/`.
- **Fonte e registro:** Markdown, PlantUML e código são a fonte. O PDF registra o que foi entregue.
- **Sem versão no nome do arquivo:** a versão do ERS fica no histórico de versões dentro dele, e o git guarda o resto.
