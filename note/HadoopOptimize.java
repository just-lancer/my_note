package optimize;

public class HadoopOptimize {
    /**
     *  Hadoop优化
     *  1、Hadoop数据压缩
     *  2、MapReduce优化（业务）
     */

    /**
     *  Hadoop数据压缩
     *      概述：Hadoop中，磁盘IO和网络传输是重要的资源，文件向HDFS中的写入以及在集群中的网络传输，会消耗这些资源
     *          因此，应该尽可能将文件大小减小。
     *          但需要说明的是，尽管文件的压缩和解压缩所需要的CPU资源不高，但压缩算法不同需要花费的时间也不同。
     *          因此，采用文件压缩会减少磁盘IO和提高了网络传输的效率，但也增加了CPU资源的消耗。所以，文件压缩运用得当会
     *          提高性能，但运用不当也会降低性能
     *
     *      使用压缩原则：
     *          当job任务属于运算密集型，即业务运算复杂，应该慎重使用压缩
     *          当job任务属于IO密集型，应该多使用压缩
     *
     *      MapReduce支持的压缩编码
     *      压缩格式    Hadoop是否自带      压缩文件扩展名      压缩文件是否可切分      文件压缩后，原MapReduce是是否需要修改     优缺点
     *      default     是，直接使用           .default            否                      否
     *      Gzip        是，直接使用           .gzip               否                      否                                压缩率比较高， 而且压缩/解压速度也比较快；不支持分片
     *      bzip2       是，直接使用           .bz2                是                      否                                支持分片，具有很高的压缩率（比Gzip高）；（解）压缩速度慢
     *      LZO         否，需要安装           .lzo                是                      是，需要添加索引，并指定输入格式    压缩/解压速度比较快，合理的压缩率，支持分片，Hadoop中最流行的压缩方式
     *      snappy      否，需要安装           .snappy             否                      否                                高速压缩速度和合理的压缩率；不支持分片，压缩率低
     *
     *      MapReduce支持的压缩/解压缩格式算法所对应的编/解码器
     *      default     org.apache.hadoop.io.compress.DefaultCodec
     *      Gzip        org.apache.hadoop.io.compress.GzipCodec
     *      bzip2       org.apache.hadoop.io.compress.BZip2Codec
     *      LZO         com.hadoop.compression.lzo.LzopCodec
     *      snappy      org.apache.hadoop.io.compress.SnappyCodec
     *
     *
     *      应该在合适的位置进行文件压缩，MapReduce执行流程：
     *      数据源 --- [InputFormat] ---> Mapper ---[shuffle] ---> Reducer --- [OutputFormat] ---> 结果文件
     *      在数据输入端、Mapper输出端、Reducer输出端都有着大量的磁盘IO和网络传输任务，因此可以考虑文件压缩
     *
     *      数据输入端：Hadoop配置文件在数据输入时指定了默认的编解码器，在数据输入时，Hadoop会自动检测文件的扩展名，并对其进行解压缩。
     *              换句话说，在数据输入端要想使用压缩/解压缩，那么我们只能手动将文件压缩，然后上传。数据输入端只能进行文件解压缩
     *      Mapper输出端：shuffle过程在MapReduce流程中是最消耗资源的，在此进行数据压缩能显著改善shuffle流程效率，压缩是为了解决磁盘IO和网络传输
     *      Reducer输出端：在此阶段启用压缩技术能够减少要存储的数据量， 因此降低所需的磁盘空间
     *
     *      配置文件中，压缩参数配置：
     *参数名称                                             参数默认值                                    压缩阶段         参数说明
     *io.compression.codecs                               null                                          源数据输入端      null
     *
     *mapreduce.map.output.compress                       false                                         Mapper输出端     true：开启Mapper输出端文件压缩
     *mapreduce.map.output.compress.codec                 org.apache.hadoop.io.compress.DefaultCodec    Mapper输出端     该参数指定解/编码器
     *
     *mapreduce.output.fileoutputformat.compress          false                                         Reducer输出端    true：开启Reducer输出端文件压缩
     *mapreduce.output.fileoutputformat.compress.codec    org.apache.hadoop.io.compress.DefaultCodec    Reducer输出端    参数指定解/编码器
     *      说明：
     *      当我们开启压缩模式后，还需要指定文件的压缩模式，即指定解/编码器
     *      使用文件压缩，可以在配置文件中设置，也可以在Driver驱动类中设置
     *          打开并设置默认的解/编码器在core-site.xml
     *          打开并设置Mapper、Reducer端的解/编码器在mapred-site.xml
     *
     */

    /**
     *  MapReduce优化（业务）
     *  MapReduce跑的慢的原因分析：两个角度：
     *      硬件：计算机性能低下，CPU、磁盘、内存、网络
     *      软件方面：
     *          数据角度：
     *              > 小文件过多：1、会影响HDFS存储能力，NN节点的元数据存储受到影响；2、小文件过多，切片过多，MapTask过多，影响MapReduce执行效率
     *              > 数据倾斜：主要出现在ReduceTask阶段，原因在于数据分区时，某一分区的数据远大于其他分区数据，使得其他分区对应的ReduceTask任务完成后，
     *                          该分区对应的ReduceTask还在执行，拖累了整体MapReduce的执行，严重时会使得整个MapReduce任务失败
     *              > 大量不可切片的超大压缩文件：大量的不可切片的压缩文件会产生大量的MapTask任务，同时每个MapTask处理的数据量过大，使得MapReduce执行变慢
     *          IO角度：
     *              > 数据溢写次数过多，数据溢写会出现在：环形缓冲区溢写数据；当一个MapTask处理数据后向磁盘溢写多个文件后，文件在传输到ReduceTask前会进行
     *                      合并，要进行磁盘溢写；ReduceTask将MapTask的输出数据拷贝到本地内存时，当内存的容量不足会进行磁盘溢写
     *              > 数据合并次数过多：在MapTask处理了数据后，一个Maptask任务溢写的多个文件会进行合并；ReduceTask将MapTask处理完的数据拉取到本地时，会
     *                      进行数据合并
     *          MapTask和ReduceTask任务的数量：
     *              > MapTask与ReduceTask任务数量设置不合理
     *              > MapTask运行时间过长，导致ReduceTask等待时间过长
     *
     *  MapReduce优化数据思路：从6个方面着手考虑
     *      1、数据输入：主要解决输入数据小文件过多
     *          解决方案：① 输入端使用CombineTextInputFormat进行文件读入；② 使用Hadoop的新特性：小文件存档
     *      2、Map端处理：主要解决数据溢写和合并的次数
     *          解决方案：合理设置shuffle的内存大小以及溢写条件，减少IO次数；设置增加合并文件的数量，减少合并次数
     *      3、Reduce端处理：主要解决与Map端的协同工作问题
     *          解决方案：① 合理设置ReduceTask的任务数量，使MapTask与ReduceTask任务数量能很好的协同工作。（MT、RT数量太少会导致Task等待；太多会使得MT、RT资源争夺）
     *                   ② 设置MT和RT共存，并行处理任务，通过参数调整，可以使使MT运行到一定程度后， RT也开始运行，减少RT的等待时间。
     *                   ③ 在不影响业务逻辑的条件下，应该尽可能避免使用RT任务
     *                   ④ 合理设置shuffle/Reduce阶段的内存缓冲区大小，内存缓冲区的数据会直接传输到RT，而缓冲区写满数据，会产生数据溢写，增加IO
     *      4、IO和网络传输优化：使用压缩，减少需要传输的数据
     *
     *      5、数据倾斜解决：合理设置分区，一级分区造成数据倾斜，可以设置二级分区；进行ReduceTask负载均衡，将相同key的一组values分配给多个ReduceTask执行；
     *                      在shuffle/Map阶段进行相同key的数据合并，减小ReduceTask的计算压力
     *
     *      6、常用参数调优：
     *      配置参数                                         参数说明
     *      mapreduce.map.memory.mb                         一个MT可使用的资源上限（单位:MB），默认为1024。如果MT实际使用的资源量超过该值，则会被强制杀死。
     *      mapreduce.reduce.memory.mb                      一个RT可使用的资源上限（单位:MB），默认为1024。如果RT实际使用的资源量超过该值，则会被强制杀死。
     *
     *      mapreduce.map.cpu.vcores                        每个MT可使用的最多cpu core数目，默认值: 1
     *      mapreduce.reduce.cpu.vcores                     每个RT可使用的最多cpu core数目，默认值: 1
     *
     *      mapreduce.reduce.shuffle.parallelcopies         每个Reduce去Map中取数据的并行数。默认值是5，（一次从多少个Map中拉取数据）
     *
     *      mapreduce.reduce.shuffle.input.buffer.percent   Buffer（Reduce端的内存缓冲区）大小占 Reduce可用内存的比例。默认值 0.7
     *      mapreduce.reduce.shuffle.merge.percent          Buffer中的数据达到多少比例开始写入磁盘。默认值 0.66
     *          也就是内存缓冲区Buffer的内存使用量达到Reduce可用内存 * 0.7 * 0.66后，开始向硬盘写入数据
     *
     *      mapreduce.reduce.input.buffer.percent           指定多少比例的内存用来接收Buffer中的数据，默认值是0.0
     *          当Reduce将所有的Map上对应自己Partition的数据下载完成后，就会开始真正的Reduce计算阶段。RT真正进入Reduce函数的计算阶段，
     *          由于Reduce计算时肯定也是需要消耗内存的，而在读取Reduce需要的数据时，同样是需要内存作为buffer，这个参数是控制，
     *          Reducer使用多少的内存（百分比）来作为Reduce读已经sort好的数据的buffer大小？默认情况下为0，也就是说，默认情况下，
     *          Reduce是全部从磁盘开始读处理数据。可以用mapreduce.reduce.input.buffer.percent来设置reduce的缓存。如果这个参数大于0，
     *          那么就会有一定量的数据被缓存在内存并输送给reduce，当reduce计算逻辑消耗内存很小时，可以分一部分内存用来缓存数据，
     *          可以提升计算的速度。所以默认情况下都是从磁盘读取数据，如果内存足够大的话，务必设置该参数让reduce直接从缓存读数据，
     *          这样做就有点Spark Cache的感觉
     *
     *      说明：yarn参数的配置，需要重启yarn服务才能生效
     *      yarn.scheduler.minimum-allocation-mb	        给应用程序Container分配的最小内存，默认值：1024
     *      yarn.scheduler.maximum-allocation-mb	        给应用程序Container分配的最大内存，默认值：8192
     *      yarn.scheduler.minimum-allocation-vcores	    每个Container申请的最小CPU核数，默认值：1
     *      yarn.scheduler.maximum-allocation-vcores	    每个Container申请的最大CPU核数，默认值：32
     *      yarn.nodemanager.resource.memory-mb	            给Containers分配的最大物理内存，默认值：8192
     *          物理内存与虚拟内存：当物理内存不够时，会将硬盘空间作为临时内存，内存+硬盘=虚拟内存
     *
     *      说明：shuffle性能优化参数
     *      mapreduce.task.io.sort.mb   	                Shuffle的环形缓冲区大小，默认100m
     *      mapreduce.map.sort.spill.percent   	            环形缓冲区溢出的阈值，默认80%
     *
     *      容错相关参数
     *      mapreduce.map.maxattempts	                    每个Map Task最大重试次数，一旦重试次数超过该值，则认为Map Task运行失败，默认值：4。
     *      mapreduce.reduce.maxattempts	                每个Reduce Task最大重试次数，一旦重试次数超过该值，则认为Map Task运行失败，默认值：4。
     *      mapreduce.task.timeout                          Task超时时间
     *
     *
     */
}
