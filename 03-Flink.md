<h1 align = "center">Flink
</h1>

# 一、Flink简介

# 二、Flink运行模式及部署模式

## 1、运行模式

### 1.1 Local模式（本地模式）

**Local模式部署非常简单，直接下载并解压Flink安装包即可，不用行进额外的配置。**

**本地模式启动命令：`start-cluster.sh`**

**本地模式停止命令：`stop-cluster.sh`**

**Flink集群和任务监控地址：JobManager所在节点的ip地址：8081**

### 1.2 Standalone模式

**区别于Local模式，Standalone模式需要修改配置文件，配置集群JobManager节点和TaskManager节点。**

**JobManager节点配置（配置文件及配置项）：./flink/conf/fink-conf.yaml -> jobmanager.rpc.address:\<JobManager节点所在机器的ip地址\>**

**TaskManager节点配置（配置文件及配置项）：./flink/conf/workers -> \<TaskManager节点的ip地址>**

**Standalone模式集群启动命令：`start-cluster.sh`**

**Standalone模式集群停止命令：`stop-cluster.sh`**

---

**flink-conf.yaml文件主要配置项：**

-   **`jobmanager.memory.process.size：JobManager进程可使用的全部内存，或者说是JobManager进程能使用的最大内存`**
-   **`taskmanager.memory.process.size：TaskManager进程可使用的全部内存，或者说是TaskManager进程能使用的最大内存`**
-   **`taskmanager.numberOfTaskSlots：用于设置每个TaskManager能够提供的task slot的数量`**
-   **`parallelism.default：Flink任务执行的默认并行度`**

---

>**Flink集群作业提交步骤**
>
>**作业提交需要将Flink程序提前打包**
>
>**1、Web UI提交作业**
>
>-   **Submit New Job -> Add New：选择jar包，并上传**
>-   **点击jar包，进行Flink任务配置：① 程序入口主类的全类名；② 任务运行的并行度；③ 任务运行所需要的配置参数和保存点路径等**
>-   **Submit：将任务提交到集群运行**
>-   **任务提交成功之后，左侧导航栏 -> Running Jobs：查看程序运行列表**
>-   **点击提交成功的任务：查看任务运行的具体情况，并能通过点击Cannel Job结束任务运行**
>
>**2、命令行提交作业**
>
>-   **任务提交命令：./bin/flink/ run -m \<JobManager节点ip:8081> -c \<Flink程序的入口类\> \<Flink程序jar包\>** 

### 1.3 YARN模式

**Flink集群的YARN运行模式，需要提前安装Hadoop服务，包括YARN和HDFS，并且版本必须至少在2.2以上。**

**YRAN配置步骤：**

-   **下载并解压安装Flink集群**

-   **配置环境变量**

    ```txt
    export HADOOP_HOME=/opt/moudule/hadoop-3.1.3-ha
    export $PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin
    export HADOOP_CONF_DIR=${HADOOP_HOME}/etc/hadoop
    export HADOOP_CLASSPATH=`hadoop classpath`
    ```

### 1.4 K8S模式

**略**

>**==一段Flink代码就是一个应用。==**
>
>**==在一个应用中可以存在多个Job，一个Job由流式执行环境、Sink算子、数据处理操作、source算子、流式数据处理执行。==**
>
>**==一个Job包含多个Flink算子，一个算子即是一个任务==**
>
>**==一个算子由于并行度的属性，所以一个算子可以有很多并行子任务。==**

## 2、部署模式

**为满足不同场景中，集群资源分配和占用方式的需求，Flink除了具有不同运行模式，还有不同的部署模式。这些模式的主要区别在于：集群的生命周期以及资源的分配方式，以及Flink应用中main方法到底在哪里执行：Client还是JobManager。**

### 2.1 会话模式（Session Mode）

**会话模式最为符合常规思维：先启动一个集群，保持一个会话，在这个会话中通过Client提交作业。**

**这样做的好处是，我们只需要一个集群，所有的作业提交之后都放到这个集群中去运行，集群的生命周期是超越作业的，作业执行结束厚，就释放资源，集群然运行。**

**其确定也是显而易见的，因为资源是共享的，所以当资源不够时，新提交的作业就会失败。此外，如果一个发生故障导致TaskManager宕机，那么所有作业都会收到影响。**

### 2.2 单作业模式（Per-Job Mode）

**会话模式会因为资源共享导致很多问题，所以为了隔离每个作业所需要的资源，Flink还有部署模式：单作业模式。**

**单作业模式中，为每个提交的作业创建一个集群，由客户端执行main()方法，解析出DataFlowGraph和JobGraph，然后启动集群，并将解析出来的作业提交给JobManager，进而分发给TaskManager执行。作业执行完成后，集群就会关闭，该集群的资源就会释放出来。**

**单作业模式中，每个作业都有自己的JobManager管理，占用独享的资源，即使发生故障，也不会影响其他作业的运行。**

**需要注意的是，Flink本身无法直接这样运行，所以单作业模式一般都需要借助一些资源调度框架来启动集群，如，YARN、Kubernetes等。**

### 2.3 应用模式（Application Mode）

**会话模式和单作业模式下，应用代码都是在Client中执行，然后将执行的二进制数据和相关依赖提交给JobManager。这种方式存在的问题是，Client需要占用大量的网络带宽，去下载依赖和将二进制数据发送给JobManager，并且很多情况下提交作业用的都是同一个Client，这样就会加重Client所在节点的资源消耗。**

**Flink解决这个问题的思路就是：把产生问题的组件干掉，把Client干掉，直接把应用提交到JobManager上运行。除此之外，应用模式与单作业模式没有区别，都是提交作业之后才创建集群，单作业模式使用Client执行代码并提交任务，应用模式直接由JobManager执行应用程序，即使应用包含多个作业，也只创建一个集群。**

### 2.4 高可用模式

**对于Flink而言，一般会存在多个TaskManager，当TaskManager出现故障时，无需将所有节点进行重启，只需要尝试重启故障节点就可以了。但是用于管理TaskManager的JobManager却只有一个，因此存在单点故障的问题。因此为了更好的可靠性，我们可以对JobManager进行高可用配置。**

## 3、运行模式下的部署模式

### 3.1 standalone运行模式下的部署模式

#### 3.1.1 standalone运行模式下的会话模式

**standalone运行模式下的会话模式，需要先启动集群，将资源都准备好，随后提交作业。**

**默认情况下，使用`start-cluster.sh`命令启动的standalone集群即为会话模式集群。**

#### 3.1.2 standalone运行模式下的单作业模式

**Flink本身无法直接运行Standalone运行模式下的单作业模式，一般需要借助资源调度框架。**

#### 3.1.3 standalone运行模式下的应用模式

**应用模式下不会提前创建集群，所以不能调用`start-cluster.sh`脚本。**

**创建standalone运行模式下的应用模式的步骤：**

-   **将Flink应用的jar包放到Flink安装包下的`lib/`目录下**
-   **执行`./bin/standalone-job.sh start --job-classname <应用的入口类>`命令，启动JobManager**
-   **执行`./bin/taskmanager.sh start`命令，启动TaskManager**
-   **使用`./bin/standalone-job.sh stop`命令停止JobManager，使用`./bin/taskmanager.sh stop`命令停止TaskManager**

#### 3.1.4 standalone运行模式下的高可用模式

**通过配置，可以让集群在任何时候都有一个主JobManager和多个备用JobManager，当主节点的JobManager故障时，可以有备用节点的JobManager来接管集群。**

**配置：**

-   **修改flink-conf.yaml配置文件，增加配置，完成后分发配置文件**

    ```txt
    high-availability:zookeeper
    high-availability.storageDir:hdfs://hadoop132:9820/flink/standalone/ha
    high-availability.zookeeper.quorum:hadoop132:2181,hadoop133:2181,hadoop134:2181
    high-availability.zookeeper.path.root:/flink-standalone
    high-availability.cluster-id:/cluster_standalone_flink
    ```
    
-   **修改masters配置文件，配置JobManager列表，完成后，分发配置文件**

    ```txt
    hadoop132:8081
    hadoop133:8081
    ```

-   **配置环境变量，HADOOP_HOME环境变量必须配置成功**

    ```txt
    # /etc/profile.d/my_env.sh
    export HADOOP_CLASSPATH=`hadoop classpath`
    ```

**集群启动步骤：**

-   **启动Zookeeper集群和HDFS集群**

-   **执行命令，启动standalone HA集群**

    ```txt
    bin/start-cluster.sh
    ```

-   **可以访问两个备用JobManager的Web UI页面**

    ```txt
    http://hadoop102:8081
    http://hadoop103:8081
    ```

-   **在Zookeeper客户端中查看JobManager的leader**

**==需要注意的时，不管是不是leader，从Web UI上都看不到区别，都可以提交应用。==**

### 3.2 YARN运行模式下的部署模式

**YARN运行模式下的Flink任务提交过程可以概括为：Client把Flink应用提交给YARN的ResourceManager，随后YARN的ResourceManager向YARN的NodeManager申请Contain容器，用于Flink部署JobManager和TaskManager实例，从而启动Flink集群。Flink集群会根据运行在JobManager上作业所需要的Slot数量，动态分配TaskManager资源。**

**在Flink 1.8.0版本之前，想要运行YARN模式的Flink集群，需要Flink有Hadoop支持。从Flink 1.8.0版本开始，不再提供基于Hadoop编译的安装包，如果需要Hadoop环境支持，需要自行在官网上下载Hadoop相关本本的组件flink-shaded-hadoop-2-uber-2.7.5-10.0.jar ，并放在Flink的lib目录下。在Flink 1.11.0版本之后，增加了很多重要的新特性，其中就包括增加了对Hadoop 3.0.0及以上版本的Hadoop支持，不再提供”flink-shaded-hadoop-xxx“jar包，而是通过配置环境变量完成与YARN框架的对接。**

**在创建YARN运行模式的Flink集群之前，需要保证节点有安装Hadoop 2.2，并且安装了HDFS服务。**

**Flink的YARN运行模式的关键是配置环境变量，需要注意的是，一定要配置HADOOP_CLASSPATH的环境变量**

```txt
# 配置环境变量
# /etc/profile.d/my_env.sh
export HADOOP_HOME=/opt/moudule/hadoop-3.1.3-ha
export PATH=$PATH:$HADOOP_HOME/bin:$PATH:$HADOOP_HOME:/sbin
export HADOOP_CONF_DIR=$PATH:${HADOOP_HOME}/etc/hadoop
export HADOOP_CLASSPATH=`hadoop classpath`
```

#### 3.2.1 YARN运行模式下的会话模式

**YARN运行模式下的会话模式与Standalone运行模式下的会话模式有所不同，YARN运行模式下的会话模式需要先启动一个YARN会话，随后再启动Flink集群。**

**集群启动：**

-   **启动Hadoop集群**

    ```txt
    start-dfs.sh
    start-yarn.sh
    ```

-   **向YARN服务申请资源，开启一个YARN会话。当YARN会话开启后，其实Flink集群就已经启动，YARN运行模式下的会话模式就已经开启**

    ```txt
    /bin/yarn-session.sh -nm <YARN任务的名称>
    
    # yarn-session.sh脚本常用参数
    -d							分离模式，将YARN会话置于后台运行，不让其占用窗口
    -jm(--jobManagerMemory)		配置JobManager所需要的内存，单位为MB
    --tm(--taskManager)			配置每个TaskManager所使用的内存，单位为MB
    --nm(--name)				配置在YARN UI界面上显示的任务名称
    --qu(--queue)				指定YARN队列名
    ```

**YARN会话启动后，会给出任务Web UI地址和YARN application ID，用于监控YRAN会话任务的运行状态。**

**提交作业：**

-   **通过Web UI提交作业，操作与Local模式相同，不再赘述。**

-   **通过命令行提交作业**

    ```txt
    ① 将Flink作业打成jar包，并上传到Flink集群中（任意一台节点的lib目录下）
    ② 执行命令将任务提交到Flink YARN-Session中运行
    	bin/flink run -c <应用的主类名> <jar包名>
    ```

-   **通过YARN Web UI查看任务的运行情况。可以看到YARN-Session实际上是YARN的一个Application**

-   **也可以通过Flink的Web UI页面查看任务的运行情况**

#### 3.2.2 YARN运行模式下的单作业模式

**在YAN环境中，由于有了外部平台做资源调度，所以我们也可以直接向YARN提交一个单独的作业，从而启动Flink集群。**

**区别与YARN运行模式下的会话模式，YARN运行模式下的单作业模式不需要提前开启一个YARN-Session，直接提交作业即可。**

**集群启动及任务提交：**

-   **执行命令提交作业**

    ```txt
    bin/flink run -d -t yarn-per-job -c <应用的主类名> <jar包名>
    ```

-   **执行命令查看作业**

    ```txt
    bin/flink list -t yarn-per-job -Dyarn.application.id=<应用id>
    ```

-   **执行命令取消作业**

    ```txt
    bin/flink cancel -t yarn-per-job -Dyarn.application.id=<应用id或作业id>
    ```

**==需要注意的是：==**

**一个Flink作业中可以包含多个Flink应用，即一套代码包含多条处理链路，那么每一条处理链路都算作是一个应用。在一个应用中会包含多个算子，并且算子还会有并行度的配置。**

#### 3.2.3 YARN运行模式下的应用模式

**与单作业模式相似，不需要提前开启一个YARN-Session，直接提交任务即可。**

**集群启动及任务提交：**

-   **执行命令提交作业**

    ```txt
    bin/flink run-application -t yarn-application -c <应用的主类名> <jar包名>
    ```

-   **在命令行中查看作业**

    ```txt
    bin/flink list -t yarn-application -Dyarn.application.id=<应用id>
    ```

-   **在命令行中取消作业**

    ```txt
    bin/flink cancel -t yarn-application -Dyarn.application.id=<应用id或作业id>
    ```

**在提交作业的时候，可以通过yarn.provided.lib.dirs配置项指定位置，提前将jar包上传到HDFS，从而不需要单独发送集群，这就使得作业提交更加轻量。**

```txt
/bin/flink run-application -t yarn-application -Dyarn.provided.lib.dirs="<hdfs:myhdfs/my-remote-flink-dist-dir>" hdfs://myhdfs/jars/my-application.jar
```

#### 3.2.5 YARN运行模式下的高可用模式

**YARN运行模式下的高可用模式与standalone运行模式下的高可用模式不同。**

**在standalone模式的高可用模式中，同时启动多个JobManager，其中一个为leader，对外提供服务，其他为standby，当leader挂了，其他JobManager上位成为leader，继续对外提供服务。**

**而YARN运行模式的高可用模式中，只启动一个JobManager，当这个JobManager失败后，YARN会再次启动一个JobManager，换句话说，是利用YARN的重试机制来实现高可用。**

**配置：**

-   **在yarn-site.xml中进行配置，配置完进行集群分发**

    ```xml
    <property>
    	<name>yarn.resourcemanager.am.max-attempts</name>
    	<value>4</value>
    	<description>The maximum number of application master execution attempts.</description>
    </property>
    ```
    
-   **在flink-conf.yaml中配置，配置完进行集群分发**

    ```txt
    yarn.application-attempts: 3
    high-availability: zookeeper
    high-availability.storageDir: hdfs://hadoop102:9820/flink/yarn/ha
    high-availability.zookeeper.quorum:
    hadoop102:2181,hadoop103:2181,hadoop104:2181
    high-availability.zookeeper.path.root: /flink-yarn
    ```

-   **启动Hadoop集群**

    ```txt
    start-dfs.sh
    start-yarn.sh
    ```

-   **启动Flink集群，可以按照不同的部署模式启动**

### **3.3 K8S模式**

**略**

