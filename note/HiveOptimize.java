package hive;

public class HiveOptimize {
    /**
     *  Hive优化
     *  1、explain -- 执行计划
     *      Hive提供了explain命令来展示一个Hive查询的执行计划，这个执行计划对于我们了解底层原理，Hive调优，排查数据倾斜等很有帮助
     *      语法结构：EXPLAIN [EXTENDED|CBO|AST|DEPENDENCY|AUTHORIZATION|LOCKS|VECTORIZATION|ANALYZE] query
     *      参数说明见收藏
     *
     *  2、Fetch -- 抓取
     *      Fetch抓取是指，Hive中对某些情况的查询可以不必使用 MapReduce 计算获得，比如select * from tb_name; 这种情况下，Hive可以
     *      简单读取tb_name对应的存储目录下的文件，直接展示在控制台
     *
     *      Fetch的配置：
     *          在hive-default.xml.template的配置文件中有配置项：hive.fetch.task.conversion，可以配置三个值：
     *              > none：关闭Fetch优化
     *              > minimal：只在select *、使用分区列过滤、带有limit的语句上进行优化
     *              > more：基于minimal，select不仅仅可以是*，还可以单独选择几列，并且filter也不再局限于分区字段，同时支持虚拟列（别名）
     *
     *  3、本地模式
     *      当输入数据不是大数据集时，为执行查询而触发执行任务消耗的时间可能会比执行任务所消耗的时间更长，此时，Hive可以通过本地模式在单台
     *      服务器上执行任务，从而大大减少执行时间。
     *
     *      在hive-default.xml.template配置文件中有配置项：hive.exec.mode.local.auto，默认值为false，表示关闭本地模式；设为true为开启本地模式
     *
     *  4、表的优化
     *      4.1 表的join操作
     *          Reducer处理表的join操作是最为基本的，以两表join为例，叙述其基本过程和存在的问题：
     *              基本过程：根据设置的关联字段，在Map阶段，将两表中的每一条记录打上标记，标记该记录来源于哪一个表，随后，在shuffle阶段，
     *                       对具有相同key（即关联字段）的记录进行合并，形成一条记录，最后交给ReduceTask处理；在Reducer阶段，对于任何一条<k,v>数据
     *                       将v中的来自不同表的记录分离，保存到不同的容器中进行维护，随后对不同容器中的数据进行join操作，最后输出结果。
     *              存在问题：当两张表数据量很大时，可能会出现数据倾斜：关联字段中，某一个值所对应的数据量非常大，这样，由这个值维护的相同key的一组values在ReduceTask中
     *                       执行时，执行时间过长，导致其他ReduceTask等待该ReduceTask结束，严重时，造成job执行失败。或者，关联字段中，每一个值所对应的
     *                       数据量都较为均衡，但在分区时，进入同一个ReduceTask的key太多，导致数据倾斜。
     *              优化方案：当这些关联字段的key不为空，那么优化方案在于进行合适的分区操作，进行ReduceTask负载均衡；
     *                       当这些关联字段的key为null时，那么优化方案在于要处理掉这些null，null数据不多时，可以删除null数据，太多时，对其随机赋值
     *
     *              说明：null ≠ null，所以关联字段中的null与null之间会形成笛卡尔积
     *
     *          Map端的表join：
     *              Reducer端的表join，仍旧存在一定问题，需要经过shuffle阶段和ReduceTask处理，因此也会使用资源和时间，在map端进行表join可以省略
     *                      shuffle阶段和ReduceTask，因此会效率提高。
     *
     *              Map端的表join有一个前提，join的两个表之间至少需要一个是小表（默认小于25M的表为小表）
     *              基本过程：MapTask将小表的数据提前添加到内存中，随后对大表进行数据读取，然后去到内存中进行数据join，join完直接将数据输出。
     *
     *              存在的问题：join时，需要小表左join大表，这是因为内存空间有限，防止内存溢出的问题。新版本对小表join大表进行了优化，左join和右join
     *                         已经没有区别。
     *
     *          对于Map端join，右两个参数可以进行配置，在hive-default.xml.template配置文件中，
     *              有配置项：hive.auto.convert.join，默认为true，表示默认开启自动选择Map端join
     *                       hive.mapjoin.smalltable.filesize，默认值为25000000，表示Map端join，表大小的阈值为25M
     *
     *      4.2 group by
     *          存在的问题：与上述Reducer端join存在的问题类似，具有相同key的一组values会进入到同一个ReduceTask进行处理，当一个表中key对应的数据过大，
     *                      也会出现数据倾斜问题。
     *
     *          优化方案及hive-default.xml.template参数配置：
     *                  ① 在Map端就对数据进行分组聚合，比如，求平均值，在每个MapTask中对数据进行局部求均值，然后放到一个ReduceTask中求最终均值
     *                      配置参数：hive.map.aggr，默认值为true，表示在Map端进行数据分组聚合
     *                      配置参数：hive.groupby.mapaggr.checkinterval，默认值100000，表示当同一个key对应的数据量为100000时，进行Map端数据分组聚合
     *
     *                   ② 出现数据倾斜时，进行ReduceTask负载均衡。即，一个key对应的数据量过大时，将这一个key对应的数据分给多个ReduceTask执行
     *                      配置参数：hive.groupby.skewindata，默认值为false，表示当有数据倾斜时，不进行ReduceTask负载均衡
     *
     *      4.3 count(distinct field) 去重统计
     *          存在的问题：在数据量大的情况下，由于count(distinct field)操作需要用一个ReduceTask来完成，这一个ReduceTask需要处理的数据量太大，就会导致整个Job很难完成
     *
     *          优化方案：先使用group by对数据分组，再count 分组字段进行去重统计。当然也需要注意group by会产生数据倾斜的问题
     *
     *      4.4 笛卡尔积
     *          解决方案：设置严格模式
     *
     *      4.5 行列过滤
     *          > 列过滤：不要出现查询语句不要出现 select *，效率极低；解决方案一般是设置严格模式
     *          > 行过滤：对于多表join，对于不必要的记录，可以先使用where语句对数据进行过滤，再进行表join
     *
     *      4.6 数据分区，分桶
     *
     *  5、合理设置MapTask和ReduceTask的数量
     *      MapTask的数量由文件的切片决定，通过设置文件切片可以来设置MapTask的数量
     *      ReduceTask的数量由数据分区决定，通过配置文件设置分区数量，可以设置ReduceTask的数量
     *
     *      Map端
     *      高表：文件大小适当，但字段少，记录多
     *      高表的处理：需要适当增加MapTask的数量
     *
     *      小文件的处理：进行小文件合并，在InputFormat阶段，使用CombineTextInputFormat进行小文件合并处理，在配置文件中可以设置
     *          配置项：hive.input.format，配置值：org.apache.hadoop.hive.ql.io.CombineHiveInputFormat
     *
     *      Reduce端
     *      每个ReduceTask能够处理的数据量，默认为256M，配置项：hive.exec.reducers.bytes.per.reducer=256000000
     *      每个job任务最多能开启的ReduceTask数，默认为1009，配置项：hive.exec.reducers.max=1009
     *
     *      因为每个ReduceTask能够处理的数据量时256M，因此，在MapTask结束时，可以进行数据合并。
     *      配置项：
     *          hive.merge.mapfiles，默认值是true，表示MapTask任务结束时开启数据合并
     *          hive.merge.size.per.task，默认值是256M，表示MapTask任务结束时，数据合并生成文件的大小为256M
     *
     *  6、并行执行
     *      Hive SQL执行会转换成一个或多个阶段，默认情况下，Hive只会串行执行每个阶段，当多个阶段之间不产生依赖时，那么
     *      这些阶段就可以并行执行，提高执行效率。需要注意的是，任务并行需要集群有空闲资源。
     *
     *      配置项：
     *          hive.exec.parallel，默认值为false，表示默认情况下，Hive不支持阶段并行
     *          hive.exec.parallel.thread.number，默认值为8，表示默认情况下，Hive只支持8个阶段并行执行。
     *
     *  7、严格模式
     *      Hive提供了严格模式，用于防止用户执行可能产生意想不到的不好的影响的查询
     *
     *      严格模式配置：
     *          hive.mapred.mode配置项，默认值nonstrict，表示默认不打开严格模式
     *
     *      严格模式限制：
     *          > 当表是分区表时，进行查询时必须使用where语句对分区进行过滤，减小查询范围
     *          > 使用order by对查询结果排序时，必须使用limit对查询结果进行限制。（order by 与 limit不是有冲突吗）
     *          > 不允许笛卡尔积查询
     *
     *          > 尽量少使用'select *'进行数据查询
     *
     *  8、JVM重用
     *      每个Task都需要一个JVM来运行，当Task计算量较小时，可以让多个Task运行在一个JVM上。
     *
     *      开启JVM重用配置项：mapreduce.job.ubertask.enable
     *
     *  9、压缩
     *
     */
}
