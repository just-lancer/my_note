package zookeeperkonwledge;

public class Zookeeper {
    /**
     *  zookeeper学习
     *  1、概述
     *  2、本地/集群环境搭建（zookeeper服务端（service））
     *  3、zookeeper选举机制
     *  4、zookeeper客户端（client）
     *  5、zookeeper节点类型及详细信息
     *  6、zookeeper客户端常用命令
     *  7、监听器原理
     */

    /**
     *  1、zookeeper概述
     *      zookeeper是一个开源的分布式的apache项目，专门为其他分布式应用提供协调服务的
     *      从设计模式的角度理解，zookeeper是一个基于观察者设计模式的分布式服务管理框架。
     *      zookeeper负责存储和管理其他分布式应用都关心的数据，然后接受观察者的注册，
     *      当这些数据的状态发生变化，Zookeeper就负责通知已经在Zookeeper上注册的那些观察者，
     *      让其做出相应的反应
     *
     *      1.1 zookeeper工作机制：zookeeper = 文件系统 + 通知机制
     *
     *      1.2 zookeeper特点
     *          > zookeeper是一个集群，在这个集群中，只有一个领导者(leader)，能有多个跟随着(follower)
     *              leader作用：
     *                  > 负责管理整个集群
     *                  > 负责数据事务的相关操作
     *                  > 转发数据非事务操作给从节点
     *              follower作用：
     *                  > 实时从主节点拉取数据，从而保持数据的一致性
     *                  > 负责数据非事务相关操作
     *                  > 转发数据事务操作给主节点
     *              observer作用：
     *                  > 没有选举权，其他作用和follower一样
     *          > 集群中只要有半数以上节点存活(>N/2)， Zookeeper集群就能正常服务
     *          > 全局数据一致：集群中每个Server都保存相同的数据副本， Client无论连接到哪个Server，数据都是一致的
     *          > 更新请求按顺序进行：来自同一个Client的更新请求会按其发送顺序依次执行
     *          > 数据更新原子性：一次数据更新要么成功，要么失败
     *          > 实时性：在一定时间范围内，Client能读到最新数据
     *
     *      1.3 zookeeper数据结构
     *          ZooKeeper数据模型的结构与Unix文件系统很类似， 整体上可以看作是一棵树， 每个节点称做一个ZNode。
     *          每一个ZNode默认能够存储 1MB 的数据，每个ZNode都可以通过其路径唯一标识
     *          |---- /
     *              |---- /znode1
     *                  |---- /znode1/right
     *                  |---- /znode1/left
     *                  ......
     *              |---- /znode2
     *                  |---- /znode2/left
     *                  ......
     *
     *      1.4 zookeeper能够解决的问题
     *          1.4.1 统一命名服务：在分布式环境下， 经常需要对应用/服务进行统一命名， 便于识别
     *              |---- /
     *                  |---- /service
     *                      |---- www.baidu.com
     *                          |---- 192.168.200.131
     *                          |---- 192.168.200.132
     *                          |---- 192.168.200.133
     *              多台服务器命名为一个域名，以对外提供服务
     *
     *          1.4.2 统一配置管理：
     *              需求：
     *              > 分布式环境下，配置文件的同步是非常常见的，比如一个集群中，所有节点的配置信息都必须是一致的
     *              > 对配置文件修改后，希望能够快速同步到各个节点上
     *
     *              zookeeper实现配置文件管理：
     *              > 可将配置信息写入ZooKeeper上的一个Znode
     *              > 各个客户端服务器监听这个Znode
     *              > 一旦Znode中的数据被修改， ZooKeeper将通知各个客户端服务器
     *
     *          1.4.3 统一集群管理
     *              需求：
     *              > 分布式环境中，实时掌握每个节点的状态是必要的，可根据节点实时状态做出一些调整
     *
     *              zookeeper实现集群统一管理：
     *              > 可将节点信息写入ZooKeeper上的一个ZNode
     *              > 监听这个ZNode可获取它的实时状态变化
     *
     *          1.4.4 服务器动态上下线
     *              属于统一集群管理的功能之一，实时监控各个节点信息，包括上下线
     *
     *          1.4.5 软负载均衡
     *              > 在Zookeeper中记录每台服务器的访问数，让访问数最少的服务器去处理最新的客户端请求
     *
     */

    /**
     *  2、zookeeper环境搭建
     *  主备工作：
     *      > 安装Linux版本的JDK
     *      > 上传zookeeper安装包并解压到指定文件目录下
     *          解压后有：bin、conf、lib、docs等目录
     *  zookeeper本地安装：
     *      > 将conf这个路径下的zoo_sample.cfg修改为zoo.cfg，原因是启动服务端时，zkService.sh需要调用zkEnv.sh，zkEnv.sh要调用zoo.cfg
     *      > 创建zookeeper数据存储目录，并修改conf/zoo.cfg的dataDir配置项，指定数据存储路径
     *
     *      本地常用命令：
     *      > 启动zookeeper服务端：zkService.sh start
     *      > 查看zookeeper服务端状态：zkService.sh status
     *      > 启动zookeeper客户端：zkCli.sh
     *      > 退出zookeeper客户端：quit
     *      > 退出zookeeper服务端：zkService.sh stop
     *      需要说明的是，需要先启动服务端，才能启动客户端
     *
     *  /conf/zoo.cfg配置文件的参数：
     *      > tickTime =2000：通信心跳数，Zookeeper服务器与客户端，客户端与客户端之间的心跳时间，单位毫秒。
     *          即每隔2000毫秒，各节点之间发送一个心跳，用于检测节点是否还能正常工作
     *      > initLimit =10：Leader与Follower（LF）直接按的初始通信时限
     *          leader和Follower第一次建立连接时，最多能容忍的心跳数，相应时间为2000*5，超出时限还没有连接成功，则连接失败
     *      > syncLimit =5：Leader与Follower（LF）同步通信时限，Leader与Follower建立连接后，leader确认Follower运行状态的心跳数
     *          超出相应的时限，则判定该节点死亡，从集群中删除
     *      > dataDir：数据文件目录+数据持久化路径
     *      > clientPort =2181：客户端连接端口，zookeeper监听客户端连接的端口
     *
     *  zookeeper集群安装：
     *      1、集群规划：zookeeper集群包含三个zookeeper服务器
     *      2、将zookeeper安装包及解压目录进行分发
     *      3、配置服务器编号：
     *          在/zkDir目录下创建myid文件，并写入相应的服务器编号，例如2
     *          说明：
     *              不要有空格和空行
     *              文件名必须是myid，因为源码会读取这个文件
     *
     *      4、分发myid
     *      5、配置zoo.cfg文件，增加如下配置
     *          #######################cluster##########################
     *          server.2=hadoop102:2888:3888
     *          server.3=hadoop103:2888:3888
     *          server.4=hadoop104:2888:3888
     *          参数说明：server.A=B:C:D
     *          A是一个数字，表示这个是第几号服务器。集群模式下配置一个文件myid，这个文件在dataDir目录下，这个文件里面有一个数据就是A的值，
     *              Zookeeper启动时读取此文件，拿到里面的数据与zoo.cfg里面的配置信息比较从而判断到底是哪个server
     *          B是这个服务器的IP地址
     *          C是这个服务器Follower与集群中的Leader服务器交换信息的端口
     *          D是万一集群中的Leader服务器挂了，需要一个端口来重新进行选举，选出一个新的Leader，而这个端口就是用来执行选举时服务器相互通信的端口
     *      6、分发zoo.cfg文件
     *
     */

    /**
     *  3、zookeeper选举机制
     *      zookeeper集群可用的必要条件：集群中半数以上机器存活，集群可用。所以Zookeeper适合安装奇数台服务器。
     *
     *      leader选举场景：① zookeeper集群启动时，第一次leader选举；② zookeeper集群已经启动，当前leader挂掉，需重新选举leader
     *
     *      先验知识：
     *          SID：服务器ID，用来唯一标识zookeeper集群中的一台机器。SID不能重复，并且和myid一致
     *          ZXID：事务ID，用来标识一次服务器状态的变更。与zookeeper服务器对客户端“更新请求”的处理逻辑有关
     *          Epoch：每个leader任期的代号。当leader挂了，投票选举的过程中，Epoch的值与逻辑时钟相同
     *
     *      第一次leader选举：
     *          当服务器节点启动，发现没有leader时，会进行投票选举，选出leader，每个zookeeper节点各持一票
     *          投票过程中，zookeeper节点两两之间会进行选票交换，SID小的节点会将票投给SID大的节点
     *          所以，第一次leader选举过程中，按zookeeper节点启动顺序，前N/2台服务器中，SID最大的服务器会成为leader
     *
     *      第n次leader选举：
     *          根据剩下活着的每个zookeeper的(SID、ZXID、Epoch)进行选举
     *          比较顺序，Epoch ----> ZXID ----> SID
     *          Epoch大的当leader；Epoch相同，ZXID大的当leader；Epoch和ZXID都相同，SID大的当leader
     *
     */

    /**
     *  5、zookeeper节点类型及节点详细信息
     *      基本说明：
     *      > 持久(persistent)：客户端和服务器端断开连接后，创建的节点不删除
     *      > 短暂/临时(ephemeral)：客户端和服务器端断开连接后，创建的节点自己删除
     *      > 有序号：创建znode时设置顺序标识，znode名称后会附加一个值，顺序号是一个单调递增的计数器，由父节点维护
     *      > 无序号：即创建znode时，不为节点设置标识
     *
     *      节点类型：
     *      > 持久化目录节点：客户端与Zookeeper断开连接后，已建立的节点依旧存在
     *      > 临时目录节点：客户端与Zookeeper断开连接后， 已建立的节点会随之被删除
     *      > 持久化顺序编号目录节点：客户端与Zookeeper断开连接后，该节点依旧存在；并且该节点创建时，Zookeeper会给该节点的名称进行顺序编号
     *      > 临时顺序编号目录节点：客户端与Zookeeper断开连接后，该节点会随之删除；而在该节点创建时，Zookeeper会给该节点的名称进行顺序编号
     *
     *      说明：创建znode时设置顺序标识，znode名称后会附加一个值，顺序号是一个单调递增的计数器，由父节点维护
     *      注意：在分布式系统中，顺序号可以被用于为所有的事件进行全局排序，这样客户端可以通过顺序号推断事件的顺序
     *
     *      节点详细参数信息
     *      > czxid： 创建节点的事务ID（ZXID），表示创建当前节点的事务的编号
     *      > ctime： 节点被创建的时间，用毫秒数（从 1970 年开始）表示
     *      > mzxid： 节点最后更新时的事务ID（ZXID），也就是最近的一次更新的事务ID
     *      > mtime： 节点最后被修改的时间，用毫秒数（从 1970 年开始）表示
     *      > pZxid： 当前节点的子节点最后一次被修改时的事务ID（ZXID）
     *      > cversion： 当前节点的子节点的变化号，子节点修改次数
     *      > dataversion： 当前节点数据变化号
     *      > aclVersion： 当前节点访问控制列表的变化号
     *      > dataLength： 当前节点的数据长度
     *      > numChildren： 当前节点的子节点数量
     */

    /**
     *  6、zookeeper客户端常用命令（Linux命令行）
     *      > zkCli.sh                       开启客户端   说明：必须已经开启服务端；默认开启本地客户端，指定客户端的位置：zkCli.sh -server IP地址
     *      > help                           获取所有命令
     *      > quit                           退出客户端
     *      > ls [-s] path                  查看节点包含内容，-s 表示查看节点的详细信息
     *      > create [-s][-e] path 节点值    创建节点，-s表示创建有序节点，-e表示创建临时节点
     *      > get [-s][-w] path             获取节点值，-s表示显示详细信息，-w表示监听节点内容
     *      > set path 节点值                修改节点的值，给节点设置值
     *      > stat path                     查看节点状态
     *      > delete                        删除节点
     *      > deleteall                     递归删除节点
     */
}
