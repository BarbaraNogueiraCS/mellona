# Atividade 03 — Protótipo individual com ESP32 e Wokwi

Central do apiário A1 num ESP32. Responsabilidade assumida: **Decisão e atuação** (ERS, D.2.1).

| Item | Registro |
| --- | --- |
| Estudante | Bárbara Nogueira · matrícula _a preencher_ |
| Projeto no Wokwi | _colar o link compartilhável_ |
| Relatório | `relatorio.pdf`, adicionado depois da entrega |
| Teste adversarial | Final da matrícula _a preencher_ |

## O que o protótipo faz

O PIR representa o sensor de presença e o MPU6050 representa o nó da colmeia A1-07. A central confirma cada detecção: 2 ativações em 10 s para a presença, e 5° ou 0,3 g mantidos por 2 s para a colmeia. Depois procura o par numa janela de 60 s pelo tempo do evento.

Com a segurança armada e sem modo visita, o par aciona a sirene (buzzer) por 30 s. À noite, pelo DS1307, acende também a luz da área (relé com LED). Em seguida vem um intervalo de proteção de 120 s, em que ocorrências novas são só vinculadas.

O monitor serial mostra os eventos em JSON (`eventType`, `deviceId`, `entityId`, `eventTimeMs`, `sequence`, `value`, `unit` e `state`, mais os campos do ERS). Cada decisão sai com o motivo.

## Arquivos

| Arquivo | Conteúdo |
| --- | --- |
| [`sketch.ino`](sketch.ino) | Código da central, com os parâmetros do ERS como constantes (`PAR07_...` a `PAR22_...`) |
| [`diagram.json`](diagram.json) | Circuito do Wokwi: ESP32 DevKitC V4, PIR, MPU6050 (0x69), DS1307 (0x68), buzzer, relé, LEDs, botão e chave |
| [`libraries.txt`](libraries.txt) | ArduinoJson |
| `evidencias/` | `teste-normal.png`, `teste-decisao.png` e `teste-adversarial.png`, depois da execução |

## Como executar

1. Em wokwi.com, criar um projeto de ESP32 com Arduino e colar `sketch.ino` e `diagram.json`.
2. Adicionar a biblioteca ArduinoJson pelo Library Manager.
3. Iniciar a simulação. O primeiro evento deve ser `seguranca.estado` com `state` `ARMADA`; se vier `DESARMADA`, mover a chave.
4. Agir sobre o circuito: "Simulate motion" no PIR, `accelX` no MPU6050 (0,13 g inclina 7,4°), botão verde para o modo visita, chave para armar e desarmar.

| Comando no monitor serial | Efeito |
| --- | --- |
| `hora HH:MM` | Ajusta o DS1307 e informa o período do dia |
| `relogio falha` e `relogio ok` | Simulam a perda e a volta do horário local |
| `atraso S`, `dup` e `perda` | A próxima mensagem do nó chega S segundos depois, chega duas vezes ou se perde |
| `estado` | Imprime as variáveis de estado da central |
