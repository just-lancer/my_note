<h1 align = "center">Kafka
</h1>
# 一、概述

## 1.1、引言

`Kafka`是一个分布式的基于发布/订阅模式的消息队列，主要应用于大数据实时处理领域。

发布/订阅模式：消息的发布者不会将消息直接发送给特定的订阅者，而是将待发布的消息分为不同的类别，订阅者只订阅感兴趣的消息。

`Kafka`新定义：`Kafka`是一个开源的分布式事件流平台，被用于高性能数据管道、流分析、数据集成和关键任务应用。

目前较为常见的消息队列有`Kafka`、`RabbitMQ`、`RocketMQ`，在大数据场景中，主要使用`Kafka`作为消息队列，在`Java`后端开发中，主要采用`RabbitMQ`和`RocketMQ`。

消息队列作为消息中间件，将具体业务和底层逻辑解耦，例如，需要利用服务的人（前端），不需要知道底层逻辑（后端）的实现，只需要拿着中间件的结果使用就可以了，因此，消息队列的主要应用场景有三个：消峰、解耦和异步。

但消息队列的使用也会带来一定的问题：使用消息队列后，会增加系统的复杂性和系统维护问题，以及系统使用过程中会出现的各种问题，主要问题是消息重复消费、消息丢失、消息顺序错乱等。

-   **消峰：**将高峰期的请求放入消息队列中，由消费者自行进行消息订阅或消费，能够解决客户端请求速度与服务端处理请求速度不匹配而引发的服务崩溃问题![Kafka-消峰](./05-Kafka.assets/Kafka-消峰.png)

-   **解耦：**消息的生产与消息的消费不直接对接，避免了消息生产者生产消息速度与消息消费者消息消费速度不匹配导致的数据积压问题

![kafka-解耦](./05-Kafka.assets/kafka-解耦.png)

-   **异步：**消息发送者发出消息后，立即继续执行下一步，不等待消息接收者的返回消息或控制![Kafka-异步](./05-Kafka.assets/Kafka-异步-16841576534508.png)

## 1.2、Kafka相关概念

-   **消息`Message`：**消息是`KafKa`数据传输的基本单位，由一个固定长度的消息头和一个可变长度的消息体构成。在旧版本中，每一条消息称为`Message`；在由`Java`重新实现的客户端中，每一条消息称为`Record`
-   **`broker`：**`KafKa`集群就是由一个或者多个`KafKa`节点（或称为实例）构成，每一个`KafKa`节点称为`broker`。一个节点就是一台服务器上的`Kafka`服务。`broker`由`Scala`语言实现
-   **生产者`producer`：**消息的生产者，即向`broker`推送数据的客户端，一般是`Java`代码实现的
-   **消费者`consumer`**：消息的消费者，即从`broker`中拉取数据的客户端，一般也是`Java`代码实现
    -   每个消费者都有一个全局唯一的`id`，通过配置项`client.id`指定。如果代码中没有指定消费者的`id`，`KafKa`会自动为该消费者生成一个全局唯一的`id`

-   **消费者组`consumer group`：**消费者组由多个消费者构成。消费组是`KafKa`实现对一个`topic`消息消费的手段，即消费者组是逻辑上的一个订阅者。单独的一个消费者也是`topic`的订阅者，因为`Kafka`会为单独的一个消费者设置消费者组，即这个消费者组只有这一个消费者
    -   在`KafKa`中，每个消费者都属于一个特定的消费者组，默认情况下，消费者属于默认的消费组`test-consumer-group`，通过`group.id`配置项，可以为每个消费者指定消费组
    -   在消费者组内，不同的消费者不能消费同一个`topic`相同分区的消息，换句话说，一个分区的消息只能由一个消费者消费，原因是进行并行消费，提高效率
-   **主题`topic`：**
    -   `topic`是指一类消息的集合，在`Kafka`中用于分类管理消息的逻辑单元，类似于数据库管理系统中的库和表，只是逻辑上的概念
    -   `topic`作为`Kafka`中的核心概念，将生产者和消费者解耦。生产者向指定`topic`中发布消息，消费者订阅指定`topic`的消息
    -   `topic`可以被分区用以提高并发处理能力和可扩展性
-   **分区`partition`：**`topic`是许多消息的集合
    -   当消息特别多（即`topic`特别大），为了方便消息的管理，`Kafka`将`topic`进行分区管理
    -   一个`topic`能够被分为许多个分区，每个分区由一系列有序、不可变的消息组成，是一个有序队列
    -   每个`topic`的分区数可以在`Kafka`启动时加载的配置文件中配置，也可以在创建`topic`的时候进行设置，还可以在代码中进行设置
    -   每个分区又有一至多个副本，分区的副本分布在`Kafka`集群的不同`broker`上，以提高数据的可靠性
-   **`leader`：**每个分区所有副本中的”老大“，用于对接生产者，接收生产者发送的消息；也用于对接消费者，接受消费者发送的消费消息的请求
-   **`follower`：**每个分区所有副本中不是`leader`的副本，实时从`leader`中同步数据，当`leader`发生故障时，`Kafka`会从`follower`中选举出新的`leader`

## 1.3、Kafka基本架构

整体来看，`Kafka`框架包含四大组件：生产者`Producer`、消费者`Consumer`、`broker`集群和`zookeeper`集群。

在一台服务器上只能部署一个`Kafka`节点，每个`Kafka`节点都称为`broker`，多个`Kafka`节点构成`broker`集群。

一个完整的`Kafka`集群包含多个生产者，多个`broker`，多个消费者以及一个`zookeeper`集群。

-   **`zookeeper`：**`Kafka`通过`zookeeper`协调管理`Kafka`集群，主要用于存储`Kafka`集群的`broker`信息，记录`Kafka`集群由哪些`broker`构成；存储`topic`的分区副本`leader`信息以及分区副本`follower`的信息；此外还用于分区副本`leader`选举过程中的信息协调，以及消费者组中消费者挂掉时，其承担的分区数据消费任务的再分配。在`Kafka 2.8.0`版本之后，搭建`Kafka`集群可以不再依赖`zookeeper`集群。

-   **生产者：**是`Kafka`中的消息生产者，主要用于将外部需要写入`Kafka`的数据进行一定的处理，然后发送给`Kafka`的`broker`进行存储。这些数据在经过处理了后会带有`topic`信息，分区信息，用以确定数据将被法网哪个`topic`的哪个分区中
-   **消费者：**一般所说的消费者，是指消费者组，它是`Kafka`消息消费的逻辑单位。每个消费者都将属于一个消费者组；在同一个消费者组内，不同的消费者只能消费一个`topic`的不同分区
-   **`broker`：**`Kafka`集群中用于存储数据的组件。在`broker`中，用`topic`来对存入`Kafka`的数据进行分类；为了避免由于单一`topic`数据量过大，导致单`broker`节点存储的数据量过大，数据存取效率低下的问题，`Kafka`中可以对每个`topic`的数据进行切分，切出来的每一块数据称为一个分区`Partition`。此外，由于`Kafka`是一个分布式的消息队列，为了提高数据的可靠性，`Kafka`还可以对每个分区设置副本数量，但是，与`Hadoop`等大数据组件不同的是，`Kafka`中，分区副本之间是有所差别的，所有副本中有一个副本将会根据特定的选举机制称为分区副本的`leader`，用于对接生产者和消费者，而其他不是`leader`的副本称为`follower`，在`Kafka`正常运行时，从`leader`中同步数据，时刻与`leader`保持一致，当`leader`挂掉时，再通过选举机制选出一个`follower`成为`leader`

![kafka-基本架构](./05-Kafka.assets/kafka-基本架构.png)

# 二、Kafka快速入门

## 2.1、Kafka集群安装部署

参考：**大数据组件部署文档.md**

## 2.2、Kafka常用命令

**说明：**

-   **以下命令的使用均站在`./kafka`目录下，因此都将使用相对路径**
-   **`<>`表示必选项，`[]`表示可选项**

**==`Kafka`集群启停：==`Kafka`集群启动之前需要先启动`zookeeper`集群，因此停止`Kafka`集群时，需要先停止`Kafka`集群，再停止`zookeeper`集群。**

-   **`Kafka`集群启动命令：**`Kafka`集群启动需要指定集群配置文件

    ```bash
    ./bin/kafka-server-start.sh -daemon ./config/server.properties
    ```

-   **`Kafka`集群停止命令：**

    ```bash
    ./bin/kafka-server-stop.sh
    ```

**==`topic`相关命令：==**

-   **查看操作`topic`相关命令的参数：**

    ```bash
    ./bin/kafka-topics.sh
    ```

    -   **`--bootstrap-server <String: server toconnect to>`：**配置连接`Kafka`集群的主机名和端口号，该配置项必不可少

    -   **`--list`：**列举现有所有`topic`

    -   **`--topic <String: topic>`：**指定想要操作的`topic`的名称

    -   **`--create`：**表明此次对指定`topic`的操作是**创建**该`topic`

    -   **`--delete`：**表明此次对指定`topic`的操作是**删除**该`topic`

    -   **`--alter`：**表明此次对指定`topic`的操作是**修改**该`topic`的配置，例如，分区数、副本数等，因此，该参数还需要其他参数配合使用

    -   **`--describe`：**查看`topic`详细描述，分区数、副本数等

    -   **`--patitions<Integer: of partitions>`：**设置`topic`的分区数

    -   **`--replication-factor<Integer: replication factor>`：**设置分区副本数

    -   **`--config<String: name = value>`：**更新系统默认配置

        -   **查看当前`Kafka`集群中所有`topic`：**

            ```bash
            kafka-topics.sh --bootstrap-server hadoop132:9092 --list
            
            # out
            __consumer_offsets
            first
            sink_topic
            ```

        -   **创建名为`second`的`topic`：**创建`topic`时，需要指定`topic`的分区数和副本数

            ```bash
            kafka-topics.sh --bootstrap-server hadoop132:9092 --create --partitions 3 --replication-factor 3 --topic second
            ```

        -   **查看`second topic`的详情：**

            ```bash
            kafka-topics.sh --bootstrap-server hadoop132:9092 --describe --topic second
            
            # out
            Topic: second	TopicId: uSXoYxJ8SBSIbeGOUWnEWQ	PartitionCount: 3	ReplicationFactor: 3	Configs: segment.bytes=1073741824
            	Topic: second	Partition: 0	Leader: 3	Replicas: 3,2,4	Isr: 3,2,4
            	Topic: second	Partition: 1	Leader: 4	Replicas: 4,3,2	Isr: 4,3,2
            	Topic: second	Partition: 2	Leader: 2	Replicas: 2,4,3	Isr: 2,4,3
            ```

        -   **修改`second topic`的分区数：**分区数只能增加，不能减少

            ```bash
            kafka-topics.sh --bootstrap-server hadoop132:9092 --alter --partitions 4 --topic second
            ```

            **==分区副本数量的调整需要通过几个步骤，后续会进行说明==**

        -   **再次查看`second topic`的详情：**

            ```bash
            kafka-topics.sh --bootstrap-server hadoop132:9092 --describe --topic second
            
            # out
            Topic: second	TopicId: uSXoYxJ8SBSIbeGOUWnEWQ	PartitionCount: 4	ReplicationFactor: 3	Configs: segment.bytes=1073741824
            	Topic: second	Partition: 0	Leader: 3	Replicas: 3,2,4	Isr: 3,2,4
            	Topic: second	Partition: 1	Leader: 4	Replicas: 4,3,2	Isr: 4,3,2
            	Topic: second	Partition: 2	Leader: 2	Replicas: 2,4,3	Isr: 2,4,3
            	Topic: second	Partition: 3	Leader: 3	Replicas: 3,4,2	Isr: 3,4,2
            ```

        -   **删除`second topic`**

            ```bash
            kafka-topics.sh --bootstrap-server hadoop132:9092 --delete --topic second
            ```

**==生产者相关命令：==生产者的命令行操作只能模拟出一个生产者客户端用于发送消息，因此，相关参数只有配置`Kafka`集群地址和端口号，以及指定需要操作的`topic`。生产者相关命令需要使用`kafka-console-producer.sh`脚本**

-   **创建一个向`first topic`写入数据的生产者：**

    ```bash
    kafka-console-producer.sh --bootstrap-server hadoop132:9092 --topic first
    ```

**==消费者相关命令：==消费者的命令行操作只能模拟出一个消费者客户端（即消费者组）用于消费指定`topic`的消息，因此，相关参数也有配置`Kafka`集群地址和端口号，指定需要操作的`topic`，以及指定消费者组名称。**

-   **消费者相关命令需要使用`kafka-console-consumer.sh`脚本**

    -   **`--bootstrap-server <String: server toconnect to>`：**配置连接`Kafka`集群的主机名和端口号

    -   **`--topic <String: topic>`：**指定想要操作的`topic`的名称

    -   **`--group<String:consumer group id>`：**指定消费者组名称

    -   **`--from-beginning`：**从头开始消费数据

        -   **创建一个消费者（组），消费`first topic`的数据**

            ```bash
            kafka-console-consumer.sh --bootstrap-server hadoop132:9092 --topic first
            ```

# 三、Kafka框架原理

## 3.1、生产者消息发送流程

生产者客户端由`Java`代码实现，在生产者客户端中，消息的发布依赖两个线程的协调运行，这两个线程分别是`main`线程和`sender`线程。

在`Kafka`生产者的逻辑中，`main`线程只关注向哪个分区中发送哪些消息；而`sender`线程只关注与哪个具体的`broker`节点建立连接，并将消息发送到所连接的`broker`中。

-   **主线程：**主线程中接受的外部系统数据，会分别经过拦截器、序列化器和分区器的加工，形成带有`topic`以及分区信息的消息，随后这些消息将被缓存到消息累加器中，准备发送到`broker`中
    -   **拦截器`interceptor`：**生产者拦截器可以在消息发送之前对消息进行定制化操作，如过滤不符合要求数据，修改消息内容，数据统计等
    
    -   **序列化器`serializer`：**数据进行网络传输和硬盘读写都需要进行序列化和反序列化
    
    -   **分区器`partitioner`：**对消息进行分区，便于发送到不同的分区中存储
    -   **消息累加器`RecoderAccumulator`：**①：用于缓存经`main`线程处理好的消息；②：`sender`线程会拉取其中的数据进行批量发送，进而提高效率
        -   `RecoderAccumulator`的缓存大小默认为`32M`
        -   `RecoderAccumulator`内部为每个分区都维护了一个双端队列，即`Deque<ProduceBatch>`，消息写入缓存时，追加到队列的尾部
        -   每个双端队列中以批`ProducerBatch`的形式存储消息，默认情况下，`ProducerBatch`的大小为`16K`
    -   **`ProducerBatch`：**一个消息批次，由多条消息合并而成，默认大小为`16K`，`sender`从`RecoderAccumulator`中读取消息时，以`ProducerBatch`为单位进行读取，进而减少网络请求次数
-   **`sender`线程：**`sender`线程从`RecoderAccumulator`中拉取到`RecoderBatch`后，会将`<partition, Deque<Producer Batch>>`形式的消息转换成`<Node,List< ProducerBatch>`形式的消息，即将消息的分区信息转换成对应的`broker`节点，随后，进一步封装成`<Node, Request>`的形式，这样形式的消息具备网络传输的条件。其中`Request`是`Kafka`的协议请求。
    -   在`sender`线程中有一个用于缓存已经发出去但还没有收到服务端响应的请求的容器`InFlightRequests`。消息具备网络传输条件后，会被保存在`InFlightRequests`中，保存对象的具体形式为`Map<NodeId，Deque<Request>>`，其默认容量为`5`。


**消息发送：**

数据经过`main`线程和`sender`线程的处理后，就具备了进行网络传输的条件，`Kafka`的`broker`在接收到消息后会对`sender`线程进行应答，即`ack`应答

**`ack(acknowledgment)`：生产者消息发送确认机制。`ack`有三个可选值`0，1，-1(all)`**

-   **`ack = 0`：**生产者发送消息后，不需要等待消息在`broker`节点写入磁盘。该应答级别安全性低，但效率高
-   **`ack = 1`：**生产者发送消息后，只需要分区副本中，`leader`分区接收，并写入磁盘，`broke`r便可以向生产者进行应答
-   **`ack = -1(all)`：**生产者发送消息后，需要`ISR`列表中，所有副本都把接收到的消息写入到磁盘后，`broker`才会向生产者进行应答。该应答级别安全性高，效率低

**`ISR(In-sync replicas)`：**同步副本。在最长滞后时间内（也就是一定时间内），能完成`leader`数据同步的副本称为同步副本。超过最长滞后时间，副本还未完成数据同步会被踢出`ISR`列表，加入`OSR`列表，当`OSR`列表中的副本完成`leader`副本中数据的同步，那么该副本会再次加入ISR列表

**`OSR(outof-sync replicas)`：**滞后同步副本

**`AR(all replicas)`：**全部副本。`AR = ISR + OSR`

当消息发送给`broker`并收到`broker`的`ack`应答，那么消息就发送成功，此时，`RecoderAccumulator`和`InFlightRequests`会删除相应的`ProducerBatch`；如果没有收到`ack`应答，那么消息发送失败，此时，`sender`线程会重新发送该消息，重试的次数默认为`int`类型的最大值，即“死磕”。

**生产者消息发送流程图**

![Kafka - 生产者消息发送原理及流程](./05-Kafka.assets/Kafka - 生产者消息发送原理及流程.png)

## 3.2、生产者API

在`IDEA`中编写`Kafka`代码首先需要引入`Kafka`的依赖：此处引入的是`Kafka 3.0`版本

```xml
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>3.0.0</version>
</dependency>
```

### 3.2.1 生产者发送数据

编写生产者代码总共分为三步：一是创建生产者对象（即开启一个`Kafka`客户端）；二是发送数据；三是关闭资源（即关闭该`Kafka`客户端）。其中数据的发送有两种方式，一种是异步发送，一种是同步发送。这里所说的同步和异步，指的是外部数据与消息累加器`RecoderAccumulator`的同步和异步，同步发送时，外部数据发送到消息累加器后，还需要等待，消息从消息累加器成功发送给`broker`，才会发送下一条数据；而异步发送时，外部数据只需要成功发送给消息累加器，不管该数据有没有成功发送给`broker`，外部系统都可以继续向消息累加器发送数据。

此外，在同步发送和异步发送的基础上，还可分为，带回调的数据发送和不带回调的数据发送。回调的信息同样由消息累加器返回，主要包含数据将发送到哪个主题、哪个分区以及消息写入`Kafka`的时间。

同步数据发送和异步数据发送，在代码的体现上，主要在于，生产者调用`send()`方法发送数据时，有没有传入第二个`Callback`类型的参数。

**使用不同方式进行数据发送**

```java
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

/**
 * @author shaco
 * @create 2023-05-17 21:39
 * @desc 生产者API：异步不带回调数据发送；带回调数据发送；同步不带回调数据发送；同步带回调数据发送
 */
public class Demo01_SendData {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // TODO 0、Kafka生产者配置：通过ProducerConfig对象设置生产者的配置，并装入Properties集合中
        // 创建Properties集合
        Properties producerProp = new Properties();
        // 配置Kafka集群连接地址，一般配置集群中的两个节点的访问地址和端口号，必须
        producerProp.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"hadoop132:9092,hadoop133:9092");

        // 配置数据的序列化方式，Kafka中的数据存储和反序列化一般使用字符串
        // Kafka中，数据一般以key-value的形式存在，所以需要分别配置key和value的序列化方式
        producerProp.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProp.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // TODO 1、创建生产者对象
        KafkaProducer<String, String> producer = new KafkaProducer<String, String>(producerProp);

        // TODO 2、发送数据
        for (int i = 1;i < 5; i++) {
            ProducerRecord<String, String> record = new ProducerRecord<>("first", "hello world " + i);
            
            // TODO 不带回调的异步数据发送方式，只需要调用send()方法将数据发送出去即可
            producer.send(record);
            
            // TODO 带回调的异步数据发送方式，需要传入第二个Callback类型的参数
            producer.send(record, new Callback() {
                @Override
                public void onCompletion(RecordMetadata metadata, Exception exception) {
                    if (exception == null){ // 没有异常，说明数据发送成功
                        String topic = metadata.topic();
                        int partition = metadata.partition();
                        long timestamp = metadata.timestamp();
                        System.out.println("topic: " + topic + "，分区：" + partition + "，写入Kafka的时间：" + timestamp);
                    }
                }
            });
            
            // TODO 不带回调的同步数据发送方式，只需要基于异步发送方式，再调用get()方法即可
            producer.send(record).get(); // 注意要进行异常处理
            
            // TODO 带回调的同步数据发送方式，还需要传入第二个参数
            producer.send(record, new Callback() {
                @Override
                public void onCompletion(RecordMetadata metadata, Exception exception) {
                    if (exception == null){ // 没有异常，说明数据发送成功
                        String topic = metadata.topic();
                        int partition = metadata.partition();
                        long timestamp = metadata.timestamp();
                        System.out.println("topic: " + topic + "，分区：" + partition + "，写入Kafka的时间：" + timestamp);
                    }
                }
            }).get(); // 注意要进行异常处理
        }

        // TODO 3、关闭资源
        producer.close();
    }
}
```

**API相关说明：**

**创建生产者对象时，所需要的配置说明：**创建生产者对象，需要通过`ProducerConfig`对象对生产者相关属性进行配置，`ProducerConfig`中声明了许多常量，用于配置生产者对象的属性，常用属性声明如下：

```Java
public class ProducerConfig extends AbstractConfig {

    // Kafka集群连接地址
    public static final String BOOTSTRAP_SERVERS_CONFIG = CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;

    // 数据的key的序列化方式
    public static final String KEY_SERIALIZER_CLASS_CONFIG = "key.serializer";

    // 数据的value的序列化方式
    public static final String VALUE_SERIALIZER_CLASS_CONFIG = "value.serializer";

    // 消息累加器中，每个双端队列里，ProducerBatch的大小，默认16K，达到该容量，sender线程将会来读取数据，发送给broker
    public static final String BATCH_SIZE_CONFIG = "batch.size";

    // ack应答级别
    public static final String ACKS_CONFIG = "acks";

    // 消息累加器中，每个双端队列里，ProducerBatch等待
    public static final String LINGER_MS_CONFIG = "linger.ms";

    /** <code>compression.type</code> */
    public static final String COMPRESSION_TYPE_CONFIG = "compression.type";

    /** <code>max.in.flight.requests.per.connection</code> */
    public static final String MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION = "max.in.flight.requests.per.connection";

    /** <code>retries</code> */
    public static final String RETRIES_CONFIG = CommonClientConfigs.RETRIES_CONFIG;

    /** <code>partitioner.class</code> */
    public static final String PARTITIONER_CLASS_CONFIG = "partitioner.class";

    /** <code>interceptor.classes</code> */
    public static final String INTERCEPTOR_CLASSES_CONFIG = "interceptor.classes";

    /** <code> transaction.timeout.ms </code> */
    public static final String TRANSACTION_TIMEOUT_CONFIG = "transaction.timeout.ms";

    /** <code> transactional.id </code> */
    public static final String TRANSACTIONAL_ID_CONFIG = "transactional.id";

    public static final String SECURITY_PROVIDERS_CONFIG = SecurityConfig.SECURITY_PROVIDERS_CONFIG;

    private static final AtomicInteger PRODUCER_CLIENT_ID_SEQUENCE = new AtomicInteger(1);

}

```

