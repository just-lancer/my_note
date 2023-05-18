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

**`ISR(In-sync replicas)`：**同步副本。在最长滞后时间内（也就是一定时间内），能完成`leader`数据同步的副本称为同步副本。超过最长滞后时间，副本还未完成数据同步会被踢出`ISR`列表，加入`OSR`列表，当`OSR`列表中的副本完成`leader`副本中数据的同步，那么该副本会再次加入`ISR`列表

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

### 3.2.1 生产者数据发送

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

    // ack应答级别，默认取值为-1(all)
    public static final String ACKS_CONFIG = "acks";

    // 消息累加器中，每个双端队列里，ProducerBatch等待
    public static final String LINGER_MS_CONFIG = "linger.ms";

    // 压缩方式，Kafka支持的压缩方式有：gzip，snappy，lz4，zstd，该配置项默认取值是none，即不开启压缩
    public static final String COMPRESSION_TYPE_CONFIG = "compression.type";

    // 配置sender线程中InFlightRequests的容量大小，建议配置数量 ≤ 5
    public static final String MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION = "max.in.flight.requests.per.connection";

    // sender线程发送消息失败时，重试次数，默认值为int类型的最大值
    public static final String RETRIES_CONFIG = CommonClientConfigs.RETRIES_CONFIG;

    // 自定义的分区器，当开发者配置了该项，Kafka会自动调用该分区器
    public static final String PARTITIONER_CLASS_CONFIG = "partitioner.class";

    // 自定义拦截器
    public static final String INTERCEPTOR_CLASSES_CONFIG = "interceptor.classes";

    // 事务超时时间
    public static final String TRANSACTION_TIMEOUT_CONFIG = "transaction.timeout.ms";

    // 事务id，必须由用户指定
    public static final String TRANSACTIONAL_ID_CONFIG = "transactional.id";

}
```

**发送数据时，异步发送和同步发送、带回调和不带回调的说明：**上面已经对数据同步发送和异步发送做了说明，同步和异步主要是指外部数据系统与生产者内部消息累加器的数据同步和异步，在代码的体现上，同步数据发送需要在异步数据发送代码的基础上，调用`get()`方法即可。

带回调数据发送和不带回调的数据发送方式，是指数据在`main()`线程中进入消息累加器后，由消息累加器返回的数据发送的`topic`以及分区信息，同时，还包括数据进入`broker`的时间。在代码的体现上，带回调数据发送方式需要在不带回调数据发送方式的基础上，在`send()`方法中传入第二个参数：`Callback`，`Callback`是一个接口，声明了一个抽象方法`onCompletion()`，包含两个参数，其中`RecordMetadata`类型参数提供了消息的相关元数据信息；`Exception`类型参数则提供了数据发送过程中的异常信息，如果该参数不为`null`，那么表示有异常出现，数据发送失败。

**关于创建`KafkaProducer`时，定义的泛型的说明：**`Kafka`中的消息都由两部分构成，第一部分是消息头`head`，一般是数据的元数据信息，例如数据发往哪个主题的哪个分区。第二部分是消息体`body`，是消息本身序列化之后的字节数组。消息头和消息体的组织形式是`key-value`，所以，在创建`KafkaProducer`时，指定的两个泛型，分别表示`key`和`value`的类型。

### 3.2.2 生产者分区

在`Kafka`中，对`topic`进行分区有两方面的好处。在存储方面，将`topic`的每个分区存储在不同的`broker`节点上，通过合理控制分区数据量大小，能够合理使用存储资源；在计算方面，能够提高生产者数据发送的并行度，也能够提高消费者消费数据的并行度。

**生产者默认分区策略：**

对于程序开发者而言，当调用`send()`方法发送消息之后，消息就自然而然的发送到了`broker`中。然而在这一过程中，消息还需要经过拦截器、序列化器和分区器的一系列处理后才能被真正地发往`broker`。

在`Kafka`中，`ProducerRecord`对象代表生产者产生的消息，即一组键值对。根据其重载构造器，可以知悉`Kafka`的默认分区策略：

![image-20230518135819148](./05-Kafka.assets/image-20230518135819148.png)

-   **当显示指明消息的分区编号，即传入`Integer partition`参数时，消息将被发往指定的分区中**
-   **当没有显示指明消息的分区编号，但指明消息（消息本身是`value`）的`key`，即传入`String key`参数时，`Kafka`会将`key`的`hashcode`值与分区数取余的结果作为分区编号**
-   **当既没有指明分区编号，也没有指定消息的`key`，`Kafka`会采用粘性分区策略`sticky partition`，即随机选择一个分区，向该分区中的`ProducerBatch(16K)`中写入数据，直到`ProducerBatch`被装满，或到达设置的时间，数据被`sender`线程读取，才会再选择一个分区进行使用（和上次不同的分区）**

**自定义分区：**

-   **创建一个类实现`Partitioner`接口，并实现其抽象方法`partition()`、`close()`、`configure()`**。其中`partition()`方法用于定义数据的分区逻辑
-   **在生产者配置中，添加自定义分区器参数**

**自定义分区演示：将包含`hello`字符串的消息发送到`first`主题的`0`号分区中，其他字符串发送`1`号分区：**

```java
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.serialization.StringSerializer;
import java.util.Map;
import java.util.Properties;

/**
 * @author shaco
 * @version Flink 1.13.6，Flink CDC 2.2.1
 * @create 2023-05-18 19:59
 * @desc 自定义生产者分区器
 */
public class Demo02_CustomerPartitioner implements Partitioner {
    // TODO 实现三个抽象方法：partition()：定义分区逻辑；close()：关闭资源；configure()：不用管
    // 各参数含义：
    // @param topic         topic，即主题
    // @param key           消息的key
    // @param keyBytes      消息的key经过序列化之后的字节数组
    // @param value         消息的value
    // @param valueBytes    消息的value经过序列化之后的字节数组
    // @param cluster       Kafka集群的元数据，可用于查看主题、分区等元数据信息
    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
        String message = value.toString();
        int partition;
        if (message.contains("hello")) {
            partition = 0;
        } else {
            partition = 1;
        }
        return partition;
    }

    // 用于关闭资源
    @Override
    public void close() {

    }

    @Override
    public void configure(Map<String, ?> configs) {

    }

    public static void main(String[] args) {
        // 利用自定义分区器，进行数据发送
        // 0 Kafka生产者配置
        Properties properties = new Properties();

        // Kafka连接地址，以及序列化器
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "hadoop132:9092");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // TODO 为了让Kafka使用自定义分区器进行数据发送，需要利用ProducerConfig对象进行配置
        properties.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, "_case.producer.Demo02_CustomerPartitioner");

        // 1、创建Kafka生产者
        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

        // 2、发送数据
        String[] strings = {"hello", "hello", "hello", "hello", "world"};
        for (int i = 0; i < strings.length; i++) {
            ProducerRecord<String, String> message = new ProducerRecord<>("first", strings[i] + i);
            producer.send(message, new Callback() {
                @Override
                public void onCompletion(RecordMetadata metadata, Exception exception) {
                    if (exception == null) {
                        System.out.println("topic: " + metadata.topic() + "; partition: " + metadata.partition());
                    } else {
                        exception.printStackTrace();
                    }
                }
            });
        }

        // 3、关闭资源
        producer.close();
    }
}
```

## 3.3、Kafka生产经验

### 3.3.1 优化：生产者提高吞吐量

在任何框架中，提高数据传输效率的核心原则都是一样的，即减少数据量，减少`IO`操作，减少网络请求次数（也就是减小资源开关次数）等。

**利用缓存手段，减少网络请求的次数，提高消息推送效率。可以配置的参数有三个：**

-   **`buffer.memory`：**`RecoderAccumulator`的大小，默认`32 M`
-   **`batch.size`：**`RecoderAccumulator`中，数据批次`ProducerBatch`的大小，默认值为`16 K`
-   **`linger.ms`：**`sender`线程从`RecoderAccumulator`中，读取`ProducerBatch`数据的时间间隔，默认值`0 ms`。也就是说，默认情况下，每有一条数据写入到`RecoderAccumulator`的`ProducerBatch`中，就会立刻被`sender`线程读取，并被发送给`broker`。当该配置项是默认值时，`batch.size`配置项相当于没有起作用。生产环境中，该配置项一般配置为`5-100 ms`

**使用数据压缩，减少数据传输量，也能够提高数据吞吐量，相应配置参数为：**

-   **`compression.type`：**默认值为`none`，即不开启压缩。`Kafka`支持的压缩方式有：`none`、`gzip`、`snappy`、`lz4`和`zstd`

**==这些参数都可以通过`ProducerConfig`对象进行设置==**

**需要说明的是，任何事务都有两面性，在提高吞吐量的同时也给`Kafka`的性能带来一定的影响。例如，配置了`linger.ms`配置项，能够对数据进行攒批，提高数据传输效率，但同时也会使得数据传输有一定延迟。对于数据压缩也是一样，压缩数据可以提高数据传输效率，但是也需要消耗一定的计算资源和时间。**

### 3.3.2 数据可靠性分析

数据可靠性主要是指数据是否会丢失，具体体现在数据是否能够被发往`broker`，并写入磁盘，这与`Kafka`的`ack`应答级别有关。

**数据发送的简要流程：**

-    生产者发送数据到`broker`中
-    `leader`接收数据
-    `leader`向磁盘中写入数据
-    `follower`将数据从`leader`中同步到本地磁盘
-    `ack`应答

**`ack`应答级别说明：前面也有说明**

-   **`ack = 0`，生产者发送的消息无需等待`broker`写入磁盘就判定数据发布成功，因此具有数据丢失的风险**
-   **`ack = 1`，生产者发布的消息在`leader`接收并写入磁盘中，判定为数据发布成功，如果在`leader`接收并写入磁盘后，在`follower`同步`leader`的数据之前，`leader`发生故障，也会出现数据丢失的风险**
-   **`ack = -1(all)`，生产者发布的消息在`ISR`列表中所有副本都同步并写入磁盘后，判定数据发布成功。当分区副本设置为`1`（只有`leader`，没有`follower`），或者`ISR`列表应答最小副本数量为`1`（就只剩下`leader`），那么此时的`ack`应答级别和`ack = 1`相同，因此也存在数据丢失的风险。**
    -   **`ISR`列表应答最小副本数量的配置参数：`min.insync.replicas`，默认为`1`。**

因此，彻底解决数可靠性问题，需要`Kafka`集群满足以下条件：

-   **`ack`应答级别设置为`1`或者`all`**
-   **`ISR`最小应答副本数量大于等于`2`：为了能满足这个要求，需要设置主题的分区副本数量也要大于等于`2`**

**==`ack`应答级别可以通过`ProducerConfig`进行配置==**

>   **`ISR(In-sync replicas)`：**同步副本。在最长滞后时间内（也就是一定时间内），能完成`leader`数据同步的副本称为同步副本。超过最长滞后时间，副本还未完成数据同步会被踢出`ISR`列表，加入`OSR`列表，当`OSR`列表中的副本完成`leader`副本中数据的同步，那么该副本会再次加入`ISR`列表
>
>   **`OSR(outof-sync replicas)`：**滞后同步副本
>
>   **`AR(all replicas)`：**全部副本。`AR = ISR + OSR`

### 3.3.3 数据重复性分析

**数据发送的简要流程：**

-    生产者发送数据到`broker`中
-    `leader`接收数据
-    `leader`向磁盘中写入数据
-    `follower`将数据从`leader`中同步到本地磁盘
-    `ack`应答

**数据重复性问题的原因仍在`ack`应答方面。当`ack = -1`时，在`leader`和`follower`都进行了数据同步和磁盘写入后，`leader`准备进行`ack`应答时，`leader`出现故障，那么此时会选举出新的`leader`，而生产者由于没有收到`ack`应答，判定数据传输失败，进而进行数据传输重试，因此在`broker`中会出现重复的数据。尽管这种事件出现的概率极小，但在涉及到高精度数据的领域，例如经济交易，也需要完全解决数据重复问题。**

**在`Kafka`中，解决数据重复性问题的手段主要有两种：幂等性和事务**

**==幂等性==**

>   幂等性是指对同一个操作进行多次执行所产生的结果是相同的。在计算机科学中，幂等性通常用于描述网络协议、`API`接口等操作的特性。例如，`HTTP`协议中的`GET`请求就是幂等的，因为对同一个`URL`进行多次`GET`请求所得到的结果是相同的。而`POST`请求则不是幂等的，因为对同一个`URL`进行多次`POST`请求所产生的结果可能不同。在`API`接口中，幂等性通常用于保证数据的一致性和可靠性，避免重复操作导致数据错误或重复。

幂等性是针对生产者的特性，幂等性可以保证生产者发送的消息，不会丢失、也不会重复。而实现幂等性的关键在于，服务端`broker`能否区分请求是否重复，进而过滤重复请求。而区分请求是否重复的关键有两点：一是请求中是否有唯一标识；二是是否记录已处理过的请求。

**在`Kafka`中，消息有一个由一组编号构成的唯一标识：`<ProducerID, Partition, SequenceNumber>`**

-   **`ProducerID`：**每个生产者客户端启动时（也就是创建了一个`KafkaProducer`对象），`Kafka`都会为其分配一个唯一的`ProducerID`，该`ProducerID`对用户不可见，即不能被用户修改。`ProducerID`只在单会话中有效，换句话说，当生产者客户端关闭后，分配给该客户端的`ProducerID`也会随之失效，再重启该生产者客户端，`Kafka`会重新为其分配一个`ProducerID`
-   **`Partition`：**是消息的分区号
-   **`SequenceNumber`：**对于每个`ProducerID`，其对应的生产者客户端发送数据的每条数据都有一个从`0`开始单调递增的`SequenceNumber`

幂等性的原理正是借助消息的唯一标识来实现的。当生产者发送一条消息时，会为这条消息生成一个唯一的标识`ID`，并将这个`ID`和消息一起发送到`Kafka`集群。`Kafka`集群会根据这个`ID`来判断这条消息是否已经被发送过，如果已经发送过，则只会保留一条消息。如果这条消息是新的，则会将消息写入到`Kafka`的主题分区中。

利用幂等性，能够在单会话内解决数据的重复问题，然而跨会话的数据传输仍旧会出现数据重复问题，原因正如上述所提及到的，重新开启会话，`Kafka`会为生产者分配新的`ProducerID`。为了解决跨会话数据重复性问题，`Kafka`引入了新特性：事务。

**`Kafka`中幂等性配置参数为`enable.idempotence`，默认值为`true`，即`Kafka`默认开启幂等性**

>   **会话（`Session`）是指在客户端和服务器之间建立的一种持久的连接，用于在一段时间内保持交互状态。创建一个生产者对象`KafkaProducer`就是创建了一个会话，当关闭生产者对象时，会话就结束了。**

**==事务==**
