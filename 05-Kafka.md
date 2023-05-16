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

![Kafka - 生产者消息发送原理及流程](./05-Kafka.assets/Kafka - 生产者消息发送原理及流程.png)