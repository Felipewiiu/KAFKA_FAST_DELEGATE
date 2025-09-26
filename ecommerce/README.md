# 🚀 Guia rápido para rodar Apache Kafka no Windows

Este guia mostra como iniciar um broker Kafka localmente, criar um tópico e testar com producer/consumer no **Windows**
usando os scripts `.bat`.

---

## 📦 Pré-requisitos

- [Java 11+](https://adoptium.net/) instalado e configurado no `PATH`
- [Apache Kafka](https://kafka.apache.org/downloads) baixado e extraído
- Terminal: **Git Bash** ou **PowerShell**
- https://developer.confluent.io/get-started/java/#create-project

---

### Cria variável

- Depois de gerar o UUID, acesse a variável para pegar o ID que esta no meio de um monte de lixo.

````shell
     $KAFKA_CLUSTER_ID= C:/KAFICA/kafka_2.13-4.1.0/bin/windows/kafka-storage.bat random-uuid
````

### Formata o diretório de logs

Antes de inicializar o Kafka é preciso criar um diretório de logs. O destino dos logs fica apontado dentro do arquivo
`server.properties`

````shell
  C:/KAFICA/kafka_2.13-4.1.0/bin/windows/kafka-storage.bat format --standalone -t $KAFKA_CLUSTER_ID -c C:/KAFICA/kafka_2.13-4.1.0/config/server2.properties
````

### Comando para subir o broker

````shell
  C:/KAFICA/kafka_2.13-4.1.0/bin/windows/kafka-server-start.bat  C:/KAFICA/kafka_2.13-4.1.0/config/server.properties
````

### Comando para criar um topic

````shell
 C:/KAFICA/kafka_2.13-4.1.0/bin/windows/kafka-topics.bat --create --bootstrap-server localhost:9092 --replication-factor 2 --partitions 3 --topic ECOMMERCE_NEW_ORDER
````

### Comando para listar um topic

````shell
  C:/KAFICA/kafka_2.13-4.1.0/bin/windows/kafka-topics.bat --list --bootstrap-server localhost:9092
````

### Comando para descrever os topic

````shell
  C:/KAFICA/kafka_2.13-4.1.0/bin/windows/kafka-topics.bat --describe --bootstrap-server localhost:9092
````

### Criando um producer no console

````shell
  bin/windows/kafka-console-producer.bat --bootstrap-server localhost:9092 --topic LOJA_NOVO_PEDIDO 
````

### Consumindo uma mensagem e especificando como

````shell
  bin/windows/kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic LOJA_NOVO_PEDIDO --from-beginning
````

## Lista grupo de consumo

````shell
  C:/KAFICA/kafka_2.13-4.1.0/bin/windows/kafka-consumer-groups.bat --all-groups --bootstrap-server localhost:9092 --describe
````

## Configurando Vários consumidores e produtores

- Defina um grupo no contexto do Kafka

### Comando para alterar uma partição:

- Não é possível diminuir uma partição já criada
- A chave quem direciona em qual a partição a mensagem vai cair;
- Configurar o pull para 1 evita rebalanceamento por erro de commit

````shell
  .\bin\windows\kafka-topics.bat --alter --bootstrap-server localhost:9092 --topic ECOMMERCE_NEW_ORDER --partitions 3 

````

### Diretórios Kafka

- Para guardar os logs de maneira permanente é preciso trocar o log da pasta temp do arquivo ``server.properties``

````text
############################# Log Basics #############################

# A comma separated list of directories under which to store log files
log.dirs=C:/KAFICA/kafka_2.13-4.1.0/data/kafka
````

### Replicando os brokers

Para podermos criar uma réplica de um broker é preciso duplicar o arquivo de ``server.properties`` e muda algumas
configurações como:

````properties
  node.id=1
  default.replication.factor=2
  controller.quorum.voters=1@localhost:9093,2@localhost:9095
  log.dirs=C:/KAFICA/kafka_2.13-4.1.0/data/kafka2 // criar um para cada broker
````

**OBS:** O ``controller.quorum.voters`` 

# Função das Partições no Apache Kafka

As **partições** são a unidade básica de paralelismo e armazenamento num tópico no Kafka.  
Elas permitem distribuir, escalar e garantir a ordenação das mensagens.

---

## 1. Escalabilidade

Cada partição pode ser distribuída em diferentes **brokers** (máquinas) no cluster.  
Assim, um mesmo tópico pode ser processado em paralelo por várias máquinas.

**Exemplo:**

- 1 tópico com **10 partições** → pode ter até **10 consumers** (no mesmo consumer group) lendo em paralelo.

---

## 2. Paralelismo no Consumo

Cada **consumer** dentro de um **consumer group** lê **exclusivamente** de uma ou mais partições.

Quanto mais partições um tópico tiver, maior será a capacidade de **processamento paralelo**.

**Exemplo:**

- 1 tópico com **1 partição** → apenas **1 consumer** por grupo consegue processar ao mesmo tempo.
- 1 tópico com **5 partições** → até **5 consumers** do mesmo grupo podem processar mensagens em paralelo.

---

## 3. Ordenação Garantida dentro da Partição

Dentro de uma partição, as mensagens têm uma ordem **imutável**:  
`offset 0, 1, 2, 3...`

Essa ordenação é garantida **somente dentro da partição**, não no tópico inteiro.

➡️ Se você precisa que mensagens relacionadas cheguem **em ordem**, elas devem ser enviadas para a **mesma partição** (
usando chave de partição).

---

## 4. Distribuição de Dados

O Kafka decide em qual partição salvar cada mensagem. Isso pode ser feito de três formas:

1. **Round-robin (sem chave):** mensagens são distribuídas de forma balanceada.
2. **Com chave:** mensagens com a mesma chave (ex.: ID do usuário) vão sempre para a mesma partição, garantindo
   ordenação por chave.
3. **Custom partitioner:** você pode implementar sua própria lógica de particionamento.

---

## 🔑 Resumindo

- **Mais partições** = mais paralelismo, maior throughput.
- **Menos partições** = menos overhead, mas menos paralelismo.
- **Ordem garantida apenas dentro da partição.**

# Acks e Reliability no Kafka

No **Kafka**, os termos **acks** e **reliability** estão relacionados à **garantia de entrega** das mensagens entre **produtores** e **brokers**.

---

## 🔹 O que é **acks**
`acks` (acknowledgments) é uma configuração do **Producer** que define quantos brokers precisam confirmar o recebimento de uma mensagem antes do produtor considerá-la entregue com sucesso.

### Valores possíveis:
- **acks=0**
   - O produtor **não espera confirmação**.
   - A mensagem é enviada e considerada entregue assim que sai do produtor.
   - **Baixa latência, mas risco de perda** se o broker cair antes de gravar a mensagem.

- **acks=1** (default)
   - O broker **líder da partição** confirma o recebimento.
   - Se o líder cair antes de replicar para os seguidores, pode haver **perda de mensagem**.
   - **Equilíbrio** entre performance e segurança.

- **acks=all** (ou `acks=-1`)
   - O produtor só recebe confirmação quando **todos os ISR (in-sync replicas)** confirmarem.
   - Garante **alta durabilidade** (mensagem só é considerada entregue após estar replicada).
   - **Maior latência**, mas mais confiável.

---

## 🔹 O que é **reliability**
**Reliability** (confiabilidade) no Kafka significa a capacidade do sistema de **garantir que mensagens não sejam perdidas ou duplicadas**.  
Ela depende de algumas configurações e boas práticas:

1. **acks** (nível de confirmação do produtor)  
   → quanto mais forte (ex: `all`), mais confiável.

2. **min.insync.replicas** (no broker)  
   → número mínimo de réplicas que precisam confirmar para considerar uma mensagem válida.

3. **retries** e **enable.idempotence** (no produtor)
   - `retries` → permite reenviar mensagens em caso de falha temporária.
   - `enable.idempotence=true` → evita mensagens duplicadas quando há retries.

4. **Replication factor** (no tópico)  
   → mais réplicas aumentam a tolerância a falhas.

---

## ✅ Resumindo
- **acks** → controle de **quando** o produtor considera a mensagem entregue.
- **reliability** → conjunto de estratégias/configurações que garantem que mensagens não se percam nem sejam duplicadas, mesmo com falhas.  


## EXemplo de configuração

````properties
# Licenciado para a Apache Software Foundation (ASF) sob um ou mais
# acordos de licença de colaborador. Veja o arquivo NOTICE distribuído com
# este trabalho para informações adicionais sobre direitos autorais.
# A ASF licencia este arquivo para você sob a Licença Apache, Versão 2.0
# (a "Licença"); você não pode usar este arquivo exceto em conformidade com
# a Licença. Você pode obter uma cópia da Licença em
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# A menos que exigido por lei aplicável ou acordado por escrito, o software
# distribuído sob a Licença é distribuído "COMO ESTÁ", SEM GARANTIAS OU
# CONDIÇÕES DE QUALQUER TIPO, expressas ou implícitas. Veja a Licença para
# o idioma específico que rege as permissões e limitações sob a Licença.

############################# Configurações Básicas do Servidor #############################

# A função (role) deste servidor. Definir isso coloca o servidor no modo KRaft
process.roles=broker,controller

# O ID do nó associado às funções desta instância
node.id=1
default.replication.factor=3

#Endereço dos controladores
controller.quorum.voters=1@localhost:9093,2@localhost:9095,3@localhost:9097,4@localhost:9099

# Lista de endpoints do controller usados para conectar ao cluster do controller
controller.quorum.bootstrap.servers=localhost:9093,localhost:9095

############################# Configurações do Socket Server #############################

# O endereço no qual o socket server escuta.
# Nós combinados (ou seja, aqueles com `process.roles=broker,controller`) devem listar o listener do controller aqui, no mínimo.
# Se o listener do broker não estiver definido, o listener padrão usará um nome de host igual ao valor de java.net.InetAddress.getCanonicalHostName(),
# com o nome de listener PLAINTEXT e a porta 9092.
#   FORMATO:
#     listeners = nome_do_listener://nome_do_host:porta
#   EXEMPLO:
#     listeners = PLAINTEXT://seu.nome.de.host:9092
listeners=PLAINTEXT://:9092,CONTROLLER://:9093

# Nome do listener usado para comunicação entre brokers.
inter.broker.listener.name=PLAINTEXT

# Nome do listener, hostname e porta que o broker ou o controller anunciará aos clientes.
# Se não for definido, usará o valor de "listeners".
advertised.listeners=PLAINTEXT://localhost:9092,CONTROLLER://localhost:9093

# Uma lista separada por vírgulas dos nomes dos listeners usados pelo controller.
# Se nenhum mapeamento explícito for definido em `listener.security.protocol.map`, o padrão será usar o protocolo PLAINTEXT
# Isso é necessário se estiver executando no modo KRaft.
controller.listener.names=CONTROLLER

# Mapeia nomes de listeners para protocolos de segurança, o padrão é que sejam os mesmos. Veja a documentação de configuração para mais detalhes
listener.security.protocol.map=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,SSL:SSL,SASL_PLAINTEXT:SASL_PLAINTEXT,SASL_SSL:SASL_SSL

# O número de threads que o servidor usa para receber solicitações da rede e enviar respostas para a rede
num.network.threads=3

# O número de threads que o servidor usa para processar solicitações, o que pode incluir E/S de disco
num.io.threads=8

# O buffer de envio (SO_SNDBUF) usado pelo socket server
socket.send.buffer.bytes=102400

# O buffer de recepção (SO_RCVBUF) usado pelo socket server
socket.receive.buffer.bytes=102400

# O tamanho máximo de uma solicitação que o socket server aceitará (proteção contra OOM - falta de memória)
socket.request.max.bytes=104857600


############################# Configurações Básicas de Log #############################

# Uma lista separada por vírgulas de diretórios onde os arquivos de log serão armazenados
log.dirs=C:/KAFICA/kafka_2.13-4.1.0/data/kafka

# O número padrão de partições de log por tópico. Mais partições permitem maior
# paralelismo para consumo, mas também resultarão em mais arquivos entre os brokers.
num.partitions=3

# O número de threads por diretório de dados a serem usadas para recuperação de log na inicialização e flushing no desligamento.
# Recomenda-se aumentar este valor para instalações com diretórios de dados localizados em array RAID.
num.recovery.threads.per.data.dir=1

############################# Configurações de Tópicos Internos  #############################
# O fator de replicação para os tópicos internos de metadados de grupo "__consumer_offsets", "__share_group_state" e "__transaction_state"
# Para qualquer coisa além de testes de desenvolvimento, recomenda-se um valor maior que 1 para garantir disponibilidade, como 3.
offsets.topic.replication.factor=2
share.coordinator.state.topic.replication.factor=2
share.coordinator.state.topic.min.isr=1
transaction.state.log.replication.factor=2
transaction.state.log.min.isr=1

############################# Política de Flush de Log #############################

# As mensagens são imediatamente escritas no sistema de arquivos, mas por padrão só fazemos fsync() para sincronizar
# o cache do SO de forma lazy. As configurações a seguir controlam a descarga (flush) de dados para o disco.
# Existem algumas compensações importantes aqui:
#    1. Durabilidade: Dados não descarregados podem ser perdidos se você não estiver usando replicação.
#    2. Latência: Intervalos de flush muito grandes podem levar a picos de latência quando o flush ocorrer, pois haverá muitos dados para descarregar.
#    3. Taxa de transferência (Throughput): O flush é geralmente a operação mais custosa, e um intervalo pequeno de flush pode levar a buscas excessivas.
# As configurações abaixo permitem configurar a política de flush para descarregar dados após um período de tempo ou
# a cada N mensagens (ou ambos). Isso pode ser feito globalmente e substituído por tópico.

# O número de mensagens a aceitar antes de forçar um flush de dados para o disco
#log.flush.interval.messages=10000

# O tempo máximo que uma mensagem pode permanecer em um log antes de forçarmos um flush
#log.flush.interval.ms=1000

############################# Política de Retenção de Log #############################

# As configurações a seguir controlam a disposição de segmentos de log. A política pode
# ser definida para excluir segmentos após um período de tempo ou após um determinado tamanho ter se acumulado.
# Um segmento será excluído sempre que *qualquer* um desses critérios for atendido. A exclusão sempre acontece
# a partir do final do log.

# A idade mínima de um arquivo de log para ser elegível para exclusão devido à idade
log.retention.hours=168

# Uma política de retenção baseada em tamanho para logs. Os segmentos são removidos do log a menos que os segmentos restantes
# caiam abaixo de log.retention.bytes. Funciona independentemente de log.retention.hours.
#log.retention.bytes=1073741824

# O tamanho máximo de um arquivo de segmento de log. Quando esse tamanho é atingido, um novo segmento de log será criado.
log.segment.bytes=1073741824

# O intervalo no qual os segmentos de log são verificados para ver se podem ser excluídos de acordo
# com as políticas de retenção
log.retention.check.interval.ms=300000
````



