<h1 align = "center">阿里云全托管Flink Connector使用
</h1>

# 一、技术选型

-   **`Flink 1.13.6`**
-   **`Flink CDC 2.2.1`**
-   **`vvr-4.0.16-flink-1.13`**

# 二、Flink提供的连接器以及Flink CDC支持的数据源

**`Flink 1.13.6 Connector`**

-   **`Apache Kafka`：`source & sink`**
-   **`Elasticsearch`：`sink`**
-   **`FileSystem`：`source & sink`**
-   **`RabbitMQ`：`source & sink`**
-   **`Apache NiFi`：`source & sink`**
-   **`JDBC`：`sink`**
-   **`Apache ActiveMQ`：`source & sink`**
-   **`Apache Flume`：`sink`**
-   **`Redis`：`sink`**

**==`HBase`可以写，暂时不能==**

**`Flink CDC 2.2.1 Source`**

-   **`MongoDB`**
-   **`MySQL`**
-   **`Oceanbase`**
-   **`Oracle`**
-   **`Postgres`**
-   **`SqlServer`**
-   **`TiDB`**

# 三、pom.xml配置

```xml
<properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <flink.version>1.13.6</flink.version>
    <target.java.version>1.8</target.java.version>
    <scala.binary.version>2.11</scala.binary.version>
    <maven.compiler.source>${target.java.version}</maven.compiler.source>
    <maven.compiler.target>${target.java.version}</maven.compiler.target>
    <log4j.version>2.12.1</log4j.version>
</properties>

<repositories>
    <repository>
        <id>apache.snapshots</id>
        <name>Apache Development Snapshot Repository</name>
        <url>https://repository.apache.org/content/repositories/snapshots/</url>
        <releases>
            <enabled>false</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>

<dependencies>
    <!-- Apache Flink dependencies -->
    <!-- These dependencies are provided, because they should not be packaged into the JAR file. -->
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-java</artifactId>
        <version>${flink.version}</version>
        <!--            <scope>provided</scope>-->
    </dependency>
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-streaming-java_${scala.binary.version}</artifactId>
        <version>${flink.version}</version>
        <!--            <scope>provided</scope>-->
    </dependency>
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-clients_${scala.binary.version}</artifactId>
        <version>${flink.version}</version>
        <!--            <scope>provided</scope>-->
    </dependency>

    <!-- flink Table & SQL API-->
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-table-api-java-bridge_${scala.binary.version}</artifactId>
        <version>${flink.version}</version>
        <!--            <scope>provided</scope>-->
    </dependency>
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-table-planner-blink_${scala.binary.version}</artifactId>
        <version>${flink.version}</version>
        <!--            <scope>provided</scope>-->
    </dependency>
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-streaming-scala_${scala.binary.version}</artifactId>
        <version>${flink.version}</version>
        <!--            <scope>provided</scope>-->
    </dependency>
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-table-common</artifactId>
        <version>${flink.version}</version>
        <!--            <scope>provided</scope>-->
    </dependency>

    <!-- flink cdc 2.2.0版本需要8.0.21的MySQL驱动 -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>8.0.21</version>
    </dependency>

    <dependency>
        <groupId>com.ververica</groupId>
        <artifactId>flink-connector-mysql-cdc</artifactId>
        <!-- 请使用已发布的版本依赖，snapshot版本的依赖需要本地自行编译。 -->
        <version>2.2.1</version>
    </dependency>

    <!-- 将数据写入MySQL，需要使用JDBC连接器 -->
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-connector-jdbc_${scala.binary.version}</artifactId>
        <version>${flink.version}</version>
    </dependency>

    <!-- MongoDB 驱动-->
    <dependency>
        <groupId>org.mongodb</groupId>
        <artifactId>mongo-java-driver</artifactId>
        <version>3.12.7</version>
    </dependency>

    <dependency>
        <groupId>com.ververica</groupId>
        <artifactId>flink-connector-mongodb-cdc</artifactId>
        <!-- The dependency is available only for stable releases, SNAPSHOT dependency need build by yourself. -->
        <version>2.2.1</version>
    </dependency>

    <!-- Flink Kafka Connector，需要引入以下两个包 -->
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-connector-base</artifactId>
        <version>${flink.version}</version>
    </dependency>
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-connector-kafka_${scala.binary.version}</artifactId>
        <version>${flink.version}</version>
    </dependency>


    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>fastjson</artifactId>
        <version>1.2.8</version>
    </dependency>


    <!-- Add connector dependencies here. They must be in the default scope (compile). -->

    <!-- Example:

    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-connector-kafka_${scala.binary.version}</artifactId>
        <version>${flink.version}</version>
    </dependency>
    -->

    <!-- Add logging framework, to produce console output when running in the IDE. -->
    <!-- These dependencies are excluded from the application JAR by default. -->
    <dependency>
        <groupId>org.apache.logging.log4j</groupId>
        <artifactId>log4j-slf4j-impl</artifactId>
        <version>${log4j.version}</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.apache.logging.log4j</groupId>
        <artifactId>log4j-api</artifactId>
        <version>${log4j.version}</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.apache.logging.log4j</groupId>
        <artifactId>log4j-core</artifactId>
        <version>${log4j.version}</version>
        <scope>runtime</scope>
    </dependency>
</dependencies>

<build>
    <plugins>

        <!-- Java Compiler -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.1</version>
            <configuration>
                <source>${target.java.version}</source>
                <target>${target.java.version}</target>
            </configuration>
        </plugin>

        <!-- We use the maven-shade plugin to create a fat jar that contains all necessary dependencies. -->
        <!-- Change the value of <mainClass>...</mainClass> if your program entry point changes. -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>3.1.1</version>
            <executions>
                <!-- Run shade goal on package phase -->
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>shade</goal>
                    </goals>
                    <configuration>
                        <artifactSet>
                            <excludes>
                                <exclude>org.apache.flink:force-shading</exclude>
                                <exclude>com.google.code.findbugs:jsr305</exclude>
                                <exclude>org.slf4j:*</exclude>
                                <exclude>org.apache.logging.log4j:*</exclude>
                            </excludes>
                        </artifactSet>
                        <filters>
                            <filter>
                                <!-- Do not copy the signatures in the META-INF folder.
                                Otherwise, this might cause SecurityExceptions when using the JAR. -->
                                <artifact>*:*</artifact>
                                <excludes>
                                    <exclude>META-INF/*.SF</exclude>
                                    <exclude>META-INF/*.DSA</exclude>
                                    <exclude>META-INF/*.RSA</exclude>
                                </excludes>
                            </filter>
                        </filters>
                        <!--                            <transformers>
                                                        <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                                            <mainClass>org.example.StreamingJob</mainClass>
                                                        </transformer>
                                                    </transformers>-->
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>

    <pluginManagement>
        <plugins>

            <!-- This improves the out-of-the-box experience in Eclipse by resolving some warnings. -->
            <plugin>
                <groupId>org.eclipse.m2e</groupId>
                <artifactId>lifecycle-mapping</artifactId>
                <version>1.0.0</version>
                <configuration>
                    <lifecycleMappingMetadata>
                        <pluginExecutions>
                            <pluginExecution>
                                <pluginExecutionFilter>
                                    <groupId>org.apache.maven.plugins</groupId>
                                    <artifactId>maven-shade-plugin</artifactId>
                                    <versionRange>[3.1.1,)</versionRange>
                                    <goals>
                                        <goal>shade</goal>
                                    </goals>
                                </pluginExecutionFilter>
                                <action>
                                    <ignore/>
                                </action>
                            </pluginExecution>
                            <pluginExecution>
                                <pluginExecutionFilter>
                                    <groupId>org.apache.maven.plugins</groupId>
                                    <artifactId>maven-compiler-plugin</artifactId>
                                    <versionRange>[3.1,)</versionRange>
                                    <goals>
                                        <goal>testCompile</goal>
                                        <goal>compile</goal>
                                    </goals>
                                </pluginExecutionFilter>
                                <action>
                                    <ignore/>
                                </action>
                            </pluginExecution>
                        </pluginExecutions>
                    </lifecycleMappingMetadata>
                </configuration>
            </plugin>
        </plugins>
    </pluginManagement>
</build>
```

**需要说明的是，对于`Flink`相关依赖，包括`DataStream API`依赖、`Table API`和`Flink SQL`依赖，在`Flink`的`lib`目录下都有，阿里云全托管`Flink`无需将其打进`jar`包。因此其`scope`标签的值是`provided`。**

# 四、读取数据源

## 4.1、读取MySQL

读取`MySQL`数据源，需要使用`Flink CDC`，因此需要引入相应的依赖：

```xml
<!-- Flink CDC MySQL Connector 2.2.1 版本-->
<dependency>
    <groupId>com.ververica</groupId>
    <artifactId>flink-connector-mysql-cdc</artifactId>
    <version>2.2.1</version>
</dependency>

<!-- Flink cdc 2.2.0版本需要MySQL 8.0.21驱动的支持 -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.21</version>
</dependency>
```

**使用`Flink DataStream API`读取`MySQL`数据，并直接打印控制台：**

**需要注意的是，使用`Flink CDC`读取`MySQL`时，首先会通过`SQL`读取全量`MySQL`数据，读取完成后，将开始监控`Binlog`，读取增量数据。为了保证`Flink CDC`读取完全量数据后，能够开始监控并读取增量数据，该`Flink Job`需要开启检查点`Checkpoint`。**

```java
public static void main(String[] args) throws Exception {
    // TODO 配置MySQL连接
    MySqlSource<String> mySqlSource = MySqlSource.<String>builder()
            .hostname("rm-2ze42x1o6er3410na.mysql.rds.aliyuncs.com")
            .port(3306)
            .databaseList("data_domain_test") // 设置捕获的数据库， 如果需要同步整个数据库，请将 tableList 设置为 ".*".
            .tableList("data_domain_test.test[0-9]_pqg") // 设置捕获的表，支持正则表达式
            .username("data_domain_test")
            .password("learnable@ANA")
            .deserializer(new JsonDebeziumDeserializationSchema()) // 将 SourceRecord 转换为 JSON 字符串
            .build();

    // TODO 创建流执行环境
    StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
    // 开启检查点，设置 3s 的 checkpoint 间隔
    env.enableCheckpointing(3000);

    env.fromSource(mySqlSource, WatermarkStrategy.noWatermarks(), "MySQL Source")
            .print();
    env.execute("Print MySQL Snapshot + Binlog");
}
```

另外一个值得说明的点是，在读取到数据后，需要对数据进行反序列化，上述代码中，使用的是`JSON`反序列化器`JsonDebeziumDeserializationSchema`，包含有较多的信息。

**`JSON`结构如下：**

```json
{
  "before": null,
  "after": {
    "id": 1001,
    "name": "zhangsan",
    "age": 18,
    "birthday": 1589587200000
  },
  "source": {
    "version": "1.5.4.Final",
    "connector": "mysql",
    "name": "mysql_binlog_source",
    "ts_ms": 0,
    "snapshot": "false",
    "db": "data_domain_test",
    "sequence": null,
    "table": "test1_pqg",
    "server_id": 0,
    "gtid": null,
    "file": "",
    "pos": 0,
    "row": 0,
    "thread": null,
    "query": null
  },
  "op": "r",
  "ts_ms": 1684382259539,
  "transaction": null
}
```

**字段含义如下：少了两个字段**

```txt
before：表示操作前的数据，是一个JSON对象，包含了所有字段及其值。
after：表示操作后的数据，是一个JSON对象，包含了所有字段及其值。
source：表示事件的来源，是一个JSON对象，包含了以下字段：
version：表示Debezium的版本号。
connector：表示连接器的名称。
name：表示事件的名称
ts_ms：表示事件的时间戳，单位为毫秒。
snapshot：表示是否为快照事件，取值为true或false。
db：表示数据库的名称。
table：表示表的名称。
server_id：表示MySQL服务器的ID。
gtid：表示MySQL GTID。
file：表示MySQL binlog文件名。
pos：表示MySQL binlog文件的位置。
row：表示事件的类型，取值为c（create）、u（update）或d（delete）。
op：表示事件的类型，取值为c（create）、u（update）或d（delete）。
ts_ms：表示事件的时间戳，单位为毫秒。
```

除了`JSON`反序列化器，还可以使用字符串反序列化器`StringDebeziumDeserializationSchema`，但所获得的数据并不利于解析，因此使用字符串反序列化器时，一般都进行自定义。

自定义反序列化器需要实现`DebeziumDeserializationSchema`接口，实现其抽象方法`deserialize()`。由于`DebeziumDeserializationSchema`接口继承自`ResultTypeQueryable`接口，因此还需要实现抽象方法`getProducedType()`，用来指定反序列化得到的数据类型。

**自定义字符串反序列化器：**

```Java
public class MyDeserialization implements DebeziumDeserializationSchema<String> {
    /**
     * {
     * "db":"",
     * "tableName":"",
     * "before":{"id":"1001","name":""...},
     * "after":{"id":"1001","name":""...},
     * "op":""
     * }
     */

    @Override
    public void deserialize(SourceRecord sourceRecord, Collector<String> collector) throws Exception {
        //创建JSON对象用于封装结果数据
        JSONObject result = new JSONObject();

        //获取库名&表名
        String topic = sourceRecord.topic();
        String[] fields = topic.split("\\.");
        result.put("db", fields[1]);
        result.put("tableName", fields[2]);

        //获取before数据
        Struct value = (Struct) sourceRecord.value();
        Struct before = value.getStruct("before");
        JSONObject beforeJson = new JSONObject();
        if (before != null) {
            //获取列信息
            Schema schema = before.schema();
            List<Field> fieldList = schema.fields();

            for (Field field : fieldList) {
                beforeJson.put(field.name(), before.get(field));
            }
        }
        result.put("before", beforeJson);

        //获取after数据
        Struct after = value.getStruct("after");
        JSONObject afterJson = new JSONObject();
        if (after != null) {
            //获取列信息
            Schema schema = after.schema();
            List<Field> fieldList = schema.fields();

            for (Field field : fieldList) {
                afterJson.put(field.name(), after.get(field));
            }
        }
        result.put("after", afterJson);

        //获取操作类型
        Envelope.Operation operation = Envelope.operationFor(sourceRecord);
        result.put("op", operation);

        //输出数据
        collector.collect(result.toJSONString());
    }

    @Override
    public TypeInformation<String> getProducedType() {
        return TypeInformation.of(String.class);
    }
}
```

另一个需要说明的是，关于为每个`Reader`设置不同的`Server id`。

每个用于读取`binlog`的`MySQL`数据库客户端都应该有一个唯一的`id`，称为`Server id`。`MySQL`服务器将使用这个`id`来维护网络连接和`binlog`位置。因此，如果不同的作业共享相同的`Server id`，可能会导致从错误的`binlog`位置读取数据，因此，建议通过为每个`Reader`设置不同的`Server id`。假设`Source`的并行度为4，那么就有4个`Reader`，为这四个`Reader`设置`Server id`，需要在建造者模式中，调用`serverId()`方法，传入字符串类型的数据区间，例如，`1000-1003`，表示`Server id`分别为`1000`、`1001`、`1002`、`1003`。

默认情况下，`Flink Job`的并行度是其`TaskManager`所在节点的核心数，例如电脑的核心数是`16`，那么在`IDEA`中运行`Flink Job`时，所有算子的并行度都将默认为`16`。

需要注意的是`Source`算子，直接继承自`SourceFunction`接口的`Source`算子，其并行度一直都是`1`，无法设置，也不算运行环境变化而变化。而使用`Flink CDC`创建的`MySQL`数据源`MySqlSource`，直接继承自`Source`，其并行度默认为节点的核心数。

## 4.2、读取MongoDB

读取`MongoDB`的数据需要借助`Flink CDC`，需要引入以下依赖：

```xml
<!-- MongoDB 驱动-->
<dependency>
    <groupId>org.mongodb</groupId>
    <artifactId>mongo-java-driver</artifactId>
    <version>3.12.7</version>
</dependency>
<!-- Flink CDC MongoDB Connector 2.2.1 版本-->
<dependency>
    <groupId>com.ververica</groupId>
    <artifactId>flink-connector-mongodb-cdc</artifactId>
    <version>2.2.1</version>
</dependency>
<!-- Kafka client -->
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>2.8.0</version>
</dependency>
```

使用`Flink CDC`读取`MongoDB`的数据，与读取`MySQL`的数据类似。

**读取`MongoDB`的数据，并打印控制台**

```Java
public static void main(String[] args) throws Exception {
    SourceFunction<String> sourceFunction = MongoDBSource.<String>builder()
            .hosts("dds-2zec6d36a5bf5a041574-pub.mongodb.rds.aliyuncs.com:3717")
            .username("data_domain_test")
            .password("learnableANA")
            .databaseList("data_domain_test") // set captured database, support regex
            .collectionList("data_domain_test.ads_org_sch_degree_dict") //set captured collections, support regex
            .deserializer(new JsonDebeziumDeserializationSchema())
            .build();

    StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

    env.addSource(sourceFunction)
            .print();

    env.execute();
}
```

**测试结果并未通过，报身份验证失败**

## 4.3、读取Kafka

读取`Kafka`的数据需要使用`Flink`本身自带的连接器，需要引入以下依赖：

```xml
<dependency>
    <groupId>org.apache.flink</groupId>
    <artifactId>flink-connector-base</artifactId>
    <version>${flink.version}</version>
</dependency>
<dependency>
    <groupId>org.apache.flink</groupId>
    <artifactId>flink-connector-kafka_${scala.binary.version}</artifactId>
    <version>${flink.version}</version>
</dependency>
```

`Flink`读取`Kafka`数据有两种方式，一种是自定义`Kafka Consumer`，另一种是使用`Flink Kafka Consumer`。两种方式的主要区别在于，`Flink Kafka Consumer`的方式能够实现精确一次性语义。

**方式一：自定义`Kafka Consumer`读取`Kafka`数据，并打印控制台：**

```Java
public static void main(String[] args) throws Exception {
    // Kafka连接配置
    KafkaSource<String> stringKafkaSource = KafkaSource.<String>builder()
            .setBootstrapServers("hadoop132:9092,hadoop133:9092")
            .setTopics("first")
            .setGroupId("first_test_group")
            .setStartingOffsets(OffsetsInitializer.earliest())
            .setValueOnlyDeserializer(new SimpleStringSchema())  // 只为value设置反序列化方式，其他的信息，例如key，则被忽略
            .build();

    // 创建流执行环境
    StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
    DataStreamSource<String> testKafkaDS = env.fromSource(stringKafkaSource, WatermarkStrategy.noWatermarks(), "testKafkaDS");

    testKafkaDS.print();

    env.execute();
}
```

读取`Kafka`数据有三项属性必须配置：

-   **指定`Kafka`连接地址**

-   **指定消费的主题 / 分区：**配置主题和分区有三种方式

    -   **主题列表的方式：`KafkaSource.builder().setTopics("topic-a", "topic-b")`**

    -   **通过正则表达式匹配主题：`KafkaSource.builder().setTopicPattern("topic.*")`**

    -   **指定消费某个主题的某个分区：**

        ```Java
        final HashSet<TopicPartition> partitionSet = new HashSet<>(Arrays.asList(
                new TopicPartition("topic-a", 0),    // Partition 0 of topic "topic-a"
                new TopicPartition("topic-b", 5)));  // Partition 5 of topic "topic-b"
        KafkaSource.builder().setPartitions(partitionSet)
        ```

关于数据反序列化，如果只需要`value`的值，可以使用调用`setValueOnlyDeserializer()`方法；如果需要其他的信息，也可以调用`setDeserializer()`方法设置反序列化方式。

对于`Kafka`中数据消费的偏移量，可以通过调用`setStartingOffsets()`方法进行指定。

**方式二：使用`Flink Kafka Consumer`读取`Kafka`的数据**

```java
public static void main(String[] args) throws Exception {
    // 创建流执行环境
    StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

    Properties properties = new Properties();
    properties.setProperty("bootstrap.servers", "hadoop132:9092");
    properties.setProperty("group.id", "test");
    
    DataStream<String> stream = env
            .addSource(new FlinkKafkaConsumer<>("first", new SimpleStringSchema(), properties).setStartFromEarliest());

    stream.print();

    env.execute();
}
```



# 五、写出外部系统

## 5.1、写入到MySQL

将`Flink`数据写入到`MySQL`，需要添加以下依赖：

```xml
<!-- MySQL驱动 -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.21</version>
</dependency>
<dependency>
    <groupId>org.apache.flink</groupId>
    <artifactId>flink-connector-jdbc_2.11</artifactId>
    <version>1.13.6</version>
</dependency>
```

从`MySQL`中读取数据，进行处理后，再将数据写入到`MySQL`

```Java
public static void main(String[] args) throws Exception {
    // TODO 配置MySQL连接
    MySqlSource<String> mySqlSource = MySqlSource.<String>builder()
            .hostname("rm-2ze42x1o6er3410na.mysql.rds.aliyuncs.com")
            .port(3306)
            .databaseList("data_domain_test") // 设置捕获的数据库， 如果需要同步整个数据库，请将 tableList 设置为 ".*".
            .tableList("data_domain_test.test1_pqg") // 设置捕获的表
            .username("data_domain_test")
            .password("learnable@ANA")
            .deserializer(new JsonDebeziumDeserializationSchema()) // 将 SourceRecord 转换为 JSON 字符串
            .startupOptions(StartupOptions.initial())
            .build();

    // TODO 创建流执行环境
    StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
    env.enableCheckpointing(3000);

    // TODO 读取MySQL数据源，并行度必须设置为1，因为Flink CDC
    DataStreamSource<String> mysqlDS = env.fromSource(mySqlSource, WatermarkStrategy.noWatermarks(), "mysql_to_mysql");
    // 当前数据是一个JSON字符串，需要进行一定的处理，获取必要的字段
    SingleOutputStreamOperator<JSONObject> jsonDS = mysqlDS.map(
            new MapFunction<String, JSONObject>() {
                @Override
                public JSONObject map(String value) throws Exception {
                    JSONObject jsonObject = JSONObject.parseObject(value);

                    JSONObject source = jsonObject.getJSONObject("source");
                    source.remove("version");
                    source.remove("connector");
                    source.remove("name");
                    source.remove("ts_ms");
                    source.remove("snapshot");
                    source.remove("gtid");
                    source.remove("file");
                    source.remove("thread");

                    jsonObject.put("source", source);
                    return jsonObject;
                }
            }
    );

    // 将数据向下游发送
    jsonDS.addSink(
            JdbcSink.sink(
                    "INSERT INTO sink_test_pqg (db, tb, op, read_operator_ts_ms, before_data, after_data) VALUES (?, ?, ?, ?, ?, ?)",
                    new JdbcStatementBuilder<JSONObject>() {
                        @Override
                        public void accept(PreparedStatement preparedStatement, JSONObject jsonObject) throws SQLException {
                            preparedStatement.setString(1, jsonObject.getJSONObject("source").getString("db"));
                            preparedStatement.setString(2, jsonObject.getJSONObject("source").getString("table"));
                            preparedStatement.setString(3, jsonObject.getString("op"));
                            preparedStatement.setLong(4, jsonObject.getLong("ts_ms"));
                            preparedStatement.setString(5, jsonObject.getString("before"));
                            preparedStatement.setString(6, jsonObject.getString("after"));
                        }
                    },
                    JdbcExecutionOptions.builder()
                            .withBatchIntervalMs(200) // 批处理时间间隔，单位为毫秒，默认值为 0 ms
                            .withBatchSize(1000) // 批大小，单位：条，默认值为5000条
                            .withMaxRetries(5) // 重试次数，默认值为3次
                            .build(),
                    new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                            .withUrl("jdbc:mysql://rm-2ze42x1o6er3410na.mysql.rds.aliyuncs.com:3306/data_domain_test?serverTimezone=UTC")
                            .withDriverName("com.mysql.cj.jdbc.Driver")
                            .withUsername("data_domain_test")
                            .withPassword("learnable@ANA")
                            .build()
            )
    );

    mysqlDS.print("mysql_DS");
    jsonDS.print("json_DS");

    env.execute();
}
```

需要说明的是，`Flink JDBC`连接器将无限流数据写入到外部数据系统时，是以批的形式写入的，所以在调用`sink()`方法时，一定要传入`JdbcExecutionOptions`参数。

## 5.2、写入到Kafka

将数据写入到`Kafka`，所需要的依赖同上。

**将`Flink`中的流式数据写入到`Kafka first`主题中。**

```java
public static void main(String[] args) throws Exception {
    // 创建流执行环境
    StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

    // 开启检查点
    env.enableCheckpointing(3000);

    // 读取自定义数据源，并将数据直接写入到Kafka中
    DataStreamSource<WebClickEvent> webClickEventDS = env.addSource(new WebClickEventSource());
    SingleOutputStreamOperator<String> outDS = webClickEventDS.map(
            new MapFunction<WebClickEvent, String>() {
                @Override
                public String map(WebClickEvent value) throws Exception {
                    return value.toString();
                }
            }
    );

    // 将数据写入到Kafka
    Properties prop = new Properties();
    prop.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "hadoop132:9092");

    FlinkKafkaProducer<String> flinkKafkaProducer = new FlinkKafkaProducer<>("first", new SimpleStringSchema(), prop);

    outDS.addSink(flinkKafkaProducer);

    env.execute();
}
```

上述的将`Flink`流式数据写入到`Kafka`的方式有两点说明，一是所使用的序列化器`SimpleStringSchema`，这是一种无`key`的序列化器，只会将`value`数据进行序列化，并写入`Kafka`中。如果需要支持`key-value`类型的数据写入`Kafka`，那么需要使用`KafkaSerializationSchema`序列化器。另一个需要说明的是，以上的数据写入方式没有对数据一致性进行保证，`Flink`只管往`Kafka`主题中发，不管数据有没有发送成功，在一些场景下，需要保证数据的“至少一次”或者“精确一次”语义，那么需要对`FlinkKafkaProducer`进行配置，其中也需要使用`KafkaSerializationSchema`。

**要想实现数据发送过程中数据一致性语义，主要要配置的内容有以下四点：**

-   **`Flink Job`需要开启检查点**
-   **`FlinkKafkaProducer`的构造器必须使用带`Semantic`类型参数的构造器，通过该参数设置数据一致性级别**
-   **将`Kafka`读取数据的消费者隔离级别`isolation.level`设置为`read_committed`。该配置项的默认值是：`read_uncommitted`**
-   **事务超时时间配置。`FlinkKafkaProducer`中，默认的事务超时时间`transaction.timeout.ms`默认值为`1 h`，而`Kafka`集群默认配置的事务超时时间`transaction.max.timeout.ms`为`15 min`。需要将前者的值配置成小于等于后者**

**在精确一次语义下，将`Flink`中的流式数据写入到`Kafka first`主题中**

```Java
public static void main(String[] args) throws Exception {
    // 创建流执行环境
    StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

    // 开启检查点
    env.enableCheckpointing(3000);

    // 读取自定义数据源，并将数据直接写入到Kafka中
    DataStreamSource<WebClickEvent> webClickEventDS = env.addSource(new WebClickEventSource());
    SingleOutputStreamOperator<String> outDS = webClickEventDS.map(
            new MapFunction<WebClickEvent, String>() {
                @Override
                public String map(WebClickEvent value) throws Exception {
                    return value.toString();
                }
            }
    );

    Properties prop = new Properties();
    prop.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "hadoop132:9092");
    prop.put(ProducerConfig.TRANSACTION_TIMEOUT_CONFIG, 15 * 60 * 1000); // 配置Flink Kafka连接器的事务超时时间

    FlinkKafkaProducer<String> first = new FlinkKafkaProducer<>(
            "first",
            new KafkaSerializationSchema<String>() {

                @Override
                public ProducerRecord<byte[], byte[]> serialize(String element, @Nullable Long timestamp) {
                    return new ProducerRecord<byte[], byte[]>("first", element.getBytes(StandardCharsets.UTF_8));
                }
            },
            prop,
            FlinkKafkaProducer.Semantic.EXACTLY_ONCE // 设置数据一致性级别
    );

    outDS.addSink(first);

    env.execute();

}
```

