package mapreduceknowledge;

public class MapReduce_API {

    /**
     *  Hadoop序列化机制
     *      序列化：就是把内存中的对象，转换成字节序列（或其他数据传输协议）以便于存储到磁盘（持久化） 和网络传输。
     *      反序列化就是将收到字节序列（或其他数据传输协议） 或者是磁盘的持久化数据， 转换成内存中的对象。
     *
     *      Java的序列化是一个重量级序列化框架（Serializable），一个对象被序列化后，
     *      会附带很多额外的信息（各种校验信息，Header，继承体系等），不便于在网络中高效传输。
     *
     *      Hadoop序列化实质也是Java序列化，不过却是轻量级的
     *      特点：
     *          > 紧凑 ： 高效使用存储空间。
     *          > 快速： 读写数据的额外开销小。
     *          > 可扩展： 随着通信协议的升级而可升级
     *          > 互操作： 支持多语言的交互
     *
     *      自定义类序列化和反序列化
     *          > 自定义类实现Writable接口，并重写其方法
     *          > 由于反序列化需要用到类的无参构造器，所以需要有无参构造器
     *          > 重写序列化方法
     *          > 重写反序列化方法
     *          注意：反序列化的顺序和序列化的顺序完全一致
     *
     */

    /**
     *  InputFormat接口用于：验证Job的输入规范性；对输入数据进行数据切片，行成多个InputSplit文件；创建RecordReader
     *      对象，从InputSplit对象中读取数据供map使用
     *
     *  InputFormat接口中定义了两个方法getSplits()和getRecordReader()，分别用于对数据进行切片和读取切片数据
     *  InputFormat接口本身有21个实现类，getSplits()和getRecordReader()分别有10和20个实现方法
     *
     *  在其实现类中有一些较为常用的API需要了解
     *
     *  以实现类FileInputFormat为例，虽然是一个抽象类，但也具有较多方法能够使用
     *      常用的API有两个，一个是设置数据的输入路径，一个是获取切片的数据信息
     *      1、设置文件的输入路径：
     *          > public static void addInputPath(JobConf conf, Path path)
     *              FileInputFormat.addInputPath(job, newPath("Path1")); //设置一个源路径
     *
     *          > public static void addInputPaths(JobConf conf, String commaSeparatedPaths)
     *              FileInputFormat.addInputPaths(job," Path1, Path2,..."); //设置多个源路径，多个源路径之间用逗号分开
     *
     *          > public static void setInputPaths(JobConf conf, String commaSeparatedPaths)
     *              FileInputFormat.setInputPaths(job,new Path(“path1”), new Path(“path2”),…); //可以包含多个源路径，
     *
     *          > public static void setInputPaths(JobConf conf, Path... inputPaths)
     *              FileInputFormat.setInputPaths(job,”Path1”,” Path2,..."); //设置多个源路径，多个源路径之间用逗号分开
     *
     *      2、获取切片信息的API
     *          > public static Path[] getInputPaths(JobConf conf) 返回切片数据的信息
     *
     */

    /**
     *  Mapper编程范式：
     */

    /**
     *  shuffle阶段
     *  1、设置Partitioner进行数据分区：设置分区的本质是实际业务需求，设置ReduceTask的数量，进而在Mapper阶段进行分区
     *      MapReduce默认分区是按照数据的key值的hashCode对ReduceTask取摸获得的，ReduceTask的默认数量是1
     *
     *     自定义设置分区步骤：
     *          > 自定义分区类继承Partitioner类，或者实现Partition接口，并重写getPartition()方法
     *              public abstract int getPartition(KEY var1, VALUE var2, int var3)
     *                  其中，var1，var2，var3分别表示输入数据的key值，value值和分区数量
     *                  分区是从0开始
     *
     *         > 在Job驱动中，指定使用自定义的分区实现类
     *              job.setPartitionerClass(CustomPartitioner.class)
     *         > 在Job驱动中，指定分区的数量
     *              job.setNumReduceTasks(5);
     *
     *      分区数量与ReduceTask数量的关系
     *          当分区数量 < ReduceTask数量    会产生几个空载的ReduceTask任务和相应数量的空输出文件
     *          当分区数量 > ReduceTask数量    不允许这种情况出现，实际也不会出现，如果出现会报错
     *          如果ReduceTask的数量=1， 则不管MapTask端输出多少个分区文件， 最终结果都交给这一个ReduceTask，
     *              最终也就只会产生一个结果文件。
     *              当ReduceTask的数量为1时，底层源码中，MapReduce并不会执行自定义的分区逻辑，会有一个逻辑判断
     *
     *  2、排序；
     *          在MapReduce过程中能够排序的地方有三处：
     *              > map的溢写阶段：根据分区以及key进行快速排序
     *              > map的合并溢写文件：将同一个分区的多个溢写文件进行归并排序，合成大的溢写文件
     *              > reduce输入阶段：将同一分区，来自不同map task的数据文件进行归并排序
     *
     *          默认排序方式：按照数据的key值进行升序排序，实现算法是快排
     *              需要说明的是，Hadoop的排序都是按照key排序的，所以需要使用哪个字段排序就将哪个字段设置成key
     *
     *          排序的分类：
     *              部分排序：MapReduce根据输入记录的键对数据集排序。保证输出的每个文件内部有序。
     *                  需要说明的是：MapReduce每有一个分区就会有一个输出文件，所以为了进行部分排序，需要指定分区
     *
     *              全排序：最终输出结果只有一个文件， 且文件内部有序。 实现方式是只设置一个ReduceTask。
     *                  但该方法在处理大型文件时效率极低， 因为一台机器处理所有文件， 完全丧失了MapReduce所提供的并行架构
     *
     *              辅助排序：在Reduce端对key进行分组。应用于：在接收的key为bean对象时，
     *                  想让一个或几个字段相同（全部字段比较不相同）的key进入到同一个reduce方法时，可以采用分组排序。
     *
     *              二次排序：也称为二级排序。在自定义排序过程中，如果compareTo中的判断条件为两个即为二次排序。
     *
     *          Hadoop排序实现方式：
     *              方式一：自定义序列化类，实现WritableComparable接口，并重写其方法
     *              方式二：自定义比较器继承WritableComparator类，重写其方法，并指定
     *
     *
     *
     *
     *  3、Combiner组件：对具有相同key值的<k-v>数据合并成一组数据
     *      能够进行数据合并的地方有两处：
     *          > 一个MapTask任务在硬盘上写入的一个临时文件，在其内部可以进行数据的合并
     *          > 多个MapTask任务在硬盘上写入的多个临时文件，在向ReduceTask传输之前可以多个文件进行合并
     *          合并就是具有相同key值的多条数据合并成一条数据
     *
     *      Combiner是一个可选的操作，只有当合并数据不会对业务逻辑有影响才能合并数据
     *      合并数据会减小网络传输的压力
     *
     *      Combiner的实现：
     *          需要说明的是，Combiner没有默认的实现类或接口，在创建Combiner组件时，创建的实现类需要继承Reducer类，并重写其方法
     *
     *          实现步骤：
     *              > 自定义一个 Combiner 继承 Reducer，重写 Reduce 方法
     *              > 在 Job 驱动类中设置Combiner实现类：
     *                  job.setCombinerClass(WordcountCombiner.class)
     *
     */

    /**
     *  OutputFormat阶段
     *      |----OutputFormat(接口或者抽象类，定义了两个抽象方法，getRecordWriter() 和 checkOutputSpecs())
     *          |----FileOutPutFormat(抽象类，实现了checkOutputSpecs())
     *              |---TextOutputFormat() 实现了getRecordReader()方法
     *
     *      在自定义输出时，一般不直接实现OutputFormat接口（抽象类），而是继承FileOutputFormat，原因是
     *      FileOutputFormat抽象类实现了checkOutputSpecs()方法
     *
     *      自定义输出结果：
     *          1、自定义类继承FileOutputFormat，重写getRecordReader()方法，
     *              getRecordReader()方法返回值为RecordWriter接口的实现类对象，
     *              因此还需要创建实现类实现RecordWriter类，并重写write()和close()方法
     *
     *          2、在Job中指定OutputFormat的实现类
     *              job.setOutputFormatClass(LogOutputFormat.class);
     *
     *          3、因为自定义的OutputFormat实现类是直接继承于FileOutputFormat的，
     *              而 fileoutputformat 要输出一个_SUCCESS 文件，所以在这还得指定一个输出目录
     *              FileOutputFormat.setOutputPath(job, new Path("D:\\logoutput"));
     *
     */
}
