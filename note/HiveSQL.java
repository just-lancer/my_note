package hive;

public class HiveSQL {
    /**
     *  HiveSQL语法
     *  1、Hive的数据类型
     *  2、Hive：DDL语句
     *      2.1、数据库相关操作
     *      2.2、表的相关操作
     *  3、Hive：DML语句
     *      3.1、数据导入
     *      3.2、数据导出
     *      -- 表数据删除
     *  4、Hive：查询
     *      4.1、基本查询：单表查询
     *      4.2、复杂查询：多表连接及联结
     *      4.3、Hive SQL 运算符
     *      4.4、聚合函数
     *  5、分区表和分桶表
     *  6、常用函数
     *      6.1 函数命令
     *      6.2 数值函数
     *      6.3 字符串函数
     *      6.4 日期函数
     *      6.5 条件函数
     *      6.6 窗口函数
     *      6.7 自定义函数：略
     *
     *  7、文件压缩和文件存储
     *      7.1 文件压缩
     *      7.2 文件存储
     *
     */

    /**
     *  1、Hive的数据类型：
     *  基本数据类型：
     *      > TINYINT   1byte有符号整数
     *      > SMALINT   2byte有符号整数
     *      > INT       4byte有符号整数
     *      > BIGINT    8byte有符号整数
     *      > BOOLEAN   布尔类型，true或者false
     *      > FLOAT     单精度浮点数
     *      > DOUBLE    双精度浮点数
     *      > STRING    字符系列。可以指定字符集。可以使用单引号或者双引号。
     *      > TIMESTAMP 时间类型
     *      > BINARY    字节数组
     *      说明：Hive的String类型相当于数据库的varchar类型，是一个可变的字符串，不过它不能声明其中最多能存储多少个字符，
     *           理论上它可以存储2GB的字符数。
     *  集合数据类型：
     *      > STRUCT
     *      > MAP：例如map<string, int>
     *          MAP是一组键-值对元组集合，使用数组表示法可以访问数据。例如，如果某个列的数据类型是MAP，
     *          其中键->值对是’first’->’John’和’last’->’Doe’，那么可以通过字段名[‘last’]获取最后一个元素
     *      > ARRAY：例如array<string>
     *          数组是一组具有相同类型和名称的变量的集合。这些变量称为数组的元素，每个数组元素都有一个编号，编号从零开始。
     *          例如，数组值为[‘John’, ‘Doe’]，那么第2个元素可以通过数组名[1]进行引用。
     *
     *  数据类型的转换：
     *      隐式转换规则：
     *          任何整数类型都可以隐式地转换为一个范围更广的类型，如TINYINT可以转换成INT，INT可以转换成BIGINT
     *          所有整数类型、FLOAT和STRING类型都可以隐式地转换成DOUBLE
     *          TINYINT、SMALLINT、INT都可以转换为FLOAT
     *          BOOLEAN类型不可以转换为任何其它的类型
     *      强制类型转换：使用cast函数，语法：cast(被转换的值 as 目标类型)
     *          例如：cast('1' as int)，将字符串'1'强制转换成整数类型int
     *          需要说明的是：当转换失败时，cast函数的返回值是NULL
     */

    /**
     * 2、Hive：DDL操作
     *  说明：以下将未定数据库名统一定为db_name，未定表名统一定为tb_name
     *  2.1、数据库相关操作
     *  ① 创建数据库：
     *      CREATE DATABASE [IF NOT EXISTS] db_name  -- [IF NOT EXISTS] 判断当前库是否存在（不建议加...）
     *      [COMMENT db_comment]  -- 针对当前库的描述信息
     *      [LOCATION hdfs_path]  -- 指定当前库对应HDFS中的具体位置（通常不加，走默认路径）
     *      [WITH DBPROPERTIES (property_name=property_value, ...)];  -- 相当于添加该数据库的注解信息，作者是谁，创建时间是什么等等
     *      说明：创建数据库，在HDFS上的默认位置是：/user/hive/warehouse/db_name.db，也就是hive-site.xml中配置的位置
     *
     *  ② 查看所有数据库：show databases;
     *  ③ 查看数据库信息：desc database db_name;
     *  ④ 查看数据库的详细信息：desc database extended db_name;
     *  ⑤ 切换数据库：use db_name;
     *
     *  ⑥ 修改数据库信息：alter关键字
     *      说明：数据库能够被修改的信息只有其注解信息，即dbpropertie定义的信息
     *      语法格式：alter databases db_name set dbproperties(key = value);
     *      key和value指的是需要添加的注解信息，当key已经存在时，那么就是修改注解信息；如果不存在，那么就是新增注解信息
     *
     *  ⑦ 删除数据库：drop database [if exists] db_name [cascade];
     *      cascade关键字，用于强制删除数据库，无论数据库中是否有数据
     *
     *  2.2、表的相关操作
     *  ① 创建表：
     *      CREATE [EXTERNAL] TABLE [IF NOT EXISTS] tb_name     -- [EXTERNAL]:指定当前表的类型为外部表，如果不加默认是内部表（管理表）
     *      [(col_name data_type [COMMENT col_comment], ...)]   -- 指定当前表的字段以及字段的数据类型，当时根据查询结果复制一张表的时候，可以不加
     *      [COMMENT table_comment]     -- 针对当前表的描述信息
     *      [PARTITIONED BY (col_name data_type [COMMENT col_comment], ...)]    -- 创建分区表的关键字
     *      [CLUSTERED BY (col_name, col_name, ...) INTO num_buckets BUCKETS]   -- 创建分桶表的关键字
     *      [SORTED BY (col_name [ASC|DESC], ...)]  -- 针对当前表指定默认的排序字段（通常情况不建议加）
     *
     *      [row format delimited fields terminated by "xxxx"]  -- 指定当前行数据字段对应的值之间的分割符
     *      [collection items terminated by "xxxx"]             -- 指定集合数据中的元素之间的分割符
     *      [map keys terminated by "xxxx"]             -- 指定map结构中的key和value的分割符
     *      [lines terminated by "\n"]          -- 指定每一行数据的之间分割符
     *
     *      [STORED AS file_format]     -- 指定当前表的存储文件的格式，默认是textfile(纯文本)
     *      [LOCATION hdfs_path]    -- 指定当前表对应HDFS中的具体位置（通常不加，走默认路径）
     *      [TBLPROPERTIES (property_name=property_value, ...)]     -- 针对当前表的结构化描述信息
     *      [AS select_statement]   -- 当复制一张表的加此项内容
     *
     *
     *      字段说明：
     *      create table tb_name;  -- 建表基本语句
     *      if not exists   -- 当表不存在时，创建表。如果表已经存在，则抛出异常；
     *      external -- hive默认创建内部表，添加external关键字，创建外部表。删除表时，内部表的元数据和数据会被一起删除；外部表只删除元数据，不删除数据
     *      comment -- 为表或者表的字段添加说明信息，在建表字段后面添加，为字段添加说明信息；在建表语句后面添加comment语句，为表添加说明信息
     *      partitioned by -- 为表添加分区字段，也是创建分区表
     *      clustered by -- 创建分桶表
     *      sorted by -- 根据表的某一字段对标进行排序，即表存储的数据是已排好序的
     *
     *  ② 修改表：alter关键字
     *      -- 修改表名：alter table tb_name rename to new_tb_name;
     *      -- 修改列名及列数据类型：alter table tb_name change old_col_name new_col_name col_type [comment col_comment] [first/after col_name]
     *      -- 修改列名：alter table tb_name rename old_col_name new_col_name
     *      -- 增加列：alter table tb_name add columns col_name col_type [comment col_comment] [first/after col_name] ...
     *      -- 删除列：alter table tb_name drop [column] col_name
     *      说明：rename关键字只能修改列的名字，而change关键字既能修改列的名字，还能修改其他信息，和创建了一个新的列一样
     *          增加列，column要加s，删除列column不需要加s，
     *
     *  ③ 删除表：drop table tb_name; 连数据带表结构一起删除
     *
     *  ④ 查看表字段信息：desc tb_name;
     *  ⑤ 查看表字段信息及元数据存储路径：desc extended tb_name; 或者 desc formatted table_name;
     *  ⑥ 查看表创建信息：show create table tb_name;
     *
     */

    /**
     *  3、Hive：DML操作
     *  3.1、数据导入
     *      3.1.1 向表中导入数据：load
     *          语法结构：load data [local] inpath 'filepath' [overwrite] into table tb_name [partition (partcol1=val1,…)];
     *          结构说明：
     *              > load data：表示加载数据
     *              > local：load data默认从HDFS加载数据到hive表中，local表示从本地加载数据
     *              > inpath：表示数据的加载路径
     *              > overwrite：load data添加数据，默认是在已有的表中追加，overwrite表示覆盖原有的数据
     *              > into table tb_name：指定需要添加数据的表
     *
     *      3.1.2 利用insert语句向表中插入数据：
     *          方式一：单条数据或多条数据插入
     *              语法结构：insert into table tb_name [(字段列表)] values(值列表);
     *          方式二：将一个select语句的查询结果作为表的数据插入
     *              语法结构：insert into table tb_name 字段列表
     *                       select 字段列表1 from tb_name1;
     *          说明：
     *              ① into字段表示以追加的方式添加数据，而overwrite表示以覆盖的方式添加数据
     *              ② 将一个select语句的查询结果作为结果进行数据插入时，select的查询结果字段必须与表的定义字段的数量和数据类型一致
     *
     *      3.1.3 创建表时，利用location关键字指定数据加载路径
     *          语法结构   create table tb_name location filepath;
     *          说明：location的效果和external有一定关系。
     *              对于外部表，建表时，会从指定的路径下读取数据，删表时，却不会删除路径下的数据；
     *              对于内部表，建表时，会从指定的路径下读取一份数据，并备份一份数据，删除时，会将备份的数据一并删除，原数据不删
     *
     *  3.2 数据导出
     *      3.2.1 将查询结果导出到本地/HDFS：
     *          语法结构：insert overwrite [local] dirertory filepath
     *                   row format delimited fields delimited by separator
     *                   select * from tb_name;
     *
     *      3.2.2 export import
     *          export：将hive数据导出到HDFS
     *          import：将HDFS数据导入到hive
     *          语法结构：
     *              > export table tb_name to hdfs_path;
     *              > import table tb_name from hdfs_path;
     *              导入和导出是以Hive表作为参考的
     *
     *      -- 表数据删除：truncate table tb_name;
     *          说明：truncate只能对内部表进行操作，删除只删除数据，不删除表结构
     *
     *          hive是一个数据仓库工具，擅长select数据，并不删除update和delete数据
     */

    /**
     *  4、Hive：查询
     *  4.1、基本查询：单表查询
     *      select 字段1 as new_name1, 字段2 as new_name2, ...
     *      from tb_name
     *      where condition11, condition12, ...
     *      group by field1, field2, ...
     *      having condition21,condition22, ...
     *      [distribute by field]
     *      order by/sort by  filed1 [asc/desc], field2 [asc/desc], ...
     *      limit num;
     *
     *      说明：
     *          ① order by不能和limit一起使用
     *          ② limit语句中，没有offset，因此hive只能限定查询结果展示多少条，不能控制从哪开始查
     *          ③ distribute by也能够对多字段进行分区，但一般不进行多字段分区
     *
     *      排序的说明：
     *          排序方式：升序--asc；降序--desc
     *          多级排序：添加多个排序字段
     *          > order by：全排序，只能有一个ReduceTask
     *          > sort by：分区内排序。为每一个ReduceTask任务进行排序
     *          > distribute by：分区，distribute by指定的字段为ReduceTask的分区字段。分区的方式是根据分区字段的hashcode值对分区数取模得到
     *          > cluster by：根据指定字段进行分区和排序，即当sort by和distribute by所指定的字段相同时，可以使用cluster by
     *          需要说明的是：cluster by只能进行升序排序
     *
     *  4.2、复杂查询：多表连接及联结
     *      表连接方式：
     *          > 交叉连接（笛卡尔积）: A [cross] join B
     *          > 内连接：A inner join B
     *          > 左外连接：A left join B
     *          > 右外连接：A right joinB
     *          > 全外连接：A full outer join B
     *      表连接的7种结果：略
     *
     *      表的联结：union和union all
     *      二者都能够对查询结果进行联结，不同的是union会对联结结果进行去重，而union all不会去重，因此union all效率高
     *
     *  4.3、Hive SQL 运算符
     *      算术运算符：+、-、*、/、% ---- 加减乘除、取模
     *      比较运算符：
     *          支持基本数据类型的
     *          > A = B
     *          > A <=> B   -- 如果A和B都为NULL，则返回TRUE，如果一边为NULL，返回False，主要是针对NULL值的比较，其他的和=没有区别
     *          > A <> B, A != B
     *          > A < B
     *          > A <= B
     *          > A > B
     *          > A >= B
     *          > A [NOT] BETWEEN B AND C
     *
     *          支持全部数据类型
     *          > A IS NULL
     *          > A IS NOT NULL
     *          > IN(数值1, 数值2)
     *
     *          支持字符串类型
     *          > A [NOT] LIKE B  -- 模糊查询，占位符'_'代表任意一个字符；'%'代表0个或者任意多个字符
     *          > A RLIKE B, A REGEXP B  -- 正则表达式查询
     *
     *      逻辑运算符：
     *          > and 与
     *          > or 或
     *          > not 非
     *
     *  4.4聚合函数
     *      count()：统计行数。根据参数不同会有不同的结果，当参数是'*'时，连带null一起统计；当参数是其他值时，不统计null
     *      sum()：求和，只支持数值类型数据
     *      avg()：求平均值，只支持数值类型数据
     *      max()：求最大值，支持所有数据类型
     *      min()：求最大值，支持所有数据类型
     *
     */

    /**
     *  5、分区表和分桶表
     *  概述：对Hive的表进行分类，可以分成普通表，分区表和分桶表。分区和分桶表的目的是对数据存储和查询的一种优化。
     *  5.1 分区表
     *      分区表的本质是：分目录存储数据
     *      分区表的基本操作：
     *          > 创建分区表：create table tb_name (field1 data_type1, field1 data_type1, ...)
     *                       partitioned by (field3 data_type3, field4 data_type4, ...)
     *                       row format delimited fields delimited by separator;
     *                       注意：分区字段不能与普通字段重复
     *          > 向分区表中添加数据：导入数据时必须指明分区（名）
     *                  load data [local] filepath [overwrite] into table tb_name partition(partitionname = name);
     *          > 查询分区表中的数据：严格模式下，必须指明分区字段；非严格模式下，建议指明分区字段。
     *                  select * from tb_name where partitionname = value;
     *          > 增加分区：alter table tb_name add partition(partitionname1 = name1) partition(partitionname2 = name2) ...
     *          > 删除分区：alter table tb_name drop partition(partitionname1 = name1),partition(partitionname2 = name2) ...
     *
     *          > 查看分区表的分区信息：show partitions tb_name;
     *          > 查看分区表的表结构：desc formatted tb_name;
     *
     *      补充：
     *      > 分区表的二级分区：建表时，指定两个分区字段即可
     *      > 把数据直接上传到分区目录上，让分区表和数据产生关联的三种方式：
     *          手动上传数据到分区目录中，Hive分区表和数据不会直接产生关联，需要进行相关操作使其关联
     *          方式一：上传数据后，手动修复，执行：msck repair tb_name;
     *          方式二：上传数据后，手动创建分区：alter table tb_name add partition(partitionname = name);
     *          方式三：创建文件夹之后，将数据load到目录中
     *      > 动态分区：动态分区过程，发生在利用select查询结果为分区表添加数据时。此时，分区表的字段必须与select查询结果字段一一对应，
     *                  而分区表的分区字段对应的select查询结果的字段即为分区字段。
     *          值得说明的是：在严格模式下，分区表必须至少有一个分区，才能进行动态分区
     *
     *  5.2 分桶表
     *      分桶表的本质是：分文件存储数据。分桶表也是对分区表的不足做出了补偿。分区表的分区字段一般都是具有一定规律的对于完全没有规律的数据，
     *                      分区表就不再适用，原因是容易造成数据倾斜。
     *      分桶表的分桶规则：对指定的分桶字段，进行hash值对分桶数取模进行分桶的
     *      分桶表基本操作；
     *          > 创建分桶表：create table tb_name (field1 data_type1, field1 data_type1, ...)
     *                       clustered by (bucket_field) into num buckets
     *                       row format delimited fields delimited by sperator;
     *          > 向分桶表中添加数据：
     *              load data [local] filepath [overwrite] into table tb_name;
     *          > 查看分桶表数据：普通的select语句即可
     *          > 查看分桶表的结构：show formatted tb_name;
     *
     *      分桶表操作注意事项：
     *          > reduce的个数设置为-1,让Job自行决定需要用多少个reduce或者将reduce的个数设置为大于等于分桶表的桶数
     *          > 从hdfs中load数据到分桶表中，避免本地文件找不到问题
     *
     *  5.3 抽样查询：tablesample函数
     *      语法格式：select * from tb_name tablesample(bucket num1 out of num2 on bucket_field);
     *      说明：num1是选取哪个分桶的内容，num2是总的分桶数
     *
     */

    /**
     *  6、常用函数
     *  6.1 函数命令
     *      > 查看系统自带函数：show functions;
     *      > 查看函数用法：desc functions fc_name;
     *      > 查看函数详细用法：desc functions extended fc_name;
     *
     *  6.2 数值函数：依照Java习惯标定函数的返回值类型
     *      > bigint round(double num)   四舍五入取整
     *      > double round(double num, int x)   返回指定精度d的double，依然满足四舍五入。精度表示保留的小数位
     *      > bigint floor(double a)    向下取整，返回不大于a的最大整数
     *      > bigint ceil(double a)     向上取整，返回不小于a的最小整数
     *      > double rand([int seed])   返回一个取值为[0,1)的随机数。seed为随机种子
     *      > double exp(double)    返回自然对数e的a次方
     *      > double log10(double a)    返回以10为底的a的对数
     *      > double log2(double a)     返回以2为底的a的对数
     *      > double log(double base, double a)     返回以base为底的a的对数
     *      > double pow(double a, double p)    返回a的p次幂
     *      > double sqrt(double a)     返回a的平方根
     *      > double abs(double/int a)  返回a的绝对值
     *      > string bin(bigint a)      以字符串的形式返回a的二进制码
     *
     *  6.3 字符串函数：SQL中，字符串的索引从1开始
     *      > int length(string a)  返回字符串长度
     *      > string reverse(string a)  反转字符串
     *      > string concat(string str1, string str2, ...)  将字符串str1, str2, ...进行拼接
     *      > string concat_ws(string str_separator, string str1, string str2, ...)  带分隔符字符串拼接函数
     *      > string concat_ws(string str_separator, array<string>) 将字符串数组中的字符串用分隔符进行拼接
     *      > string substr/substring(string str1, int start[, int len])     截取字符串，当不设置截取长度时，默认截取到字符串末尾
     *      > string upper(string str)/lower(string str)   字符串大小写转换
     *      > string trim(string str)  去掉字符串两端的空格
     *      > string ltrim(string str) 去掉左侧空格
     *      > string rtrim(string str) 去掉右边空格
     *      > string repeat(string str, int n)  将字符串str重复n次
     *      > string replace(string str1, string old_str, string new_str)   用new_str替换字符串str1中的old_str字符串，并返回新的字符串
     *
     *  6.4 日期函数：
     *      说明：日期表示年月日，时间表示时分秒，时期表示年月日加时分秒
     *          日期：date     时间：time     时期：timestamp
     *      > string from_unixtime(bigint unixtime [string format])     将时间戳转换成指定格式的字符串时间
     *      > bigint unix_timestamp([string date[,string pattern]])   默认获取当前unix时间戳；当指定字符串时期以及解析格式时，可将指定格式时期解析成时间戳
     *
     *      > date current_date()   获取当前日期
     *      > timestamp current_timestamp()     获取当前日期和时间
     *
     *      > date to_date(string timestamp)    获取日期时间中的日期部分
     *      > int year(string date)     获取日期时间或日期中的年份
     *      > int month(string date)    获取日期时间或日期中的月份
     *      > int day(string date)      获取日期时间或日期中的天数
     *      > int hour(string date)      获取日期时间或日期中的小时
     *      > int minute(string date)      获取日期时间或日期中的分钟
     *      > int second(string date)      获取日期时间或日期中的秒
     *      > int weekofyear(string date)      返回当前日期是一年中的第几周
     *
     *      > int datediff(string enddate, string start date)   返回两个日期间相差的天数
     *      > string date_add(string startdate, int days)   返回开始日期startdate增加days天后的日期
     *      > string date_sub(string startdate, int days)   返回开始日期startdate减少days天后的日期
     *
     *  6.5 条件函数
     *      > T if(boolean testCondition, T valueTrue, T valueFalseOrNull)
     *      > T COALESCE(T v1, T v2, …)     返回参数中的第一个非null值，如果所有的值都为null，那么返回null
     *      > case when 表达式，有两种用法
     *        方式一：
     *        case  value
     *          when value1 then expression1
     *          when value2 then expression2
     *          ......
     *          else  expression
     *        end;
     *        说明：当value=value1时，执行expression1表达式；当value=value2时，执行expression2表达式；都不匹配时，执行expression
     *
     *        方式二：
     *        case
     *          when condition1 then expression1
     *          when condition2 then expression2
     *          ......
     *          else expression
     *        end;
     *        说明：当条件表达式condition1为真时，执行 expression1；当条件表达式condition2为真时，执行 expression2；都不为真时，执行expression
     *
     *  6.6 窗口函数
     *      针对当前离线数据分析逻辑日趋复杂，hive官方推出窗口函数用以增强Hive SQL的功能
     *
     *      需要注意的是：在SQL处理中，窗口函数都是最后一步执行，而且仅位于Order by字句之前
     *
     *      窗口函数基本语法：<窗口函数> over([partition_definition] [order_definition] [frame_definition])
     *
     *      |---- 窗口函数
     *          |---- 静态窗口函数
     *              |---- 排名函数
     *                  |---- rank() 排名：允许相同排名出现，相同排名会占据排名位，例如：1，1，3，4，4，6
     *                  |---- dense_rank() 排名：允许相同排名出现，相同排名不会占据排名位，例如：1，1，2，3，4，4，5
     *                  |---- row_number() 排名：不允许出现相同排名，例如：1，2，3，4，5
     *          |---- 滑动窗口函数
     *              |---- 聚合函数
     *                  |---- count()
     *                  |---- sun()
     *                  |---- avg()
     *                  |---- max()
     *                  |---- min()
     *              |---- 特殊窗口函数
     *                  |---- first_value() 返回分组内第一个值
     *                  |---- last_value() 返回分组内最后一个值
     *                  |---- nth_value() 返回分组内第n个值
     *                  |---- lag(offset n, default_value) 以当前列为单位，将当前列向下移动n行，出现空行的地方用default_value填充，否则为null
     *                  |---- lead(offset n, default_value) 以当前列为单位，将当前列向上移动n行，出现空行的地方用default_value填充，否则为null
     *                  |---- ntile(int n) 返回当前行在分组内的分桶号。计算时，先将分组内的所有数据划分为n个桶，之后返回每个记录的分桶号。范围为1~n
     *                                      当分桶不均匀时，最后桶中的数据会少一些
     *
     *      partition_definition：窗口分区，分区语句为：partition by fields，窗口函数在执行时会以分区为单位进行。
     *                          如果没有指定分区，也没有指定行数据范围，那么只有一个分区，整张表
     *
     *      order_definition：窗口排序，排序语句为：order by fields，窗口函数执行前会对分区数据进行排序
     *
     *      frame_definition：行控制，以当前数据行为准，在当前分区内进一步细分窗口。指定行控制语句，通常是为了当作滑动窗口使用。
     *                      一些窗口函数是静态窗口，其指定的行控制语句不会生效。行控制分为基于行号的ROWS，和基于值范围的RANGE。
     *
     *
     *      常用的行控制语句：
     *          基于行号的行控制，使用rows between frame_start and frame_end语句来表示范围，frame_start和frame_end支持以下关键字
     *          说明：between and 表示的范围左闭右闭，[frame_start, frame_end]
     *          > CURRENT ROW：表示当前行
     *          > unbounded preceding：相当于-∞，表示当前分区中的第一行
     *          > unbounded following：相当于+∞，表示当前分区中的最后一行
     *          > expr preceding：以当前行为基准，当前行前面的第expr(数值或表达式)行为边界
     *          > expr following：以当前行为基准，当前行后面的第expr(数值或表达式)行为边界
     *
     */

    /**
     *  7、文件压缩和文件存储
     *  7.1、文件压缩
     *      Hive是基于Hadoop开发的数据仓库工具，因此Hive支持的压缩方式与Hadoop支持的压缩方式相同，并且.xml配置文件的配置项都是一样的
     *
     *  7.2、文件存储
     *      文件存储的四种常见格式：TextFile、SequenceText、orc、parquet
     *      存储效率：orc > parquet > SequenceText
     *      计算效率：orc > parquet > sequenceText
     *
     *      Hive数据存储方式：行存储和列存储
     *      行存储特点：查询快，计算慢
     *      列存储特点：查询慢，计算快
     *
     *      TextFile、SequenceText存储格式采用行存储
     *      orc、parquet存储格式采用列存储
     *
     *      说明：
     *          Hive中默认的存储格式是TextFile，最常用的存储是orc
     *          orc存储格式自带压缩方式，默认压缩方式为：'ZLIB'
     *          创建表时可以利用stored by语句指定表的存储格式，对于orc存储格式，还需要使用tbproperties("orc.compress" = "ZLIB")指定orc的压缩方式
     *
     *      结论：实际开发中在考虑存储空间的使用率的同时也会考虑数据计算的效率，所以综合一下 更推荐的存储格式+压缩方式的组合
     *          orc + snappy
     *
     */
}
