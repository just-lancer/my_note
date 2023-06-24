package mapreduceknowledge;

public class MapReduce {
    /**
     *  MapReduce学习
     *  一、MapReduce概述
     *      MapReduce是一个分布式运算程序的编程框架，是Hadoop的数据分析应用的核心框架。
     *      MapReduce核心功能是将用户编写的业务逻辑代码和自带默认组件整合成一个完整的分布式运算程序，并行运行在一个Hadoop集群上。
     *      优点：[
     *          简单，易于编程：它简单的实现一些接口，就可以完成一个分布式程序
     *          良好的扩展性：当计算资源不足时，可以通过增加机器来扩展它的计算能力
     *          高容错性：MapReduce设计的初衷就是使程序能够部署在廉价的PC机器上，这就要求它具有很高的容错性。
     *          比如其中一台机器挂了，它可以把上面的计算任务转移到另外一个节点上运行，不至于这个任务运行失败，
     *          而且这个过程不需要人工参与，而完全是由Hadoop内部完成的。
     *          适合PB级以上海量数据的离线处理：可以实现上千台服务器集群并发工作，提供数据处理能力
     *
     *      缺点：
     *          不擅长实时计算：MapReduce无法像MySQL一样，在毫秒或者秒级内返回结果。
     *          不擅长流式计算：流式计算的输入数据是动态的，而MapReduce的输入数据集是静态的，不能动态变化。
     *              这是因为MapReduce自身的设计特点决定了数据源必须是静态的。
     *          不擅长有向无环图(DAG)计算：多个应用程序存在依赖关系，后一个应用程序的输入为前一个的输出。
     *          在这种情况下，MapReduce并不是不能做，而是使用后，每个MapReduce作业的输出结果都会写入到磁盘，
     *          会造成大量的磁盘IO，导致性能非常的低下。
     *
     * 二、MapReduce核心思想及流程
     *      MapReudce数据处理核心思想
     *          将数据按一定规则分发到各个处理组件中进行处理，将处理完的数据再组合到一起，然后输出
     *      在MapReduce处理数据过程中涉及三个核心进程：
     *          > MapTask进程：负责Map阶段的数据处理
     *          > ReduceTask进程：负责Reduce阶段的数据处理
     *          > MrAPPMaster进程：负责整个程序的过程调度和状态协调
     */

    /*      MapReduce数据处理核心流程：
     *数据源 --- [InputFormat] ---> Mapper ---[shuffle/Mapper] ---[shuffle/Reducer] ---> Reducer --- [OutputFormat] ---> 结果文件
     *
     *      核心流程包含两个核心节点(Mapper/Reducer)和四个主要处理流程(InputFormat、shuffle/Mapper、shuffle/Reduce、OutputFormat)
     */

    /*      1、InputFormat：是一个接口，其主要作用是验证job的输入规范，并对输入的数据块进行相应处理，以供MapTask进程使用
     *          InputFormat接口有两个抽象方法：
     *              InputSplit[] getSplits(JobConf var1, int var2) throws IOException
     *              RecordReader<K, V> getRecordReader(InputSplit var1, JobConf var2, Reporter var3) throws IOException
     *          其中getSplit()方法会对输入数据进行切片，getRecordReader()方法会从切片的数据中按一定方式读取数据，并交由Mapper阶段处理
     *
     *          InputFormat实现结构，有21个实现类
     *          |----InputFormat
     *              |----FileInputFormat(抽象类，有9个实现类)
     *                  |----TextInputFormat(InputFormat的默认实现类)
     *              |----TextInputFormat
     *              |----DBInputFormat
     *              |----CombineFileInputFormat(抽象类)
     *              |----CombineTextInputFormat
     *              |----KeyValueTextInputFormat
     *              |----......
     *
     *          getSplit()方法，定义了数据切片的方式，切片对象是HDFS存储的数据块
     *              数据块：Block是HDFS在物理上把数据分成一块一块。数据块是HDFS存储数据单位
     *              数据切片：数据切片只是在逻辑上对输入进行分片，并不会在磁盘上将其切分成片进行存储。
     *                       数据切片是 MapReduce程序计算输入数据的单位，一个切片会对应启动一个 MapTask
     *
     *              切片时并不是针对整体的数据集，而是以文件为单位，逐个文件进行切片的
     *
     *              以FileInputFormat类为例，介绍文本文件数据切片做了哪些事
     *              > ① 设置，是否忽略目录下子目录中的文件
     *              > ② 设置切片的大小，切片大小默认为数据块大小，也可以自行设置
     *              > ③ 判断文件是否可以切分，有的压缩文件并不支持切分。当文件不可切分时，不论文件的大小，都当作一个文件进行处理
     *              > ④ 剩余数据块的处理，剩余数据块的方法：当剩余数据块大小大于1.1倍的切片大小，那么将剩余数据块再切分成两块数据
     *                      当剩余数据块大小小于1.1倍的切片大小，那么将剩余数据块作为一个数据切片进行处理，以降低系统资源使用
     *
     *              数据切片完成后，数据的切片信息会存储在InputSplit中，该文件记录了切片的元数据信息。
     *
     *          值得说明的是，InputFormat的实现类FileInputFormat是一个抽象类，其中实现了InputFormat接口中的getSplit()方法，
     *          而没有实现InputFormat接口中的RecordReader()方法，因此FileInputFormat没有规定文件读取的方式
     *
     *          InputFormat的默认实现类是FileInputFormat，而FileInputFormat的默认继承类是TextInputFormat，其中实现了getRecordReader()方法
     *
     *          getRecordReader()方法，定义了以怎样的方式从切片中读取一条记录，每读取一条记录都会调用RecordReader类
     *              TextInputFormat类中实现了getRecordReader()方法，规定，以行偏移量为键，以整行数据为值，进行数据读取
     *              其中键的数据类型是：LongWritable；值的数据类型为：Text
     *
     *          另一个InputFormat的实现类：CombineTextInputFormat
     *          CombineTextInputFormat类中实现了getSplit()方法和getRecordReader()方法，其中，getRecordReader()方法实现与FileInputFormat类相同
     *              而getSplit()方法规定的文件切片方式不同。CombineTextInputFormat类改变了传统的切片方式，将多个小文件，
     *              划分到一个切片中，适合小文件过多的场景。
     *
     *          CombineTextInputFormat类的切片过程：（针对小文件处理）
     *              虚拟存储最大值：CombineTextInputFormat.setMaxInputSplitSize(job, 4194304) // 4M
     *              第一个过程：虚拟存储过程：
     *                  > 将输入目录下所有文件的大小，依次和设置的setMaxInputSplitSize值比较，如果不大于设置的最大值，逻辑上划分一个块。
     *                      如果输入文件大于设置的最大值且大于两倍，那么以最大值切割一块；当剩余数据大小超过设置的最大值且不大于最大值2倍，
     *                      此时将文件均分成2个虚拟存储块（防止出现太小切片）
     *                  > 判断虚拟存储的文件大小是否大于setMaxInputSplitSize值，大于等于则单独形成一个切片。
     *                      如果不大于则跟下一个虚拟存储文件进行合并，共同形成一个切片
     */



    /*  2、Mapper阶段
     *      Mapper阶段会产生MapTask进程，主要对InputFormat传入的切片数据根据业务需求进行数据处理，一个切片会产生一个MapTask任务
     *
     *      Mapper阶段业务逻辑处理编程范式：
     *          > 用户自定义的Mapper类继承Mapreduce提供的Mapper类，并重写map()方法，将业务逻辑写入方法体中
     *          > 对于输入数据和输出数据的数据类型说明：
     *              > 在实际的业务场景中，处理最多的数据是文本文件，因此，InputFormat接口使用最多的类是
     *                  FileInputFormat、TextInputFormat、CombineTextInputFormat，所以
     *                  输入数据的键，类型为LongWritable（固定的不可变），输入数据的值是Text
     *                  输出数据的键和值可以根据业务需求自定义
     *          需要说明的是，在Mapper阶段，每一行（也就是每一对输入的<K,V>）数据的输入，MapTask进程都会调用一次map()方法
     */

    /*  3、shuffle/Mapper阶段和shuffle/Reducer阶段
     *      Mapper阶段输出的数据需要交给Reducer阶段进行处理，而在这一过程中涉及到数据整合、写入磁盘、从磁盘拉取数据、对数据再整合等步骤。
     *      我们将这一中间过程称为shuffle阶段（重新洗牌），shuffle阶段包含shuffle/Mapper和shuffle/Reducer阶段。
     *
     *      shuffle过程：
     *          先验了解：一个数据切片会产生一个MapTasK进程任务，一个MapTask对应有一个环形缓冲区
     *                   MapTask进程任务对切片数据的处理是一行一行进行处理的，因此处理完的数据也是一行一行写入到环形缓冲区的
     *
     *          具体过程：
     *      *********************************以下是shuffle/Reducer阶段**********************************************************************************************************************
     *
     *              > Mapper阶段处理后的数据，一行一行写入环形缓冲区。
     *
     *              > 环形缓冲区是一段内存空间，默认大小是100M，其中记录了每一行数据<k,v>和其分区信息，当环形缓冲区容量达到80%时，
     *                  会一次性将数据写出到磁盘中，在写出之前还会对其中的数据进行分区和排序。环形缓冲区的作用就是减少IO次数，提高效率。
     *                  数据分区：分区的数量决定了Reducer阶段的ReducerTask的数量，其实质是，根据业务需求人为设置ReducerTask的数量，进而决定分区数
     *                  数据排序：为了提升shuffle/Mapper阶段最后写入磁盘时的效率以及Reducer阶段拉取数据的效率，默认按照数据的键进行升序排序，排序算法是快排
     *
     *              > 需要说明的是：环形缓冲区向磁盘写出一次数据，并不是一个切片数据都处理完之后的数据，因为环形缓冲区向磁盘写入一次数据默认只有80M，
     *                  而一个切片数据默认有128M，经过Mapper阶段处理后，也不一定是80M，因此一个切片数据在处理后，经过环形缓冲区向磁盘中写入，
     *                  要么生成一个文件，要么生成两个文件、或者多个文件。在一个切片数据被完全处理并写入磁盘中后，MapReduce还会对这些文件（若有）进行归并排序，
     *                  形成一个文件，即一个切片数据在经过Mapper阶段处理，shuffle/Mapper阶段写入磁盘后，只会形成一个文件，文件中的数据是有序并且是分区存放的。
     *                  [其中还有个可选过程：Combiner，Combiner能够对一个被Mapper和shuffle/Mapper流程处理后的切片数据生成的文件进行合并，
     *                   即，具有相同key值的一组values]
     *
     *              > [在所有切片数据都经过Mapper阶段和shuffle/Mapper阶段处理后，在硬盘上会形成与切片数量相同的临时文件。]
     *
     *                  值得说明的是：在HDFS中，数据是按照数据块进行存储的，一个数据块大小为128M。在MapReduce中，一个MapTask任务只处理一个数据切片，
     *                  数据切片的大小和数据块的大小相同。
     *
     *                  因此，在MapTask和shuffle/MapTask的过程中有两个地方能够对数据进行Combiner，一个地方是：当一个数据切片被处理完之后，会生成
     *                  若干个文件，在文件的内部可以进行数据合并，而文件与文件也能够进行合并，留下一个文件。
     *
     * ********************************以下是shuffle/Reducer阶段**********************************************************************************************************************
     *              > 数据经过数据切片和Mapper阶段处理之后，会在硬盘中产生对应的文件，一个MapTask任务产生一个文件这些文件作为Reducer阶段的输入数据，
     *                  需要被拷贝到ReduceTask所在机器上
     *              > ReduceTask根据自己所要处理的分区数据，去各个MapTask所在的机器上，将相应分区的数据拷贝到本地内存中，
     *                  如果某一MapTask的文件过大时，ReduceTask所在机器的内存存不下，那么会将数据写入到硬盘中
     *              > 在远程拷贝数据的同时，ReduceTask启动了两个后台线程对内存和磁盘上的文件进行合并，以防止内存使用过多或磁盘上文件过多
     *              > 当ReduceTask所要处理的分区的数据全部拷贝到本地后，ReduceTask会对数据进行一次归并排序，形成一个对应分区的大的文件，
     *
     *              此时，shuffle阶段结束。进入Reducer阶段
     *
     */

    /**
     *  4、Reducer阶段
     *      Reducer阶段会产生ReduceTask进程，主要对Mapper阶段输出的数据根据业务需求进行数据处理，一个分区会产生一个ReduceTask任务
     *
     *      Reducer编程范式：
     *          > 编写自定义Reducer类，继承Reducer类，并重写reduce()方法，将业务逻辑写入方法中
     *          需要注意的是，reduce()方法的输入数据是Mapper阶段输出的数据，因此reduce()方法的输入数据类型与map()方法的输出数据类型要对应
     *          ReduceTask会对每一组具有相同key的<k,v>执行一次reduce()方法
     */

    /**
     *  5、OutputFormat阶段
     *      hadoop中，旧版的OutputFormat是接口，新版的OutputFormat是抽象类
     *      而InputFormat一直都是接口
     *
     *      OutputFormat用于将Reducer阶段的输出数据按一定方式写入到硬盘中
     *      OutputFormat抽象类有10个继承类，较为常用的是FileOutputFormat、TextOutputFormat和SequenceFileOutputFormat
     *
     *      OutputFormat抽象类中定义了三个抽象方法：
     *      > public abstract RecordWriter<K, V> getRecordWriter(TaskAttemptContext var1)
     *          throws IOException, InterruptedException
     *          getRecordWriter()方法用于返回一个RecordWriter的实例，Reduce任务在执行的时候就是利用这个实例来输出Key/Value的。
     *          （如果Job不需要Reduce，那么Map任务会直接使用这个实例来进行输出。）
     *
     *          RecordWriter有如下两个方法：
     *              void  write(K key, V value) 负责将Reduce输出的Key/Value写成特定的格式
     *              void  close(TaskAttemptContext context) 负责对输出做最后的确认并关闭输出
     *
     *      > public abstract void checkOutputSpecs(JobContext var1)
     *          throws IOException, InterruptedException
     *          该方法在JobClient提交Job之前被调用的（在使用InputFomat进行输入数据划分之前），用于检测Job的输出路径。
     *          会对输入的路径进行是否为空，是否已存在的判断
     *
     *      > public abstract OutputCommitter getOutputCommitter(TaskAttemptContext var1)
     *          throws IOException, InterruptedException
     *          getOutputCommitter()方法则用于返回一个OutputCommitter的实例，OutputCommitter用于控制Job的输出环境
     *
     *      FileOutputFormat也是一个抽象类，它实现了 checkOutputSpecs()和 getOutputCommitter()方法
     *
     *      TextOutputFormat：继承了FileOutputFormat类，自身实现了getRecordWriter()方法
     *          TextOutputFormat类是作为OutputFormat的默认实现类，将Reducer阶段的输出结果以文本行的方式输出
     *
     *      SequenceFileOutputFormat：一般用于后续还有MapReduce流程的数据输出，其输出数据会作为Mapper阶段的数据输入，
     *          其输出结果格式紧凑、容易被压缩
     *
     *      自定义输出格式：
     *          > 自定义输出格式类，并继承FileOutputFormat，并重写getRecordReader()方法，由于该方法需要返回一个RecordReader对象，
     *              因此还需要定义一个类继承RecordReader类（这是一个抽象类），并重写其中的write()方法和close()方法。
     *              wirte(K,V)方法的输入即是Reducer阶段的输出，用于将输入的K-V以自己指定的方式输出到自己指定的位置。
     *              close()方法用于关闭流
     *
     */

}
