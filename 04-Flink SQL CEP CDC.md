<h1 align = "center">Flink SQL CDC CEP
</h1>

# 一、`Flink SQL`

尽管`Flink`是流数据处理框架，但为了更为方便的开发，`Flink`也提供了对数据流的“表”处理支持，这就是更高层级的应用`API`，即`Flink Table API`和`Flink SQL`。

`Flink Table API`是一种基于表的`API`，这套`API`是内嵌在`Java`、`Scala`等语言中的一种声明式领域特定语言，也就是专门为处理表而设计的。在此基础上，`Flink`还基于`Apache Calcite`实现了对`SQL`的支持，因此，通过`Flink SQL`，开发者可以直接在`Flink`中写`Flink SQL`代码来实现业务需求。

![image-20230509151851740](./04-Flink SQL CEP CDC.assets/image-20230509151851740.png)

类似于`Flink DataStream API`是基于`DataStream`进行数据转换和处理，`Flink SQL`是基于数据表进行数据处理和转换，在使用过程中，通过`Flink`获得数据表，然后对表调用相应的`Table API`或者写`Flink SQL`，就能够实现数据的处理和转换。

但区别于`Flink DataStrem API`的是，`Flink Table API`或`Flink SQL`对于数据表的输入和输出定义没有严格的顺序，在数据输出时，使用哪张表，哪张表就是输出表；在数据读入时，使用哪张表，哪张表就是读入表。

>   **在代码中使用`Flink Table API`或者`Flink SQL`时需要引入相关的依赖**
>
>   -   **桥接器：主要负责`Table API`和底层`DataStream API`的连接，根据开发语言的不同，分为`Java`版本和`Scala`版本**
>
>       ```xml
>       <dependency>
>           <groupId>org.apache.flink</groupId>
>           <artifactId>flink-table-api-java-bridge_${scala.binary.version}</artifactId>
>           <version>${flink.version}</version>
>       </dependency>
>       ```
>
>   -   **计划器：是`Table API`的核心组件，负责提供运行时环境，并生成程序的执行计划。`Flink`安装目录下的`lib`目录自带计划器，在`IDE`中引入是为了便于在本地`IDE`中使用并运行`Flink Table API`和`FlinK SQL`。由于`Flink`计划器中有部分代码由`Scala`语言实现，因此还需要引入一个`Scala`版本的流处理依赖**
>
>       ```xml
>       <dependency>
>           <groupId>org.apache.flink</groupId>
>           <artifactId>flink-table-planner-blink_2.12</artifactId>
>           <version>${flink.version}</version>
>       </dependency>
>       <dependency>
>           <groupId>org.apache.flink</groupId>
>           <artifactId>flink-streaming-scala_${scala.binary.version}</artifactId>
>           <version>${flink.version}</version>
>       </dependency>
>       ```
>
>   -   **为了能够实现自定义数据格式的序列化，需要引入以下依赖**
>
>       ```xml
>       <dependency>
>           <groupId>org.apache.flink</groupId>
>           <artifactId>flink-table-common</artifactId>
>           <version>${flink.version}</version>
>       </dependency>
>       ```

## 1.1、创建表执行环境

对于实时流数据处理而言，数据流和表在结构上是有所区别的，使用`Flink Table API`和`Flink SQL`需要一个特别的运行时环境，即表环境`TableEnvironment`。

表环境主要负责：

-   注册`Catalog`和表
-   执行`SQL`查询
-   注册用户自定义函数`UDF`
-   `DataStream`和表之间的转换

其中`Catalog`主要用于管理所有数据库和表的元数据，通过`Catalog`，可以方便地对数据库和表进行查询管理。在表环境中，开发者可以由用户自定义`Catalog`，并在其中注册表和自定义函数，默认的`Catalog`名字是`default_catalog`。

`TableEnvironment`是`Table API`中提供的基本接口类，有一个子接口`StreamTableEnvironment`。

使用`TableEnvironment`创建表执行环境实例时，需要调用接口的静态方法`create()`，传入`EnvironmentSettings`类型的参数即可。`EnvironmentSettings`类用于配置当前表执行环境的执行模式和计划器，其内部实现使用了建造者设计模式。执行模式有批执行模式，有流执行模式，配置时，需分别调用静态内部类`Builder`的`inBatchMode()`方法和`inStreamingMode()`方法，默认情况下使用的是流执行模式。计划器有旧版计划器和新版的`Blink`计划器可以使用，默认情况下使用`Blink`计划器。

```Java
// 对环境进行配置
EnvironmentSettings build = EnvironmentSettings.newInstance()
        .inBatchMode()     // 创建批处理表执行环境
        .inStreamingMode() // 创建表处理执行环境
        .useOldPlanner()   // 使用旧版计划器，该方法已经过时
        .useAnyPlanner()   // 不显示设置计划器，默认情况使用Blink计划器
        .useBlinkPlanner() // 显示指定Blink计划器
        .build();
```

区别于`TableEnvironment`可以创建具有不同的执行模式和不同计划器的表执行环境，`StreamTableTableEnvironment`只能创建基于新版`Blink`计划器的流式执行环境，其创建方式与`TableEnvironment`创建表执行环境相同，都是调用其静态方法`create()`，传入`EnvironmentSettings`类型的参数即可。

除此之外，`StreamTableEnvironment`还能基于流式执行环境`StreamExecutionEnvironment`创建表执行环境。

**表执行环境创建：**

```Java
/**
 * @author shaco
 * @create 2023-05-09 16:25
 * @desc 创建表执行环境
 */
public class C029_CreateTableExecutionEnvironment {
    public static void main(String[] args) {
        // 1、使用TableEnvironment创建表执行环境
        // 对环境进行配置
        EnvironmentSettings build = EnvironmentSettings.newInstance()
                .inBatchMode()     // 创建批处理表执行环境
                .inStreamingMode() // 创建表处理执行环境
                .useOldPlanner()   // 使用旧版计划器，该方法已经过时
                .useAnyPlanner()   // 不显示设置计划器，默认情况使用Blink计划器
                .useBlinkPlanner() // 显示指定Blink计划器
                .build();

        TableEnvironment tableEnvironment = TableEnvironment.create(build);
        System.out.println(tableEnvironment);

        // 2、使用StreamTableEnvironment创建表执行环境
        // 先创建流执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        StreamTableEnvironment streamTableEnvironment = StreamTableEnvironment.create(env, build);
        System.out.println(streamTableEnvironment);
    }
}
```

## 1.2、在表执行环境中创建表

表是关系型数据库中数据存储的基本形式，也是`SQL`执行的基本对象。`Flink`中表的概念也并不特殊，是由多个行`Row`数据构成，每个行又具有定义好的多个列。因此表就是固定类型的数据构成的二维矩阵。

在`Flink`中，为了对表进行管理，表环境中会维护一个`Catalog`，所有的表都是通过`Catalog`来进行注册创建的。表在环境中有唯一的`ID`，由三部分构成：`Catalog`名、数据库名、以及表名。默认情况下，`Catalog`名为`default_catalog`，数据库名为`default_database`，所以如果创建一个名为`test_tb`的表，那么它的`ID`为`default_catalog.default_database.test_tb`。

具体表的创建有两种方式。

-   **通过连接器在表执行环境中注册表：**

    通过连接器连接到外部系统，然后定义出对应的表结构，是最为直接的创建表的方式。当在表执行环境中读取定义好的表时，连接器就会从外部系统读取数据，并进行转换；而当向定义好的表中写入数据时，连接器也会将数据输出到外部系统中。

    在实际的代码操作中，需要使用表执行环境对象`tableEnv`调用`executeSql()`方法，传入字符串类型的表的`DDL`语句作为参数，就能在表环境中创建表。需要注意的是，区别于离线`SQL`的`DDL`，在`Flink`实时`SQL`中，其`DDL`语言中还需要通过`WITH`关键字指定连接到外部系统的连接器。

    **通过连接器创建一张名为`test_tb1`的表：**

    ```Java
    tableEnv.executeSql("create table test_tb1 ... with ('connector' = ...)")
    ```

    上述声明，没有定义`Catalog`和`Database`，因此取值都是默认的，表的完整`id`是：`default_catalog.default_database.test_tb1`。

    如果需要使用自定义的`Catalog`名和数据库名，可以在表执行环境中调用相应的方法，设置`Catalog`名和数据库名。设置后，在表环境中创建的所有表，其`Catalog`名和数据库名都将变成开发者设置的名称。

    ```Java
    tableEnv.useCatalog("custom_catalog");
    tableEnv.useDatabase("custome_database");
    ```

-   **使用虚拟表向表环境中注册表：**

    通过连接器的方式在表执行环境中注册表之后，就可以直接将表名用于`SQL`语句中，但执行`SQL`得到的结果是`Table`类型，是`Java`中的一个类，如果想要在后续直接使用得到的`Table`，那么就需要在表执行环境中注册这张表。

    使用表执行环境`tableEnv`调用`createTemporaryView()`方法，将字符串类型的需要注册的表名和`Table`类型的实例，作为参数传入，即可在表执行环境注册这张表，随后，便可以在后续的`SQL`中直接使用。

表的注册其实质是创建了一个虚拟表，与`SQL`语法中的视图非常相似，这张虚拟表并不会直接保存数据，只会在用到这张表的时候，会将其对应的查询语句嵌入到`SQL`中。

## 1.3、表查询

`Flink`表数据查询对应着流数据的转换，在`Flink`中有两种表数据查询方式，`SQL`查询和`Table API`查询。其中`Table API`的数据查询需要借用`Expression`，使用其作为数据查询造作语言并不方便，但完全使用`SQL`查询也是基于`Table`数据类型的，因此在表数据查询中，往往都是`SQL`查询和`Table API`结合使用，`SQL`查询用于数据查询，`Table API`用于对`Table`类型和`DataStream`类型之间的转换，以及向表环境中注册表。

使用`SQL`查询，需要使用表执行环境`tableEnv`调用`sqlQuery()`，将字符串类型的`SQL`查询语句作为参数传入，返回结果为`Table`。`Flink 1.13`版本支持标准`SQL`中绝大部分的用法，也提供了丰富的计算函数。

**简单的分组聚合**

```java
Table resTable = tableEnv.sqlQuery("select user, count(url) from mytable group by user");
```

除了基于表进行数据查询，还可向表执行环境中已注册的表中写入数据。使用表执行环境`tableEnv`调用`executeSql()`方法，传入`Insert`类型的`DDL`语句，返回结果为`TableResult`。

**==需要注意的是，无法直接插入具体的数据，只能将查询得到的结果再写入到指定的表中。例如：==**

```java
tableEnv.executeSql(
	" insert into outputTable " + 
    " select user, url " + 
    " from inputTable " + 
    " where user = 'Bob' "
);
```

无论是哪种方式得到的`Table`类型对象，都可以继续调用`Table API`进行数据查询和转换。但如果需要对表进行`SQL`查询操作，那么必须先在表执行环境中进行表注册。使用表执行环境`tableEnv`调用`createTemporaryView()`方法能够在表环境中对`Table`进行注册。

```java
tableEnv.createTemporaryView("table_name", myTable);
```

其中`table_name`是在表执行环境中注册的表名，可以在`SQL`中直接调用，而`myTable`即是`Java`中`Table`对象。

## 1.4、表数据输出

表的创建和查询，对应着流处理中的读取数据源`Source`和转换`Transform`，流处理中的`Sink`操作，对应表的输出操作。

在代码实现中，输出一张表最直接的方法是基于`Table`调用`executeInsert()`方法将`Table`中的数据写入到表执行环境中已注册的表中，其参数即为表执行环境中注册的表名。

```java
Table result = ...
result.executeInsert("table_name");
```

在底层实现中，表的输出是通过将数据写入到`TableSink`来实现的，`TableSink`是`Table API`中提供的一个用于向外部系统写入数据的通用接口，可以支持不同的文件格式，例如`CSV`、`Parquet`；不同的数据库系统，例如`JDBC`、`HBase`；以及消息队列，例如`Kafka`。在表执行环境中注册的表，在写入数据的时候就对应着一个`TableSink`。

## 1.5、`Table`与`DataStream`之间的相互转换

**==需要注意的是，`Table`与`DataStream`之间的相互转换，只能基于流式表执行环境，因为对于批处理模式，无法直接转换成`DataStream`。==**

### 1.5.1 **`Table`转换为`DataStream`**

最简单的转换方式是基于表执行环境，调用`toDataStream()`方法，传入`Table`类型参数，返回值即为`DataStream`。

```Java
DataStream myDataStream = tableEnv.toDataStream(myTable, MyDataType.class);
```

**==需要注意的是，调用`toDataStream()`进行`Table`转`DataStream`，只适合一般的“仅插入流”，即数据流中的数据只能增，不能减。==**

为了解决由于分组聚合产生等操作产生的数据更新，需要基于表执行环境`tableEnv`调用`toChangelogStream()`方法，将`Table`转换成“更新日志流”。其原理是记录每一条数据的更新日志，将其更新日志进行打印输出。

```java
DataStream<Row> myDataStream = tableEnv.toChangelogStream(myTable);
```

### 1.5.2 `DataStream`转换成`Table`

最简单的将`DataStream`转换成`Table`的方式是，直接基于表执行环境`tableEnv`调用`fromDataStream()`方法，将`DataStream`作为参数传入，返回结果即是一个`Table`。

```java
Table myTable = tableEnv.fromDataStream(myDataStream);
```

在这里需要注意的是，将`DataStream`转换成`Table`时，数据类型的问题。在流中，数据类型由`Flink`支持的数据类型决定，因此，各种各样的数据类型映射到`Table`中之后会有着不同的数据类型呈现。关于数据类型的说明，将在后面进行叙述。

除此之外，对于更新日志流，也可将其转换成`Table`，基于表执行环境`tableEnv`调用`fromChangelogStream()`方法，将更新日至六作为参数传入，即可得到一个`Table`。需要注意的是，只能将更新日志流中数据类型为`Row`的数据流转换成`Table`，并且流中的每一条数据都需要指定当前行的`RowKind`，开发者直接使用比较少，一般都由连接器进行实现。

### 1.5.3 表环境中注册表

无论通过什么方式获得的`Table`，如果需要在`Flink SQL`中直接使用，那么需要在表执行环境中进行注册。

基于表执行环境`tableEnv`调用`createTemporaryView()`方法，传入需要在表环境中注册的表名和`Table`对象，即可在表环境中注册相应的表。

除此之外，还可以直接将`DataStream`在表环境中进行注册，同样是调用`createTemporaryView()`方法，不过，此时需要将`DataStream`对象作为参数传入。

## 1.6、`Table`支持的数据类型

整体来看，`DataStream`中支持的数据类型，`Table`中也都是支持的，只不过，在进行转换的时候需要注意一些细节。

-   **原子类型：**在`Flink`中，基本数据类型的包装类，和不可拆分的通用数据类型统一称为原子类型。原子类型的`DataStream`转换成`Table`之后，就是只有一列的表，其字段的数据类型可以由原子类型进行自动推断
-   **`Tuple`类型：**`Table`支持`Flink`中定义的元组类型`Tuple`，对应在表中字段名默认为就是元组中元素的属性名，即`f0`、`f1`、`f2`等。
-   **`POJO`类型：**`POJO`中已经定义了可读性很强的字段名，所以，在将`POJO`类型的`DataStream`转换成`Table`时，默认情况下，会直接使用原始`POJO`中，的字段名称。
-   **`Row`类型：**`Flink`中还定义了一个在关系型表中更加通用的数据类型`Row`，它是`Table`中数据的基本组织形式。`Row`类型也是一种符合类型，其长度固定，并且无法直接推断出每个字段的类型。所以在使用时必须指定具体的类型。除此之外，`Row`类型还需要附加一个属性`RowKind`，用于指定当前行在更新操作中的类型，这样，`Row`就可以用来表示更新日志流中的数据。

**需要注意的是，在将`DataStream`转换成`Table`的过程中，可以对数据的字段进行筛选、重新排序、以及重命名。**