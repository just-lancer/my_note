package optimize;

public class NewFeature {
    /**
     *  Hadoop新特性
     */

    /**
     *  Hadoop 2.x 新特性
     *  1、集群间数据拷贝
     *      scp实现两个远程主机之间的文件复制
     *      采用distcp命令实现两个Hadoop集群之间的递归数据复制
     *
     *  2、小文件存档
     *      大量小文件的弊端：每个文件均按块存储， 每个块的元数据存储在NameNode的内存中， 因此HDFS存储小文件会非常低效。
     *          因为大量的小文件会耗尽NameNode中的大部分内存。
     *
     *      说明：存储小文件所需要的磁盘容量和数据块的大小无关。例如，一个1MB的文件设置为128MB的块存储，实际使用的是1MB的磁盘空间，而不是128MB
     *
     *      小文件存档思想：
     *      HDFS存档文件或HAR文件， 是一个更高效的文件存档工具， 它将文件存入HDFS块， 在减少NameNode内存使用的同时， 允许对文件进行透明的访问。
     *      具体说来， HDFS存档文件对内还是一个一个独立文件， 对NameNode而言却是一个整体， 减少了NameNode的内存。
     *
     *  3、回收站
     *      相关参数：
     *      默认值fs.trash.interval=0， 0表示禁用回收站;其他值表示设置文件的存活时间。
     *      默认值fs.trash.checkpoint.interval=0， 检查回收站的间隔时间。 如果该值为0，则该值设置和fs.trash.interval的参数值相等。
     *
     *  4、yarn从MapReduce中分离出来
     *
     */

    /**
     *  Hadoop 3.x 新特性
     *  1、多NN的HA架构
     *  2、纠删码
     */
}
