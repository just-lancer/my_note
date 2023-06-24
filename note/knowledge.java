package hive;

public class knowledge {
    /**
     *  Hive学习
     *  1、概述
     *  2、Hive的架构原理
     *  3、Hive与数据库的对比
     *  4、Hive的安装
     *  5、Hive常用的客户端命令和属性配置
     *
     */

    /**
     *  1、概述
     *  Hive是基于Hadoop的数据仓库工具，可以将结构化的数据文件映射为一张表，并提供类SQL查询功能。
     *      注意：Hive只能对结构化的数据进行映射，非结构化数据解决不了
     *
     *  Hive的本质是将Hive SQL转化成MapReduce程序
     *      Hive处理的数据存储在HDFS中
     *      Hive分析数据的底层实现是MapReduce
     *      执行程序运行在Yarn上
     *
     *  Hive优点：
     *      操作接口采用类SQL语法，简单、容易上手
     *      避免了去写MapReduce，学习成本低
     *  Hive缺点：
     *      Hive SQL的表达能力有限：
     *          迭代算法无法表达，
     *          MapReduce数据处理流程的限制，效率更高的算法无法实现
     *      Hive SQL的效率比较低：
     *          Hive SQL是MapReduce的二次包装，其执行效率通常较低
     *          Hive SQL调优比较困难
     *  Hive特点：
     *      Hive执行延迟较高，通常用于数据分析以及对实时性要求不高的场景
     *      Hive的优势在于大量数据的快速读取以及处理
     *      Hive支持自定义函数，可根据自己的需求实现函数
     *
     */

    /**
     *  2、Hive的架构原理
     *  Hive的架构包含三个部分：Hive、RDBMS、Hadoop
     *
     *  Hive端：
     *      Client：用户接口，提供用户连接Hive的方法，包含：cli(command-line interface)、jdbc和浏览器访问
     *      Driver：将Hive SQL转换成MapReduce任务的组件
     *          解析器（SQL Parser）：将SQL字符串转换成抽象语法树AST，这一步一般都用第三方工具库完成，比如antlr；
     *                               对AST进行语法分析，比如表是否存在、字段是否存在、SQL语义是否有误。
     *          编译器（Physical Plan）：将AST编译生成逻辑执行计划。
     *          优化器（Query Optimizer）：对逻辑执行计划进行优化。
     *          执行器（Execution）：把逻辑执行计划转换成可以运行的物理计划。对于Hive来说，就是MR/Spark。
     *
     *  Hadoop：使用HDFS存储数据，使用yarn跑MapReduce任务，使用MapReduce进行计算
     *
     *  RDBMS：一般使用MySQL。用于存储Hive中 库，表的元数据信息。通常Hive ----> MySQL ----> HDFS。元数据信息一般包括表名、
     *          表所属的数据库（默认是default）、表的拥有者、列、分区字段、表的类型（是否是外部表）、表的数据所在目录等
     *
     */

    /**
     *  3、Hive与数据库的对比
     *  Hive采用类SQL语言进行数据查询，除此之外，Hive与数据库没有其他相似的地方。本质上就不是同一个东西。
     *  Hive是为数据仓库设计的，数据库可以使用在online应用中
     *
     *  数据更新：
     *      Hive数据仓库中的内容是读多写少，Hive中不建议对数据进行修改，所有的数据都是在加载的时候确定好的。
     *      数据库的数据支持频繁的更新。
     *
     *  执行速度：
     *      Hive 在查询数据的时候，由于没有索引，需要扫描整个表，因此延迟较高。
     *      另外一个导致 Hive 执行延迟高的因素是 MapReduce框架。
     *      数据库的执行延迟较低。
     *
     *      说明：Hive执行速度慢，数据库执行速度快，有一个前提，数据规模不能太大
     *
     *  处理数据的规模：
     *      Hive能够处理的数据的规模大于数据库处理的数据规模
     *
     */

    /**
     *  4、Hive的安装
     *      Hive的架构决定了连接Hive的方式。Hive有三种连接方式：
     *          > 普通cli连接方式：每个cli就是一个客户端，每个cli独自开启一个metastore服务，用于连接MySQL数据库。
     *              优点：不会出现窗口独占和线程阻塞；缺点是：客户端数量增加，会出现许多相同的metastore服务
     *          > 多个cli客户端使用同一个metastore服务连接数据库
     *              优点：只需要开启一个metastore服务就可以让所有cli客户端连接数据库；缺点是：会出现窗口独占和线程阻塞
     *              开启一个metastore服务步骤：
     *                  > 在hive-site.xml配置文件（自定义配置文件）中，指定元数据库的url。配置项：
     *                      <!-- 指定元数据库的连接地址，url -->
     *                      <property>
     *                          <name>hive.metastore.uris</name>
     *                          <value>thrift://hadoop102:9083</value>
     *                      </property>
     *                  > 在hive客户端启动metastore服务：hive --service metastore
     *                  > 再启动hive时，就是单metastore服务连接元数据库
     *
     *          > 利用jdbc访问hiveserver2服务，hiveserver2服务去访问单独的metastore服务，进而访问数据库
     *              优点：可以只需要开启一个metastore服务就可以让所有cli客户端连接数据库，并且hive查询结果展示友好；缺点是：会出现窗口独占和线程阻塞
     *              利用jdbc方式连接hive的步骤：
     *                  > 在hive-site.xml添加配置：
     *                      <!-- 指定hiveserver2连接的host -->
     *                      <property>
     *                          <name>hive.server2.thrift.bind.host</name>
     *                          <value>hadoop102</value>
     *                      </property>
     *
     *                      <!-- 指定hiveserver2连接的端口号 -->
     *                      <property>
     *                          <name>hive.server2.thrift.port</name>
     *                          <value>10000</value>
     *                      </property>
     *
     *                  > 开启hiveserver2服务：hive --service hiveserver2
     *                  > 启动beeline客户端：beeline -u jdbc:hive2://hadoop132:10000 -n atguigu
     *                      注意，此处需要等待较长的时间
     *                  > 此时再开启hive，就是以hiveserver2服务连接数据库的hive客户端
     *
     */

    /**
     *  5、Hive常用的客户端命令和属性配置
     *  常用命令：Hive常用命令格式：hive 命令
     *  说明：命令都是以'-'开始
     *      > -help：查看hive的所有命令。hive -help;
     *      > -e：不进入hive的交互窗口执行sql语句。hive -e "select * from test";这相当于重新开启了一个Hive会话窗口
     *      > -f：执行脚本中sql语句。hive -f /opt/module/hive/datas/test.sql;
     *
     *      其他：
     *          > exit; 退出hive客户端，提交数据后，退出
     *          > quit; 退出hive客户端，直接退出
     *          说明：新版本hive中，二者没有区别
     *
     *  常用属性配置：
     *      1、打印当前库名和表名：
     *          在hive-site.xml中添加配置：
     *              <property>
     *                  <name>hive.cli.print.header</name>
     *                  <value>true</value>
     *              </property>
     *              <property>
     *                  <name>hive.cli.print.current.db</name>
     *                  <value>true</value>
     *              </property>
     *
     *      2、hive运行日志配置：修改hive运行日志文件到指定地方：
     *          在hive-log4j2.properties配置文件中，将property.hive.log.dir项指定新的日志文件路径
     *          说明：hive-log4j2.properties.template文件是hive的默认配置文件，对其内容修改后，需要将文件名更改，去掉.template
     *
     *
     *  说明：修改配置分为：临时修改和永久修改
     *      修改hive的配置文件是永久修改hive的设置，每次hive的启动都会读取配置文件
     *      临时修改配置文件是在hive客户端中修改配置信息，每次hive客户端关闭都会重置配置信息。临时设置配置信息：在hive客户端中使用set命令
     *
     *      3、查看所有能够临时设置的配置信息：hive> set;
     *      4、查看参数的值：hive> set 参数名;
     *
     *  说明：hive参数的三种配置方式：
     *      > hive的默认配置文件，修改默认配置文件后，要删掉.template后缀
     *      > 自定义hive配置文件，自定义的配置文件会覆盖默认配置，即hive启动时，先读取默认配置文件，再读取自定义配置文件
     *      > 使用set命令设置参数，这种方式是临时方式，只在一个hive会话中生效
     *
     */
}
