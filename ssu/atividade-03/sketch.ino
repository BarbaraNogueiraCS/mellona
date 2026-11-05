/*
 * Mellona — Software para Sistemas Ubíquos — Atividade 03
 * Protótipo individual: central do apiário A1 (ESP32 + Wokwi)
 * Bárbara Nogueira · Responsabilidade: Decisão e atuação
 *
 * Ciclo implementado (enunciado, seção 1):
 *   pessoa no apiário e colmeia mexida  →  PIR e MPU6050
 *   →  confirmação de cada detecção (PAR-07, PAR-08) e validação da leitura
 *   →  eventos JSON com identidade, tempo, sequência, valor e unidade
 *   →  janela de 60 s pelo tempo do evento (RN-61, RN-65, PAR-10, PAR-21)
 *   →  decisão: armada, sem modo visita, fora do intervalo de proteção (RN-31, RN-69)
 *   →  sirene por 30 s; à noite, sirene e luz (RN-33, RN-58, RN-59)
 *
 * Simplificações declaradas no relatório:
 *   - Um único ESP32 faz três papéis: sensor de presença, nó da colmeia A1-07 e
 *     central do apiário. O rádio entre o nó e a central é simulado em software.
 *   - eventTimeMs é o tempo desde o início da simulação (millis()). O DS1307 só
 *     define o período do dia e o horário legível (eventTime).
 *   - A chave representa o comando de armar e desarmar vindo do aplicativo, e o
 *     botão representa o código do modo visita já aceito pela central.
 */

#include <Wire.h>
#include <ArduinoJson.h>
#include <math.h>
#include <stdarg.h>
#include <limits.h>

// ---------------------------------------------------------------- pinos
const int PINO_PIR          = 27;  // sensor de presença (saída digital)
const int PINO_CHAVE        = 18;  // chave da proteção: LOW = ligada (pull-up interno)
const int PINO_BOTAO_VISITA = 4;   // botão do modo visita: LOW = pressionado
const int PINO_BUZZER       = 25;  // sirene
const int PINO_RELE_LUZ     = 26;  // relé da luz da área (LED no contato NO)
const int PINO_LED_VISITA   = 32;  // verde: modo visita ativo
const int PINO_LED_ATENCAO  = 33;  // amarelo: armada com uma evidência na janela
const int PINO_LED_FALHA    = 14;  // vermelho: sensor em falha
const int PINO_SDA = 21, PINO_SCL = 22;

const int END_RTC = 0x68;  // DS1307
const int END_MPU = 0x69;  // MPU6050 com AD0 em VCC, porque o DS1307 já usa 0x68

// ---------------------------------------------------------------- parâmetros (ERS, Seção 12)
const int   PAR07_ATIVACOES      = 2;       // presença: 2 ativações...
const long  PAR07_JANELA_MS      = 10000;   // ...em até 10 s
const float PAR08_GRAUS          = 5.0;     // movimentação: inclinação >= 5°...
const float PAR08_G              = 0.3;     // ...ou variação de aceleração >= 0,3 g...
const long  PAR08_PERSISTENCIA_MS = 2000;   // ...mantida por 2 s
const long  PAR10_JANELA_MS      = 60000;   // correlação: até 60 s entre os tempos do evento
const long  PAR11_SINALIZACAO_MS = 30000;   // sirene (e luz) por 30 s
const long  PAR12_PROTECAO_MS    = 120000;  // intervalo de proteção após a sinalização
const long  PAR13_VISITA_MS      = 60L * 60 * 1000;  // modo visita: 60 min
const int   PAR14_INICIO_DIA     = 6 * 60;           // dia: 06:00...
const int   PAR14_FIM_DIA        = 17 * 60 + 59;     // ...até 17:59
const long  PAR19_TRAVADO_MS     = 10L * 60 * 1000;  // PIR ativo sem parar por 10 min
const long  PAR21_TOLERANCIA_MS  = 5000;    // tolerância de atraso da marca d'água
const float PAR22_MODULO_MIN_G   = 0.2;     // proposta: módulo abaixo disso é impossível
const int   PAR22_AMOSTRAS       = 10;      // proposta: 10 amostras (1 s) mudam a validade
const long  AMOSTRA_MPU_MS       = 100;     // amostragem do acelerômetro: 10 Hz
const long  DEBOUNCE_MS          = 50;

// ---------------------------------------------------------------- identificação
const char *ID_PRESENCA = "presenca-apiario-A1";
const char *ID_NO       = "no-colmeia-A1-07";
const char *ID_CENTRAL  = "seguranca-apiario-A1";
const char *APIARIO     = "apiario:A1";
const char *COLMEIA     = "colmeia:A1-07";

// ---------------------------------------------------------------- tipos
enum EstadoSeguranca { DESARMADA, ARMADA, MANUTENCAO, SINALIZANDO, PROTECAO };
const char *NOME_ESTADO[] = {"DESARMADA", "ARMADA", "MANUTENCAO", "SINALIZANDO", "PROTECAO"};

struct Evento {              // detecção produzida pelo sensor de presença ou pelo nó
  char tipo[28];             // eventType
  char dispositivo[24];      // deviceId
  char entidade[16];         // entityId
  char id[37];               // eventId (UUID)
  long tMs;                  // eventTimeMs: instante da ocorrência
  unsigned long seq;         // sequence, por dispositivo
  float valor;               // value
  char unidade[10];          // unit
  char hora[26];             // eventTime legível (vazio sem relógio válido)
};

struct Registro {            // detecção guardada na janela da central
  bool ocupado;
  char id[37];
  long tMs;
};

struct Mensagem {            // mensagem no rádio simulado entre o nó e a central
  bool ocupada;
  Evento e;
  long entregarEmMs;
};

struct Relogio { bool valido; int ano, mes, dia, hora, min, seg; };

// ---------------------------------------------------------------- estado da central
EstadoSeguranca estado = DESARMADA;
bool protecaoLigada  = false;  // posição da chave
bool degradada       = false;  // RN-72: sensor de presença travado
long armadaDesdeMs   = 0;      // rearme: detecções anteriores não formam par
long sinalizandoAteMs = 0, protecaoAteMs = 0, visitaAteMs = 0;
long sinalizacaoInicioMs = 0;
char ocorrenciaAtual[24] = "";
bool somLigado = false, luzLigada = false;

Registro janelaPresencas[6], janelaMovimentos[6];  // janela de 60 s do apiário A1
char vistos[16][37];                               // eventId já processados (RN-67)
int  proximoVisto = 0;
unsigned long ultimaSeqNo = 0, ultimaSeqPresenca = 0;
unsigned long seqCentral = 0;
int  numeroOcorrencia = 0;

// ---------------------------------------------------------------- estado dos produtores
unsigned long seqPresenca = 0, seqNo = 0;
bool pirAnterior = false;
long pirAltoDesdeMs = -1;
long ativacoes[8];
int  nAtivacoes = 0;

bool  mpuIniciado = false, colmeiaEmFalha = false;
int   invalidasSeguidas = 0, validasSeguidas = 0;
const char *motivoFalha = "";
bool  referenciaPronta = false;
float refX = 0, refY = 0, refZ = 0, somaX = 0, somaY = 0, somaZ = 0;
int   amostrasReferencia = 0;
bool  emCondicao = false, confirmadaNaCondicao = false;
long  inicioCondicaoMs = 0;
float maiorGraus = 0, maiorG = 0;
long  ultimaAmostraMpuMs = 0, ultimaTentativaMpuMs = 0;

// rádio simulado e comandos de teste
Mensagem filaRadio[6];
long atrasoProximoMs = 0;
bool duplicarProximo = false, perderProximo = false;
bool relogioFalhaSimulada = false;
bool relogioEstavaValido = true;
long ultimaLeituraRelogioMs = -1000;

// controles com debounce
int  chaveEstavel = HIGH, chaveBruta = HIGH;  long chaveMudouMs = 0;
int  botaoEstavel = HIGH, botaoBruto = HIGH;  long botaoMudouMs = 0;
bool tomAlto = false; long ultimaTrocaTomMs = 0, bipeAteMs = 0;

char comando[40]; int nComando = 0;

// ================================================================ utilidades
void nota(const char *fmt, ...) {  // linha de diagnóstico legível (não é evento)
  char buf[200];
  va_list args; va_start(args, fmt); vsnprintf(buf, sizeof(buf), fmt, args); va_end(args);
  Serial.print("# ");
  Serial.println(buf);
}

void novoId(char *s) {  // UUID versão 4
  unsigned int p[8];
  for (int i = 0; i < 8; i++) p[i] = (unsigned int)random(65536);
  p[3] = (p[3] & 0x0FFF) | 0x4000;
  p[4] = (p[4] & 0x3FFF) | 0x8000;
  snprintf(s, 37, "%04x%04x-%04x-%04x-%04x-%04x%04x%04x",
           p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7]);
}

uint8_t bcdParaDec(uint8_t v) { return (v >> 4) * 10 + (v & 0x0F); }
uint8_t decParaBcd(int v) { return (uint8_t)(((v / 10) << 4) | (v % 10)); }

bool lerRegistros(int end, uint8_t reg, uint8_t *buf, int n) {
  Wire.beginTransmission(end);
  Wire.write(reg);
  if (Wire.endTransmission() != 0) return false;
  if (Wire.requestFrom(end, n) != n) return false;
  for (int i = 0; i < n; i++) buf[i] = Wire.read();
  return true;
}

bool escreverRegistro(int end, uint8_t reg, uint8_t valor) {
  Wire.beginTransmission(end);
  Wire.write(reg);
  Wire.write(valor);
  return Wire.endTransmission() == 0;
}

// ================================================================ relógio (DS1307)
Relogio lerRelogio() {
  Relogio r = {false, 0, 0, 0, 0, 0, 0};
  uint8_t b[7];
  if (relogioFalhaSimulada || !lerRegistros(END_RTC, 0x00, b, 7)) return r;
  if (b[0] & 0x80) return r;  // bit CH: oscilador parado
  r.seg = bcdParaDec(b[0] & 0x7F); r.min = bcdParaDec(b[1]); r.hora = bcdParaDec(b[2] & 0x3F);
  r.dia = bcdParaDec(b[4]); r.mes = bcdParaDec(b[5]); r.ano = 2000 + bcdParaDec(b[6]);
  r.valido = r.ano >= 2024 && r.mes >= 1 && r.mes <= 12 && r.hora < 24 && r.min < 60 && r.seg < 60;
  return r;
}

void ajustarRelogio(int h, int m, int s) {  // comando de teste "hora HH:MM"
  Relogio atual = lerRelogio();
  int dia = atual.valido ? atual.dia : 21, mes = atual.valido ? atual.mes : 10;
  int ano = atual.valido ? atual.ano : 2026;
  Wire.beginTransmission(END_RTC);
  Wire.write((uint8_t)0x00);
  Wire.write(decParaBcd(s));  // CH = 0: oscilador ligado
  Wire.write(decParaBcd(m));
  Wire.write(decParaBcd(h));  // modo 24 h
  Wire.write(decParaBcd(1));
  Wire.write(decParaBcd(dia));
  Wire.write(decParaBcd(mes));
  Wire.write(decParaBcd(ano - 2000));
  Wire.endTransmission();
}

void horaLegivel(char *s) {  // "2026-10-21T02:14:13-03:00", ou vazio sem relógio válido
  Relogio r = lerRelogio();
  if (!r.valido) { s[0] = '\0'; return; }
  snprintf(s, 26, "%04d-%02d-%02dT%02d:%02d:%02d-03:00", r.ano, r.mes, r.dia, r.hora, r.min, r.seg);
}

const char *periodoDoDia() {  // RN-60 e PAR-14; sem horário válido, noite (RN-64)
  Relogio r = lerRelogio();
  if (!r.valido) return "NOTURNO";
  int minutos = r.hora * 60 + r.min;
  return (minutos >= PAR14_INICIO_DIA && minutos <= PAR14_FIM_DIA) ? "DIURNO" : "NOTURNO";
}

// ================================================================ eventos JSON
const char *estadoPublicado() {
  bool temEvidencia = false;  // só conta o que ainda pode formar par (depois do rearme)
  for (int i = 0; i < 6; i++) {
    if (janelaPresencas[i].ocupado && janelaPresencas[i].tMs >= armadaDesdeMs) temEvidencia = true;
    if (janelaMovimentos[i].ocupado && janelaMovimentos[i].tMs >= armadaDesdeMs) temEvidencia = true;
  }
  if (estado == ARMADA && degradada) return "DEGRADADA";
  if (estado == ARMADA && temEvidencia) return "ATENCAO";
  return NOME_ESTADO[estado];
}

void cabecalho(JsonDocument &doc, const char *tipo, const char *disp, const char *ent,
               long tMs, unsigned long seq) {
  doc["eventType"] = tipo;
  doc["deviceId"] = disp;
  doc["entityId"] = ent;
  doc["eventTimeMs"] = tMs;
  doc["sequence"] = seq;
}

void rodape(JsonDocument &doc, const char *id, const char *hora, const char *qualidade) {
  doc["schemaVersion"] = 1;
  doc["eventId"] = id;
  if (hora[0]) { doc["eventTime"] = hora; doc["timeSource"] = "RTC"; }
  else         { doc["eventTime"] = nullptr; doc["timeSource"] = "RELATIVO"; }
  doc["quality"] = qualidade;
}

void emitir(JsonDocument &doc) {
  char buf[1024];
  serializeJson(doc, buf, sizeof(buf));
  Serial.println(buf);
}

// Evento publicado pelo produtor (sensor de presença ou nó). valor NAN = value null.
void emitirDeteccao(const Evento &e, const char *estadoDet, const char *qualidade,
                    const char *motivo) {
  JsonDocument doc;
  cabecalho(doc, e.tipo, e.dispositivo, e.entidade, e.tMs, e.seq);
  if (isnan(e.valor)) doc["value"] = nullptr; else doc["value"] = e.valor;
  doc["unit"] = e.unidade;
  doc["state"] = estadoDet;
  rodape(doc, e.id, e.hora, qualidade);
  if (strcmp(e.tipo, "presenca.confirmada") == 0) doc["activations"] = (int)e.valor;
  if (motivo) doc["motivo"] = motivo;
  emitir(doc);
}

// Evento da central. "valor" < 0 indica ausência de valor (value = null).
void emitirCentral(const char *tipo, float valor, const char *unidade, const char *motivo,
                   JsonDocument &extra) {
  long agora = millis();
  char id[37], hora[26];
  novoId(id); horaLegivel(hora);
  JsonDocument doc;
  cabecalho(doc, tipo, ID_CENTRAL, APIARIO, agora, ++seqCentral);
  if (valor >= 0) doc["value"] = valor; else doc["value"] = nullptr;
  if (unidade) doc["unit"] = unidade; else doc["unit"] = nullptr;
  doc["state"] = estadoPublicado();
  rodape(doc, id, hora, "VALIDO");
  if (degradada) doc["degradada"] = true;
  for (JsonPair kv : extra.as<JsonObject>()) doc[kv.key()] = kv.value();
  doc["motivo"] = motivo;
  emitir(doc);
}

void publicarEstado(const char *motivo) {
  JsonDocument extra;
  emitirCentral("seguranca.estado", -1, nullptr, motivo, extra);
}

// ================================================================ atuação
void ligarSinalizacao(bool comLuz) {
  somLigado = true;
  tomAlto = false; ultimaTrocaTomMs = millis();
  tone(PINO_BUZZER, 880);
  if (comLuz) { digitalWrite(PINO_RELE_LUZ, HIGH); luzLigada = true; }
}

void desligarSinalizacao() {
  somLigado = false;
  noTone(PINO_BUZZER);
  digitalWrite(PINO_RELE_LUZ, LOW);
  luzLigada = false;
}

void encerrarSinalizacao(long agora, const char *motivo) {
  bool tinhaLuz = luzLigada;
  desligarSinalizacao();
  JsonDocument extra;
  extra["correlationId"] = ocorrenciaAtual;
  JsonArray rec = extra["resources"].to<JsonArray>();
  rec.add("SOM"); if (tinhaLuz) rec.add("LUZ");
  emitirCentral("sinalizacao.encerrada", (agora - sinalizacaoInicioMs) / 1000.0, "s", motivo, extra);
}

// ================================================================ janela e decisão
bool jaVisto(const char *id) {
  for (int i = 0; i < 16; i++) if (strcmp(vistos[i], id) == 0) return true;
  return false;
}

void marcarVisto(const char *id) {
  strncpy(vistos[proximoVisto], id, 36); vistos[proximoVisto][36] = '\0';
  proximoVisto = (proximoVisto + 1) % 16;
}

long marcaDagua() { return (long)millis() - PAR21_TOLERANCIA_MS; }

void guardar(Registro *janela, const Evento &e) {
  int alvo = 0;
  for (int i = 0; i < 6; i++) {  // vaga livre, ou a detecção mais antiga
    if (!janela[i].ocupado) { alvo = i; break; }
    if (janela[i].tMs < janela[alvo].tMs) alvo = i;
  }
  janela[alvo].ocupado = true;
  strncpy(janela[alvo].id, e.id, 36); janela[alvo].id[36] = '\0';
  janela[alvo].tMs = e.tMs;
}

// Complemento mais próximo no tempo, com até 60 s de diferença (RN-61, PAR-10),
// considerando só detecções ocorridas a partir de "desdeMs"
Registro *complemento(Registro *janela, long tMs, long desdeMs) {
  Registro *melhor = nullptr;
  long melhorDif = PAR10_JANELA_MS + 1;
  for (int i = 0; i < 6; i++) {
    if (!janela[i].ocupado || janela[i].tMs < desdeMs) continue;
    long dif = janela[i].tMs - tMs; if (dif < 0) dif = -dif;
    if (dif <= PAR10_JANELA_MS && dif < melhorDif) { melhor = &janela[i]; melhorDif = dif; }
  }
  return melhor;
}

void expirarJanela() {
  long limite = marcaDagua() - PAR10_JANELA_MS;
  for (int k = 0; k < 2; k++) {
    Registro *janela = k == 0 ? janelaPresencas : janelaMovimentos;
    for (int i = 0; i < 6; i++) {
      if (janela[i].ocupado && janela[i].tMs < limite) {
        janela[i].ocupado = false;
        nota("janela: %s de t=%ld ms expirou (60 s + 5 s de tolerancia)",
             k == 0 ? "presenca" : "movimentacao", janela[i].tMs);
      }
    }
  }
}

void avaliar(const Evento &d, const char *qualidade, const char *resultado, const char *motivo,
             float dtS, const char *correlacao) {
  JsonDocument extra;
  JsonObject det = extra["deteccao"].to<JsonObject>();
  det["eventId"] = d.id;
  det["eventType"] = d.tipo;
  det["eventTimeMs"] = d.tMs;
  det["sequence"] = d.seq;
  det["quality"] = qualidade;
  extra["receivedAtMs"] = (long)millis();
  extra["resultado"] = resultado;
  if (correlacao && correlacao[0]) extra["correlationId"] = correlacao;
  emitirCentral("seguranca.avaliacao", dtS, dtS >= 0 ? "s" : nullptr, motivo, extra);
}

void acionar(const Evento &e, const Registro *par, float dtS, long agora) {
  const char *periodo = periodoDoDia();
  bool comLuz = strcmp(periodo, "NOTURNO") == 0;
  bool relogioOk = lerRelogio().valido;
  ligarSinalizacao(comLuz);  // resposta em até 2 s (PAR-17)
  estado = SINALIZANDO;
  sinalizacaoInicioMs = agora;
  sinalizandoAteMs = agora + PAR11_SINALIZACAO_MS;
  snprintf(ocorrenciaAtual, sizeof(ocorrenciaAtual), "ocr-A1-%04d", ++numeroOcorrencia);

  char motivo[160];
  snprintf(motivo, sizeof(motivo), "presenca e colmeia A1-07 com %.0f s de diferenca; %s%s",
           dtS, comLuz ? "periodo noturno: sirene e luz" : "periodo diurno: so sirene",
           relogioOk ? "" : " (sem horario valido, noite adotada: RN-64)");
  long maisRecente = e.tMs > par->tMs ? e.tMs : par->tMs;

  avaliar(e, "VALIDO", "ACIONADA", motivo, dtS, ocorrenciaAtual);
  JsonDocument extra;
  extra["correlationId"] = ocorrenciaAtual;
  JsonArray rel = extra["relatedEventIds"].to<JsonArray>();
  rel.add(par->id); rel.add(e.id);
  extra["period"] = periodo;
  JsonArray rec = extra["resources"].to<JsonArray>();
  rec.add("SOM"); if (comLuz) rec.add("LUZ");
  extra["latenciaMs"] = agora - maisRecente;
  emitirCentral("ocorrencia.suspeita", dtS, "s", motivo, extra);

  JsonDocument extra2;
  extra2["correlationId"] = ocorrenciaAtual;
  JsonArray rec2 = extra2["resources"].to<JsonArray>();
  rec2.add("SOM"); if (comLuz) rec2.add("LUZ");
  emitirCentral("sinalizacao.iniciada", PAR11_SINALIZACAO_MS / 1000.0, "s",
                comLuz ? "sirene e luz por 30 s (RN-59)" : "sirene por 30 s (RN-58)", extra2);
}

// Ponto de entrada de toda detecção que chega à central (pelo fio ou pelo rádio)
void receberDeteccao(const Evento &e) {
  long agora = millis();
  bool ehPresenca = strcmp(e.tipo, "presenca.confirmada") == 0;

  // 1. Repetição: mesmo eventId já processado (RN-67)
  if (jaVisto(e.id)) {
    avaliar(e, "VALIDO", "DESCARTADA", "eventId repetido: descartado sem efeito (RN-67)", -1, nullptr);
    return;
  }
  marcarVisto(e.id);

  // 2. Sequência por dispositivo: salto = possível perda; menor = fora de ordem
  unsigned long &ultima = ehPresenca ? ultimaSeqPresenca : ultimaSeqNo;
  char obsSeq[100] = "";
  if (e.seq > ultima + 1)
    snprintf(obsSeq, sizeof(obsSeq), "; salto de sequencia %lu -> %lu: %lu evento(s) possivelmente perdido(s)",
             ultima, e.seq, e.seq - ultima - 1);
  else if (e.seq < ultima)
    snprintf(obsSeq, sizeof(obsSeq), "; fora de ordem (ultima sequencia %lu): avaliado pelo tempo do evento", ultima);
  if (e.seq > ultima) ultima = e.seq;

  char motivo[300];
  // Leitura inválida ou normalizada do nó: só registra (o nó já parou de confirmar movimentações)
  if (strncmp(e.tipo, "leitura.", 8) == 0) {
    bool invalida = strcmp(e.tipo, "leitura.invalida") == 0;
    snprintf(motivo, sizeof(motivo), "%s%s",
             invalida ? "colmeia A1-07 sem leitura valida: fica fora da correlacao ate normalizar (RN-07)"
                      : "colmeia A1-07 com leitura valida de novo", obsSeq);
    avaliar(e, invalida ? "INVALIDO" : "VALIDO", "REGISTRADA", motivo, -1, nullptr);
    return;
  }

  // 3. Atrasado: a janela a que pertence já fechou (RN-66, PAR-21)
  if (e.tMs < marcaDagua() - PAR10_JANELA_MS) {
    snprintf(motivo, sizeof(motivo), "chegou %ld s depois de ocorrer; janela fechada: registrada sem sinalizacao retroativa (RN-66)%s",
             (agora - e.tMs) / 1000, obsSeq);
    avaliar(e, "ATRASADO", "ATRASADA", motivo, -1, nullptr);
    return;
  }

  // 4. Sensor de presença em falha não entra na correlação (RN-72)
  if (ehPresenca && degradada) {
    avaliar(e, "VALIDO", "SEM_ACIONAMENTO", "sensor de presenca em falha: nao entra na correlacao (RN-72)", -1, nullptr);
    return;
  }

  // 5. Guarda na janela e procura o complemento
  guardar(ehPresenca ? janelaPresencas : janelaMovimentos, e);
  Registro *outra = ehPresenca ? janelaMovimentos : janelaPresencas;
  Registro *par = e.tMs >= armadaDesdeMs ? complemento(outra, e.tMs, armadaDesdeMs) : nullptr;
  bool parAntesDoRearme = false;
  if (par == nullptr) {  // só existe par com detecção anterior ao rearme?
    par = complemento(outra, e.tMs, LONG_MIN);
    parAntesDoRearme = par != nullptr;
  }
  if (par == nullptr) {
    snprintf(motivo, sizeof(motivo), "%s isolada: aguarda %s por ate 60 s (RN-62)%s",
             ehPresenca ? "presenca" : "colmeia mexida", ehPresenca ? "colmeia mexida" : "presenca", obsSeq);
    avaliar(e, "VALIDO", "AGUARDANDO_COMPLEMENTO", motivo, -1, nullptr);
    return;
  }

  long dif = e.tMs - par->tMs; if (dif < 0) dif = -dif;
  float dtS = dif / 1000.0;

  // 6. Decisão (RN-31, RN-69, RN-70, RN-71)
  if (estado == DESARMADA) {
    snprintf(motivo, sizeof(motivo), "par encontrado, mas a seguranca esta desarmada (RN-70)%s", obsSeq);
    avaliar(e, "VALIDO", "SEM_ACIONAMENTO", motivo, dtS, nullptr);
  } else if (estado == MANUTENCAO) {
    snprintf(motivo, sizeof(motivo), "par encontrado, mas o modo visita esta ativo (RN-31, condicao 4)%s", obsSeq);
    avaliar(e, "VALIDO", "SEM_ACIONAMENTO", motivo, dtS, nullptr);
  } else if (estado == SINALIZANDO || estado == PROTECAO) {
    snprintf(motivo, sizeof(motivo), "mesma situacao: vinculada a %s, sem reiniciar a sirene (RN-69)%s",
             ocorrenciaAtual, obsSeq);
    avaliar(e, "VALIDO", "VINCULADA", motivo, dtS, ocorrenciaAtual);
  } else if (parAntesDoRearme) {
    snprintf(motivo, sizeof(motivo), "par inclui deteccao anterior ao armamento ou ao fim do modo visita: sem acionamento (rearme)%s", obsSeq);
    avaliar(e, "VALIDO", "SEM_ACIONAMENTO", motivo, dtS, nullptr);
  } else {
    acionar(e, par, dtS, agora);
  }
}

// ================================================================ rádio simulado (nó → central)
void enviarPeloRadio(const Evento &e) {
  if (perderProximo) {
    perderProximo = false;
    nota("radio simulado: mensagem seq %lu do no perdida", e.seq);
    return;
  }
  int copias = duplicarProximo ? 2 : 1;
  for (int c = 0; c < copias; c++) {
    for (int i = 0; i < 6; i++) {
      if (!filaRadio[i].ocupada) {
        filaRadio[i].ocupada = true;
        filaRadio[i].e = e;
        filaRadio[i].entregarEmMs = (long)millis() + atrasoProximoMs + c * 300;
        break;
      }
    }
  }
  if (atrasoProximoMs > 0) nota("radio simulado: mensagem seq %lu retida por %ld s", e.seq, atrasoProximoMs / 1000);
  if (duplicarProximo) nota("radio simulado: mensagem seq %lu sera entregue duas vezes", e.seq);
  atrasoProximoMs = 0;
  duplicarProximo = false;
}

void entregarRadio(long agora) {
  for (int i = 0; i < 6; i++) {
    if (filaRadio[i].ocupada && agora >= filaRadio[i].entregarEmMs) {
      filaRadio[i].ocupada = false;
      receberDeteccao(filaRadio[i].e);
    }
  }
}

// ================================================================ sensor de presença (PIR)
void amostrarPresenca(long agora) {
  bool alto = digitalRead(PINO_PIR) == HIGH;
  if (alto && !pirAnterior) {  // borda de subida: uma ativação
    pirAltoDesdeMs = agora;
    int n = 0;
    for (int i = 0; i < nAtivacoes; i++)
      if (agora - ativacoes[i] <= PAR07_JANELA_MS) ativacoes[n++] = ativacoes[i];
    nAtivacoes = n;
    if (nAtivacoes < 8) ativacoes[nAtivacoes++] = agora;
    nota("sensor de presenca: ativacao %d de %d em 10 s", nAtivacoes, PAR07_ATIVACOES);
    if (nAtivacoes >= PAR07_ATIVACOES) {
      Evento e;
      strcpy(e.tipo, "presenca.confirmada"); strcpy(e.dispositivo, ID_PRESENCA);
      strcpy(e.entidade, APIARIO); novoId(e.id);
      e.tMs = agora; e.seq = ++seqPresenca; e.valor = nAtivacoes;
      strcpy(e.unidade, "ativacoes"); horaLegivel(e.hora);
      nAtivacoes = 0;  // a próxima confirmação exige novas ativações
      emitirDeteccao(e, "PRESENCA_CONFIRMADA", "VALIDO", nullptr);
      receberDeteccao(e);
    }
  }
  if (!alto && pirAnterior) {
    pirAltoDesdeMs = -1;
    if (degradada) {
      degradada = false;
      publicarEstado("sensor de presenca voltou a variar: estado normal restabelecido (RN-72)");
    }
  }
  if (alto && !degradada && pirAltoDesdeMs >= 0 && agora - pirAltoDesdeMs > PAR19_TRAVADO_MS) {
    degradada = true;
    publicarEstado("sensor de presenca ativo sem parar por mais de 10 min: seguranca degradada (RN-72, PAR-19)");
  }
  pirAnterior = alto;
}

// ================================================================ nó da colmeia (MPU6050)
bool iniciarMpu() {
  uint8_t quem;
  if (!lerRegistros(END_MPU, 0x75, &quem, 1) || quem != 0x68) return false;  // WHO_AM_I
  escreverRegistro(END_MPU, 0x6B, 0x00);  // acorda o sensor
  escreverRegistro(END_MPU, 0x1C, 0x00);  // escala de ±2 g
  return true;
}

// Lê a aceleração e classifica a amostra: "" (válida), AUSENTE, FORA_DA_FAIXA ou IMPOSSIVEL
const char *lerColmeia(float &ax, float &ay, float &az) {
  uint8_t b[6];
  if (!mpuIniciado || !lerRegistros(END_MPU, 0x3B, b, 6)) { mpuIniciado = false; return "AUSENTE"; }
  int16_t r[3];
  for (int i = 0; i < 3; i++) r[i] = (int16_t)((b[2 * i] << 8) | b[2 * i + 1]);
  for (int i = 0; i < 3; i++) if (r[i] == 32767 || r[i] == -32768) return "FORA_DA_FAIXA";  // saturado
  ax = r[0] / 16384.0; ay = r[1] / 16384.0; az = r[2] / 16384.0;
  if (sqrtf(ax * ax + ay * ay + az * az) < PAR22_MODULO_MIN_G) return "IMPOSSIVEL";
  return "";
}

// Leitura inválida ou normalizada: publicada pelo nó e enviada à central pelo mesmo rádio,
// para que a sequência do nó chegue completa (sem falso "salto de sequência").
void emitirLeitura(const char *tipo, const char *estadoLeitura, const char *qualidade, const char *motivo) {
  Evento e;
  strcpy(e.tipo, tipo); strcpy(e.dispositivo, ID_NO); strcpy(e.entidade, COLMEIA);
  novoId(e.id);
  e.tMs = millis(); e.seq = ++seqNo; e.valor = NAN;
  strcpy(e.unidade, "g"); horaLegivel(e.hora);
  emitirDeteccao(e, estadoLeitura, qualidade, motivo);
  enviarPeloRadio(e);
}

void amostrarColmeia(long agora) {
  if (!mpuIniciado && agora - ultimaTentativaMpuMs >= 1000) {
    ultimaTentativaMpuMs = agora;
    mpuIniciado = iniciarMpu();
  }
  float ax = 0, ay = 0, az = 0;
  const char *problema = lerColmeia(ax, ay, az);

  // Validade com confirmação: 10 amostras seguidas (1 s) mudam o estado do sensor
  if (problema[0]) {
    validasSeguidas = 0;
    emCondicao = false; confirmadaNaCondicao = false;  // a condição não pode ser verificada
    if (++invalidasSeguidas >= PAR22_AMOSTRAS && !colmeiaEmFalha) {
      colmeiaEmFalha = true; motivoFalha = problema;
      char m[120];
      snprintf(m, sizeof(m), "leitura %s por 1 s: colmeia A1-07 fora da correlacao (RN-07)", problema);
      emitirLeitura("leitura.invalida", "SENSOR_EM_FALHA", "INVALIDO", m);
    }
    return;
  }
  invalidasSeguidas = 0;
  if (colmeiaEmFalha) {
    if (++validasSeguidas < PAR22_AMOSTRAS) return;
    colmeiaEmFalha = false;
    emitirLeitura("leitura.normalizada", "NORMAL", "VALIDO",
                  "leituras validas por 1 s; posicao de referencia mantida, sem recalibrar");
  }

  // Posição de referência: média das 10 primeiras amostras válidas
  if (!referenciaPronta) {
    somaX += ax; somaY += ay; somaZ += az;
    if (++amostrasReferencia == 10) {
      refX = somaX / 10; refY = somaY / 10; refZ = somaZ / 10;
      referenciaPronta = true;
      nota("colmeia A1-07: posicao de referencia (%.2f, %.2f, %.2f) g", refX, refY, refZ);
    }
    return;
  }

  // Inclinação em relação à referência e variação da aceleração (PAR-08)
  float modulo = sqrtf(ax * ax + ay * ay + az * az);
  float moduloRef = sqrtf(refX * refX + refY * refY + refZ * refZ);
  float cosseno = (ax * refX + ay * refY + az * refZ) / (modulo * moduloRef);
  if (cosseno > 1) cosseno = 1;
  if (cosseno < -1) cosseno = -1;
  float graus = acosf(cosseno) * 180.0 / PI;
  float dx = ax - refX, dy = ay - refY, dz = az - refZ;
  float variacaoG = sqrtf(dx * dx + dy * dy + dz * dz);
  bool condicao = graus >= PAR08_GRAUS || variacaoG >= PAR08_G;

  if (condicao) {
    if (!emCondicao) {
      emCondicao = true; confirmadaNaCondicao = false;
      inicioCondicaoMs = agora; maiorGraus = 0; maiorG = 0;
      nota("colmeia A1-07: %.1f graus / %.2f g, acima do limite; aguardando 2 s", graus, variacaoG);
    }
    if (graus > maiorGraus) maiorGraus = graus;
    if (variacaoG > maiorG) maiorG = variacaoG;
    if (!confirmadaNaCondicao && agora - inicioCondicaoMs >= PAR08_PERSISTENCIA_MS) {
      confirmadaNaCondicao = true;  // um evento por episódio
      Evento e;
      strcpy(e.tipo, "movimentacao.confirmada"); strcpy(e.dispositivo, ID_NO);
      strcpy(e.entidade, COLMEIA); novoId(e.id);
      e.tMs = agora; e.seq = ++seqNo;
      bool porGraus = maiorGraus >= PAR08_GRAUS;
      e.valor = porGraus ? roundf(maiorGraus * 10) / 10 : roundf(maiorG * 100) / 100;
      strcpy(e.unidade, porGraus ? "deg" : "g");
      horaLegivel(e.hora);
      emitirDeteccao(e, "MOVIMENTACAO_ANORMAL", "VALIDO", nullptr);
      enviarPeloRadio(e);
    }
  } else if (emCondicao) {
    if (!confirmadaNaCondicao)
      nota("colmeia A1-07: voltou abaixo do limite apos %ld ms: vibracao passageira, descartada",
           agora - inicioCondicaoMs);
    emCondicao = false; confirmadaNaCondicao = false;
  }
}

// ================================================================ controles
void aplicarChave(bool ligada, long agora) {
  protecaoLigada = ligada;
  if (ligada && estado == DESARMADA) {
    estado = ARMADA; armadaDesdeMs = agora;
    publicarEstado("seguranca armada pela chave; deteccoes anteriores nao formam par (rearme)");
  } else if (!ligada && (estado == ARMADA || estado == SINALIZANDO || estado == PROTECAO)) {
    bool estavaSinalizando = estado == SINALIZANDO;
    estado = DESARMADA;
    if (estavaSinalizando) encerrarSinalizacao(agora, "seguranca desarmada pela chave");
    publicarEstado("seguranca desarmada pela chave (RN-70)");
  } else if (estado == MANUTENCAO) {
    nota("modo visita ativo: a chave vale quando a visita terminar");
  }
}

void encerrarVisita(long agora, const char *como) {
  JsonDocument extra;
  extra["autorizacao"] = como;
  emitirCentral("autorizacao.estado", -1, nullptr,
                strcmp(como, "EXPIRADA") == 0 ? "modo visita expirou (60 min, PAR-13)" : "modo visita encerrado no botao",
                extra);
  estado = protecaoLigada ? ARMADA : DESARMADA;
  armadaDesdeMs = agora;
  publicarEstado(protecaoLigada ? "protecao restabelecida (RN-63); deteccoes da visita nao formam par (rearme)"
                                : "visita encerrada com a chave desligada");
}

void alternarVisita(long agora) {
  if (estado == MANUTENCAO) { encerrarVisita(agora, "ENCERRADA"); return; }
  if (estado == DESARMADA) { nota("seguranca desarmada: modo visita nao e necessario"); return; }
  bool estavaSinalizando = estado == SINALIZANDO;
  estado = MANUTENCAO;
  visitaAteMs = agora + PAR13_VISITA_MS;
  if (estavaSinalizando) encerrarSinalizacao(agora, "modo visita confirmado durante a sinalizacao (RN-71)");
  tone(PINO_BUZZER, 2000); bipeAteMs = agora + 150;  // confirmação local, distinta da luz (RN-71)
  JsonDocument extra;
  extra["autorizacao"] = "CONFIRMADA";
  extra["duracaoMin"] = PAR13_VISITA_MS / 60000;
  emitirCentral("autorizacao.estado", -1, nullptr, "modo visita confirmado na central", extra);
}

void lerControles(long agora) {
  int chave = digitalRead(PINO_CHAVE);
  if (chave != chaveBruta) { chaveBruta = chave; chaveMudouMs = agora; }
  if (agora - chaveMudouMs >= DEBOUNCE_MS && chaveBruta != chaveEstavel) {
    chaveEstavel = chaveBruta;
    aplicarChave(chaveEstavel == LOW, agora);
  }
  int botao = digitalRead(PINO_BOTAO_VISITA);
  if (botao != botaoBruto) { botaoBruto = botao; botaoMudouMs = agora; }
  if (agora - botaoMudouMs >= DEBOUNCE_MS && botaoBruto != botaoEstavel) {
    botaoEstavel = botaoBruto;
    if (botaoEstavel == LOW) alternarVisita(agora);
  }
}

// ================================================================ comandos de teste (monitor serial)
void imprimirEstado() {
  long agora = millis();
  Relogio r = lerRelogio();
  Serial.printf("# estado=%s protecaoLigada=%d degradada=%d colmeiaEmFalha=%d periodo=%s relogio=%s\n",
                estadoPublicado(), protecaoLigada, degradada, colmeiaEmFalha, periodoDoDia(),
                r.valido ? "valido" : "invalido");
  if (r.valido) Serial.printf("# hora=%02d:%02d:%02d\n", r.hora, r.min, r.seg);
  Serial.printf("# agora=%ld ms marcaDagua=%ld ms armadaDesde=%ld ms ocorrencia=%s\n",
                agora, marcaDagua(), armadaDesdeMs, ocorrenciaAtual[0] ? ocorrenciaAtual : "-");
  if (estado == SINALIZANDO) Serial.printf("# sinalizacao termina em %ld s\n", (sinalizandoAteMs - agora) / 1000);
  if (estado == PROTECAO) Serial.printf("# intervalo de protecao termina em %ld s\n", (protecaoAteMs - agora) / 1000);
  if (estado == MANUTENCAO) Serial.printf("# modo visita termina em %ld s\n", (visitaAteMs - agora) / 1000);
  for (int i = 0; i < 6; i++) {
    if (janelaPresencas[i].ocupado) Serial.printf("# janela: presenca t=%ld ms\n", janelaPresencas[i].tMs);
    if (janelaMovimentos[i].ocupado) Serial.printf("# janela: movimentacao t=%ld ms\n", janelaMovimentos[i].tMs);
  }
  Serial.printf("# sequencias: presenca=%lu no=%lu central=%lu\n", seqPresenca, seqNo, seqCentral);
}

void executarComando(char *c) {
  int h, m, s = 0, n;
  if (sscanf(c, "hora %d:%d:%d", &h, &m, &s) >= 2 && h >= 0 && h < 24 && m >= 0 && m < 60) {
    relogioFalhaSimulada = false;
    ajustarRelogio(h, m, s);
    nota("relogio ajustado para %02d:%02d:%02d; periodo %s", h, m, s, periodoDoDia());
  } else if (strcmp(c, "relogio falha") == 0) {
    relogioFalhaSimulada = true; nota("teste: horario local indisponivel");
  } else if (strcmp(c, "relogio ok") == 0) {
    relogioFalhaSimulada = false; nota("teste: horario local restabelecido");
  } else if (sscanf(c, "atraso %d", &n) == 1 && n > 0 && n <= 300) {
    atrasoProximoMs = n * 1000L; nota("teste: a proxima mensagem do no chegara %d s depois", n);
  } else if (strcmp(c, "dup") == 0) {
    duplicarProximo = true; nota("teste: a proxima mensagem do no sera entregue duas vezes");
  } else if (strcmp(c, "perda") == 0) {
    perderProximo = true; nota("teste: a proxima mensagem do no sera perdida");
  } else if (strcmp(c, "estado") == 0) {
    imprimirEstado();
  } else {
    nota("comandos: hora HH:MM | relogio falha | relogio ok | atraso S | dup | perda | estado");
  }
}

void lerComandos() {
  while (Serial.available() > 0) {
    char ch = (char)Serial.read();
    if (ch == '\n' || ch == '\r') {
      if (nComando > 0) { comando[nComando] = '\0'; executarComando(comando); nComando = 0; }
    } else if (nComando < (int)sizeof(comando) - 1) {
      comando[nComando++] = ch;
    }
  }
}

// ================================================================ ciclo da central
void atualizarCentral(long agora) {
  expirarJanela();
  if (estado == SINALIZANDO && agora >= sinalizandoAteMs) {
    estado = PROTECAO;
    protecaoAteMs = agora + PAR12_PROTECAO_MS;
    encerrarSinalizacao(agora, "duracao de 30 s concluida (RN-33, PAR-11)");
    publicarEstado("intervalo de protecao de 120 s iniciado (RN-34, PAR-12)");
  } else if (estado == PROTECAO && agora >= protecaoAteMs) {
    estado = ARMADA;
    publicarEstado("intervalo de protecao encerrado: novas ocorrencias voltam a acionar");
  } else if (estado == MANUTENCAO && agora >= visitaAteMs) {
    encerrarVisita(agora, "EXPIRADA");
  }
  if (agora - ultimaLeituraRelogioMs >= 1000) {  // relógio conferido a cada 1 s
    ultimaLeituraRelogioMs = agora;
    bool valido = lerRelogio().valido;
    if (!valido && relogioEstavaValido) {
      JsonDocument extra;
      emitirCentral("relogio.invalido", -1, nullptr, "sem horario local valido: periodo noturno adotado (RN-64)", extra);
    } else if (valido && !relogioEstavaValido) {
      nota("relogio valido de novo; periodo %s", periodoDoDia());
    }
    relogioEstavaValido = valido;
  }
}

void atualizarSaidas(long agora) {
  if (somLigado && agora - ultimaTrocaTomMs >= 400) {  // sirene de dois tons
    tomAlto = !tomAlto; ultimaTrocaTomMs = agora;
    tone(PINO_BUZZER, tomAlto ? 1320 : 880);
  }
  if (bipeAteMs > 0 && agora >= bipeAteMs) {
    bipeAteMs = 0;
    if (!somLigado) noTone(PINO_BUZZER);
  }
  digitalWrite(PINO_LED_VISITA, estado == MANUTENCAO ? HIGH : LOW);
  digitalWrite(PINO_LED_ATENCAO, strcmp(estadoPublicado(), "ATENCAO") == 0 ? HIGH : LOW);
  digitalWrite(PINO_LED_FALHA, (degradada || colmeiaEmFalha) ? HIGH : LOW);
}

void setup() {
  Serial.begin(115200);
  pinMode(PINO_PIR, INPUT);
  pinMode(PINO_CHAVE, INPUT_PULLUP);
  pinMode(PINO_BOTAO_VISITA, INPUT_PULLUP);
  pinMode(PINO_BUZZER, OUTPUT);
  pinMode(PINO_RELE_LUZ, OUTPUT);
  pinMode(PINO_LED_VISITA, OUTPUT);
  pinMode(PINO_LED_ATENCAO, OUTPUT);
  pinMode(PINO_LED_FALHA, OUTPUT);
  desligarSinalizacao();  // estado seguro: sirene e luz desligadas
  Wire.begin(PINO_SDA, PINO_SCL);
  for (int i = 0; i < 16; i++) vistos[i][0] = '\0';

  nota("Mellona - central do apiario A1 - Atividade 03 (Decisao e atuacao)");
  nota("parametros: presenca 2 ativacoes/10 s; colmeia 5 graus ou 0,3 g por 2 s; janela 60 s; "
       "tolerancia 5 s; sirene 30 s; protecao 120 s");
  mpuIniciado = iniciarMpu();
  ultimaTentativaMpuMs = millis();
  if (!mpuIniciado) nota("MPU6050 nao respondeu no endereco 0x69");
  Relogio r = lerRelogio();
  if (r.valido) nota("relogio: %02d:%02d:%02d, periodo %s", r.hora, r.min, r.seg, periodoDoDia());
  nota("digite 'ajuda' para os comandos de teste");

  chaveEstavel = chaveBruta = digitalRead(PINO_CHAVE);
  botaoEstavel = botaoBruto = digitalRead(PINO_BOTAO_VISITA);
  protecaoLigada = chaveEstavel == LOW;
  estado = protecaoLigada ? ARMADA : DESARMADA;
  armadaDesdeMs = millis();
  publicarEstado(protecaoLigada ? "inicio: chave em protecao ligada" : "inicio: chave em protecao desligada");
}

void loop() {
  long agora = millis();
  lerComandos();
  lerControles(agora);
  amostrarPresenca(agora);
  if (agora - ultimaAmostraMpuMs >= AMOSTRA_MPU_MS) {
    ultimaAmostraMpuMs = agora;
    amostrarColmeia(agora);
  }
  entregarRadio(agora);
  atualizarCentral(agora);
  atualizarSaidas(agora);
  delay(10);
}
