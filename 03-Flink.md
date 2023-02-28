<h1 align = "center">Flink
</h1>

# 一、Flink简介

# 二、Flink部署和运行模式

在不同的部署模式下，Flink各组件的启动以及资源获取的方式都有所不同，为此Flink提供了三种不同的部署模式。而在不同的运行环境下，Flink的资源调度和服务启停也都有所不同，Flink根据不同的场景也提供了不同运行模式。

# 1、部署模式

为满足不同场景中，集群资源分配和占用方式的需求，Flink提供了不同的部署模式。这些模式的主要区别在于：集群的生命周期以及资源的分配方式，以及Flink应用中main方法到底在哪里执行：Client还是JobManager。

>   **==一段Flink代码就是一个应用（Application）。==**
>
>   **==在一个Application中可以存在多个作业（Job），一个Job由流式执行环境、Sink算子、数据处理操作、Source算子、流式数据处理执行。==**
>
>   **==一个Job包含多个Flink算子，一个算子即是一个任务（Task）==**
>
>   **==一个算子由于并行度的属性，所以一个算子可以有很多并行子任务。==**

### 1.1 会话模式（Session Mode）

会话模式最为符合常规思维：先启动一个Flink集群，保持一个会话，在这个会话中通过Client提交Application，进而提交Job。

这样做的好处是，我们只需要一个集群，所有的Job提交之后都放到这个集群中去运行，集群的生命周期是超越Job的，Job执行结束后，就释放资源，集群依然运行。

其缺点也是显而易见的，因为资源是共享的，所以当资源不够时，新提交的Job就会失败。此外，如果一个发生故障导致TaskManager宕机，那么所有Job都会受到影响。

### 1.2 单作业模式（Per-Job Mode）

会话模式会因为资源共享导致很多问题，所以为了隔离每个Job所需要的资源，Flink还提供了单作业模式。

单作业模式中，为每个提交的Job创建一个集群，由客户端执行main()方法，解析出DataFlowGraph和JobGraph，然后启动集群，并将解析出来的Job提交给JobManager，进而分发给TaskManager执行。Job 执行完成后，集群就会关闭，该集群的资源就会释放出来。

单作业模式中，每个Job都有自己的JobManager管理，占用独享的资源，即使发生故障，也不会影响其他作业的运行。

需要注意的是，Flink本身无法直接这样运行，所以单作业模式一般都需要借助一些资源调度框架来启动集群，如，YARN、Kubernetes等。

### 1.3 应用模式（Application Mode）

会话模式和单作业模式下，应用代码都是在Client中执行，然后将执行的二进制数据和相关依赖提交给JobManager。这种方式存在的问题是，Client需要占用大量的网络带宽，去下载依赖和将二进制数据发送给JobManager，并且很多情况下提交Job用的都是同一个Client，这样就会加重Client所在节点的资源消耗。

Flink解决这个问题的思路就是：把产生问题的组件干掉，把Client干掉，直接把Application提交到JobManager上运行，进而解析出DataGraph和JobGraph。除此之外，应用模式与单作业模式没有区别，都是提交Job之后才创建集群，单作业模式使用Client执行代码并提交Job，应用模式直接由JobManager执行应用程序，即使Application包含多个Job，也只创建一个集群。

## 2、运行模式

==**本文档所使用Flink版本为Flink 1.13**==

### 1.1 Local模式（本地模式）

Local模式部署非常简单，直接下载并解压Flink安装包即可，不用行进额外的配置。因此，Local模式下，Flink的数据均存储在本地。

**Local模式的部署只需要一台节点，以下以Hadoop132节点为例，介绍部署步骤：**

-   **使用xftp工具将Flink压缩包flink-1.13.0-bin-scala_2.12.tgz上传到/opt/software目录下**

-   **解压到/opt/moudle目录下：`tar -zxvf /opt/software/flink-1.13.0-bin-scala_2.12.tgz -C /opt/module/`**

-   **对flink解压包进行重命名，添加-local后缀，表示Local运行模式：`mv /opt/software/flink-1.13.0/ /opt/module/flink-1.13.0-local`**

-   **配置环境变量：`vim /etc/profile.d/my_env.sh`**

    ```txt
    #FLINK_HOME
    export FLINK_HOME=/opt/module/flink-1.13.0-local
    export PATH=$PATH:$FLINK_HOME/bin
    ```

-   **执行文件，让环境变量生效：`source /etc/profile`**

-   **执行命令，启动Flink Local模式：`start-cluster.sh`。`start-cluster.sh`脚本将会依次启动以下服务：**

    ```bash
    [justlancer@hadoop132 ~]$ start-cluster.sh 
    Starting cluster.
    Starting standalonesession daemon on host hadoop132.
    Starting taskexecutor daemon on host hadoop132.
    ```

-   **hadoop132节点此时应该运行的服务有：**

    ```txt
    ============== hadoop132 =================
    1971 Jps
    1594 StandaloneSessionClusterEntrypoint
    1871 TaskManagerRunner
    ============== hadoop133 =================
    1142 Jps
    ============== hadoop134 =================
    1140 Jps
    ```

-   **此时访问`hadoop132:8081`可以对Flink进行监控和任务提交**![image-20230227142736371](C:\Users\28645\AppData\Roaming\Typora\typora-user-images\image-20230227142736371.png)

-   **执行命令，停止Flink Local模式：`stop-cluster.sh`**

### 1.2 Standalone模式（独立部署模式）

Standalone模式是一种独立运行的集群模式，这种模式下，Flink不依赖任何外部的资源管理平台，集群的资源调度、数据处理、容错机制和一致性检查点等都由集群自己管理。Standalone模式的优点是，不需要任何外部组件，缺点也很明显，当集群资源不足或者出现故障，由于没有故障自动转移和资源自动调配，需要手动处理，会导致Flink任务失败。

区别于Local模式，Standalone模式需要进行集群配置，配置集群JobManager节点和TaskManager节点。

Flink集群规划

|  hadoop132  |  hadoop133  |  hadoop134  |
| :---------: | :---------: | :---------: |
| JobManager  |    **—**    |    **—**    |
| TaskManager | TaskManager | TaskManager |

-   **再解压一份Flink解压包：`tar -zxvf /opt/software/flink-1.13.0-bin-scala_2.12.tgz -C /opt/module/`**

-   **对Flink解压包进行重命名，添加-standalone后缀，表示Standalone运行模式：`mv /opt/module/flink-1.13.0 /opt/module/flink-1.13.0-standalone`**

-   **修改之前的FLINK_HOME环境变量：`vim /etc/profile.d/my_env.sh` **

    ```txt
    #FLINK_HOME
    export FLINK_HOME=/opt/module/flink-1.13.0-standalone
    export PATH=$PATH:$FLINK_HOME/bin
    ```

-   **执行命令，使环境变量生效：`source /etc/profile`**

-   **JobManager节点配置：`vim /opt/module/flink-1.13.0-standalone/conf/flink-conf.yaml`**![image-20230227151242157](C:\Users\28645\AppData\Roaming\Typora\typora-user-images\image-20230227151242157.png)

-   **TaskManager节点配置：`vim /opt/module/flink-1.13.0-standalone/conf/workers`**

    ```txt
    hadoop132
    hadoop133
    hadoop134
    ```

-   **分发flink-1.13.0-standalone目录：`xsync /opt/module/flink-1.13.0-standalone/`**

-   **在hadoop133、hadoop134节点中配置环境变量：`vim /etc/profile.d/my_env.sh`**

    ```txt
    #FLINK_HOME
    export FLINK_HOME=/opt/module/flink-1.13.0-standalone
    export PATH=$PATH:$FLINK_HOME/bin
    ```

**==至此，Flink Standalone运行模式已经配置完成，下面将进行集群启动和停止==**

#### 1.2.1 Standalone运行模式下的会话模式（Standalone - Session模式）

-   **来到JobManager服务所在的hadoop132节点，执行命令，启动Flink Standalone运行模式的会话模式：`start-cluster.sh`。`start-cluster.sh`脚本将依次启动以下服务：**

    ```txt
    [justlancer@hadoop132 ~]$ start-cluster.sh 
    Starting cluster.
    Starting standalonesession daemon on host hadoop132.
    Starting taskexecutor daemon on host hadoop132.
    Starting taskexecutor daemon on host hadoop133.
    Starting taskexecutor daemon on host hadoop134.
    ```

-   **此时各节点应该运行的服务有：**

    ```txt
    ============== hadoop132 =================
    1524 StandaloneSessionClusterEntrypoint
    1962 Jps
    1838 TaskManagerRunner
    ============== hadoop133 =================
    1472 Jps
    1410 TaskManagerRunner
    ============== hadoop134 =================
    1468 Jps
    1407 TaskManagerRunner
    ```

    >   **==注意一：`start-cluster.sh`脚本将在<u>本地节点</u>启动一个JobManager，并通过ssh连接到workers文件中所有的worker节点，在每一个节点上启动TaskManager。因此，在`flink-conf.yaml`配置文件中，配置项`jobmanager.rpc.address`所指定的JobManager所在节点并不是实际的JobManager所在的节点，而是`start-cluster.sh`脚本执行的节点才是JobManager服务所在的节点。==**
    >
    >   **==注意二：在虚拟机上操作时，如果在测试了Local模式后，立刻进行Standalone模式的部署，在执行`start-cluster.sh`脚本时，可能会出现启动的仍旧是Local模式的Flink服务，而不是Standalone模式的Flink集群，即使你的环境变量配置的没有问题。出现这个问题的原因不清楚，解决这个问题的方法是，重启虚拟机即可。==**

-   **访问`hadoop132:8081`，进入Standalone模式的Web UI，对Flink集群进行监控**![image-20230228103044465](C:\Users\28645\AppData\Roaming\Typora\typora-user-images\image-20230228103044465.png)

-   **执行命令，停止Standalone - Session模式的Flink集群：`stop-cluster.sh`**

>   **Standalone运行模式下没有单作业部署模式，一方面，Flink本身无法直接以单作业模式启动集群，需要借助资源调度组件；另一方面，Flink本身也没有提供相应的脚本启动单作业模式。**

#### 1.2.2 Standalone运行模式下的应用模式（Standalone - Application模式）

正如前面对应用模式的介绍，应用模式下，直接将Application提交到JobManager上运行，进而解析出DataFlowGraph和JobGraph。应用模式下，需要为每一个Application创建一个Flink集群，进而开启一个JobManager。当该JobManager执行结束后，该Flink集群也就关闭了。

因此，应用模式下，当开启Flink集群时，必需要向Flink提供包含Application的jar包。

-   **利用xftp组件，将已经写好的Flink应用——WordCount提交到/opt/module/flink-1.13.0-standalone/lib目录下**

    ```java
    import org.apache.flink.api.common.functions.FlatMapFunction;
    import org.apache.flink.api.java.functions.KeySelector;
    import org.apache.flink.api.java.tuple.Tuple2;
    import org.apache.flink.streaming.api.datastream.DataStreamSource;
    import org.apache.flink.streaming.api.datastream.KeyedStream;
    import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
    import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
    import org.apache.flink.util.Collector;
    
    /**
     * Author: shaco
     * Date: 2023/1/30
     * Desc: 入门案例：有界流的World Count
     */
    public class D1_WorldCount_Bounded {
        public static void main(String[] args) throws Exception{
            // TODO 1、创建流式执行环境
            StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
    
            // TODO 2、读取文件
            // 以下方法是按行读取文件的
            DataStreamSource<String> inputDS = env.readTextFile("input\\text1_world.txt");
    
            // TODO 3、对读取的每一行数据进行拆分，拆分成不同的单词
            SingleOutputStreamOperator<Tuple2<String, Integer>> worldOfOne = inputDS.flatMap(
                    new FlatMapFunction<String, Tuple2<String, Integer>>() {
                        @Override
                        public void flatMap(String value, Collector<Tuple2<String, Integer>> out) throws Exception {
                            // 单词分解
                            // 需要注意的是，数据是一行一行读取，并被处理的
                            String[] worldList = value.split(" ");
    
                            // 遍历单词数组，并添加到Tuple中
                            for (String world : worldList) {
                                // 将(world,1)输出
                                out.collect(Tuple2.of(world, 1));
                            }
                        }
                    }
            );
    
            // TODO 4、分组
            KeyedStream<Tuple2<String, Integer>, String> keyedStream = worldOfOne.keyBy(new KeySelector<Tuple2<String, Integer>, String>() {
                @Override
                public String getKey(Tuple2<String, Integer> value) throws Exception {
                    return value.f0;
                }
            });
    
            // TODO 5、聚合
            SingleOutputStreamOperator<Tuple2<String, Integer>> sum = keyedStream.sum(1);
    
            sum.print();
    
            // TODO 6、执行流式数据处理
            env.execute();
        }
    }
    ```

-   **在hadoop132节点执行命令启动 JobManager：`standalone-job.sh start --job-classname D1_WorldCount_Bounded`。此处D1_WorldCount_Bounded为Flink Application的全类名。**

-   **访问`hadoop132:8081`，可以看到Application已经提交并处于Running状态，但此时数据并没有执行，原因是没有TaskManager服务启动。**

>   **由于WordCount应用是有限流数据处理，在启动TaskManager后，任务就会开始执行，当任务执行结束后JobManager就会自动停止并释放资源，此时就看不到Web UI了。**

-   **可以在hadoop132、hadoop133、hadoop134中的任意一个节点上启动TaskManager：`taskmanager.sh start`**
-   **当启动了TaskManager后，任务将很快执行完成，JobManager就会释放资源，此时需要手动停止TaskManager：`taskmanager.sh stop`**

>   **==需要注意的是，当JobManager长时间等不到TaskManager启动，获取不到任务执行的资源，那么JobManager会自动关闭。==**

#### 1.2.3 Standalone运行模式的高可用部署（Standalone - HA模式）

Standalone的HA模式是通过在集群中配置并运行多个JobManager的方式避免出现单点故障的问题。

-   **修改`/opt/module/flink-1.13.0-standalone/conf/flink-conf.yaml`文件，增加配置：**

    ```txt
    high-availability: zookeeper
    # 该配置项需要指定namenode的内部通信端口，需要与hdfs-site.xml中的配置项保持同步
    high-availability.storageDir: hdfs://hadoop132:8020/flink/standalone/ha
    # 配置Zookeeper集群的连接地址，其端口号也需要和配置文件中的保持一致
    high-availability.zookeeper.quorum: hadoop132:2181,hadoop133:2181,hadoop134:2181
    high-availability.zookeeper.path.root: /flink-standalone
    high-availability.cluster-id: /cluster_justlancer_flink
    ```

-   **修改配置文件`/opt/module/flink-1.13.0-standalone/conf/masters`，配置JobManager服务所在节点的列表：**

    ```txt
    hadoop132:8081
    hadoop133:8081
    ```

-   **分发修改后的配置文件：`xsync /opt/module/flink-1.13.0-standalone/conf/flink-conf.yaml`，`xsync /opt/module/flink-1.13.0-standalone/conf/masters`**

>   **注意，Standalone - HA模式需要使用Zookeeper集群进行状态监控，同时，需要Hadoop集群存储数据。本次测试使用的Hadoop集群，HDFS和YARN均为高可用部署，其部署步骤与Zookeeper部署步骤参见`Hadoop_HDFS&YARN_HA部署文档.md`**

>   **注意：在Flink 1.8.0版本之前，Flink如果需要使用Hadoop的相关组件，那么需要安装Hadoop进行支持。从Flink 1.8版本开始，Flink不再提供基于Hadoop编译的安装包，如果需要Hadoop环境支持，需要自行在官网上下载Hadoop相关本版的组件，例如需要Hadoop 2.7.5环境的支持，需要下载`flink-shaded-hadoop-2-uber-2.7.5-10.0.jar`等类似的jar包，并将该jar包上传至Flink的lib目录下。在Flink 1.11.0版本之后，增加了很多重要的新特性，其中就包括增加了对Hadoop 3.0.0以及更高版本的支持，不再提供相关Hadoop编译的安装包，而是通过配置环境变量完成与Hadoop的集成。**
>
>   **本次测试过程中，Flink版本为Flink 1.13，Hadoop版本为Hadoop 3.1.3，而Standalone - HA模式需要使用HDFS服务，因此需要对环境变量`HADOOP_HOME`添加额外的配置。**

-   **对环境变量`HADOOP_HOME`进行补充配置，hadoop132、hadoop133、hadoop134均需要配置：`vim /etc/profile.d/my_env.sh`**

    ```txt
    #HAOODP_HOME
    export HADOOP_HOME=/opt/module/hadoop-3.1.3-ha
    export PATH=$PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin
    # 以下两个环境变量为Flink集成Hadoop环境所需要的环境变量
    export HADOOP_CONF_DIR=${HADOOP_HOME}/etc/hadoop
    export HADOOP_CLASSPATH=`hadoop classpath`
    ```

-   **执行文件，使环境变量生效：`source /etc/profile`**

-   **启动Zookeeper集群：`zk_mine.sh start`**

-   **启动Hadoop HA集群，HDFS HA集群一定要启动，YARN HA可以不用启动，暂时用不到：`start-dfs.sh`**

-   **启动Standalone HA集群：`start-cluster.sh`，此时，各节点启动服务如下：**

    ```bash
    [justlancer@hadoop132 ~]$ start-cluster.sh 
    Starting HA cluster with 2 masters.
    Starting standalonesession daemon on host hadoop132.
    Starting standalonesession daemon on host hadoop133.
    Starting taskexecutor daemon on host hadoop132.
    Starting taskexecutor daemon on host hadoop133.
    Starting taskexecutor daemon on host hadoop134.
    ```

-   **检查各节点所启动的服务，应该有：**

    ```txt
    ============== hadoop132 =================
    4273 JournalNode
    5475 TaskManagerRunner
    3844 NameNode
    4004 DataNode
    4484 DFSZKFailoverController
    5591 Jps
    3546 QuorumPeerMain
    5098 StandaloneSessionClusterEntrypoint
    ============== hadoop133 =================
    2928 DataNode
    3650 StandaloneSessionClusterEntrypoint
    3188 DFSZKFailoverController
    3063 JournalNode
    4007 TaskManagerRunner
    2808 NameNode
    4108 Jps
    2621 QuorumPeerMain
    ============== hadoop134 =================
    1832 QuorumPeerMain
    2457 TaskManagerRunner
    2012 DataNode
    2527 Jps
    ```

-   **访问Flink的Web UI，访问`hadoop132:8081`，或者`hadoop133:8081`均看到Flink的Web页面，均可在页面上提交任务以及监控任务运行状态。如果需要查看哪个节点为leader，那么需要查看Zookeeper的节点信息。**![image-20230228165230194](C:\Users\28645\AppData\Roaming\Typora\typora-user-images\image-20230228165230194.png)![image-20230228165253197](C:\Users\28645\AppData\Roaming\Typora\typora-user-images\image-20230228165253197.png)

-   **在Zookeeper中查看JobManager的主备信息：**

    -   **登录Zookeeper：`zkCli.sh -server hadoop132:2181`**

    -   **查看/flink-standalone节点下的信息：`get /flink-standalone/cluster_justlancer_flink/leader/rest_server_lock`**

        ```txt
        [zk: hadoop132:2181(CONNECTED) 7] get /flink-standalone/cluster_justlancer_flink/leader/rest_server_lock
        ��whttp://hadoop132:8081srjava.util.UUID����m�/J
                                                        leastSigBitsJ
                                                                     mostSigBitsxp��&U|�ɡ
        ���E7
        ```

-   **停止Standclone - HA模式：`stop-cluster.sh`**

-   **停止HDFS集群：`stop-dfs.sh`**

-   **停止zookeeper集群：`zk_mine.sh stop`**

### 1.3 YARN模式

Standalone模式由Flink自身提供资源调度，无需其他框架，但存在的问题是，当集群资源不够时，Flink任务提交就会失败，需要进行手动的资源扩充。

另一方面，Flink是大数据计算框架，不是资源调度框架，在需要的时候，只需要和现有的资源调度框架进行集成就好，将专业的事情交给专业的框架来做。

目前，国内使用最为广泛的资源调度框架是YARN，国外使用较为广泛的资源框架是MESOS。还有使用Kubernetes（k8s）进行的容器化部署。以下介绍Flink集成YARN是如何进行集群部署的。

**注意：以下所使用的Hadoop集群是HA部署模式**

>   **正如上面所说，在Flink 1.11版本之后，如果Flink需要集成Hadoop的服务，那么不需要下载相关的Hadoop组件jar包，只需要通过环境变量的配置即可实现Flink对Hadoop环境的依赖。**
>
>   **因此，一定要在有Flink服务的节点配置Hadoop的环境变量：**
>
>   ```txt
>   #HAOODP_HOME
>   export HADOOP_HOME=/opt/module/hadoop-3.1.3-ha
>   export PATH=$PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin
>   # 以下两个环境变量为Flink集成Hadoop环境所需要的环境变量
>   export HADOOP_CONF_DIR=${HADOOP_HOME}/etc/hadoop
>   export HADOOP_CLASSPATH=`hadoop classpath`
>   ```

Flink YARN运行模式前置准备工作：

-   **同样，再解压一份Flink解压包：`tar -zxvf /opt/software/flink-1.13.0-bin-scala_2.12.tgz -C /opt/module/`**

-   **对Flink解压包进行重命名，添加-yarn后缀，表示YARN运行模式：`mv /opt/module/flink-1.13.0 /opt/module/flink-1.13.0-yarn`**

-   **修改之前的FLINK_HOME环境变量：`vim /etc/profile.d/my_env.sh`**

    ```txt
    #FLINK_HOME
    export FLINK_HOME=/opt/module/flink-1.13.0-yarn
    export PATH=$PATH:$FLINK_HOME/bin
    ```

-   **配置上述Hadoop的环境变量：`vim  /etc/profile.d/my_env.sh`**

    ```txt
    #HAOODP_HOME
    export HADOOP_HOME=/opt/module/hadoop-3.1.3-ha
    export PATH=$PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin
    # 以下两个环境变量为Flink集成Hadoop环境所需要的环境变量
    export HADOOP_CONF_DIR=${HADOOP_HOME}/etc/hadoop
    export HADOOP_CLASSPATH=`hadoop classpath`
    ```

-   **执行文件，使环境变量生效：`source /etc/profile`**

**==Flink YARN运行模式不需要修改其他配置文件==**

#### 1.3.1 YARN运行模式下的会话模式（YARN - Session模式）

不同于YARN的其他模式，YARN -Session模式需要先启动一个YARN会话，进而在会话中来启动Flink集群。

-   **启动Zookeeper集群：`zk_mine.sh start`**

-   **启动HDFS HA集群：`start-dfs.sh`**

-   **启动YARN HA集群：`start-yarn.sh`**

-   **向YARN集群申请资源，开启YARN会话，进而会自动启动Flink集群：`yarn-session.sh -nm test`**

    ```txt
    # 参数说明
    -nm(--name)					配置在YARN UI界面上显示的任务名称
    
    -d(--detached)				分离模式，YARN会话会前台占用一个客户端会话，使用该参数，可以使YARN会话后台运行
    -jm(--jobManagerMemory)		配置JobManager所需要的内存，默认单位是MB
    -tm(--taskManagerMemory)	配置TaskManager所使用的内存，默认单位是MB
    -qu(--queue)				指定YARN队列名称		
    ```

    >   **在Flink 1.11.0版本之前可以使用-n参数和-s参数分别指定TaskManager数量和Slot数量。从Flink 1.11.0版本开始，便不再使用-n参数和-s参数。YARN会按照需求，动态分配TaskManager和Slot。所以YARN - Session模式是动态分配资源的。**

-   **YARN - Session启动后，会给出一个Web UI地址以及一个YARN Application ID**

