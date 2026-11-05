# Documentos comuns às três disciplinas

| Arquivo | Conteúdo |
| --- | --- |
| [`ERS.md`](ERS.md) | Especificação de Requisitos de Software do Mellona, versão 1.1.2. O histórico de versões no início do arquivo diz o que mudou em cada uma |
| [`diagramas/`](diagramas/) | Diagramas do ERS em PlantUML (`.puml`) e imagem (`.png`) |

| Diagrama | Seção do ERS |
| --- | --- |
| [Modelo conceitual](diagramas/modelo-conceitual-mellona.png) | 13.3 |
| [Casos de uso](diagramas/casos-de-uso-mellona.png) | 14.1 |
| [Decisão de sinalização](diagramas/atividade-sinalizacao.png) | 14.2 |
| [Autorização de visita](diagramas/atividade-autorizacao-visita.png) | 14.3 |
| [Estados da segurança](diagramas/estados-seguranca-apiario.png) | 14.4 |

O `.puml` é a fonte. Quem mudar um diagrama no ERS deve gerar o `.png` de novo, por exemplo com `plantuml diagramas/*.puml`.
