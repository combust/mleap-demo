/**
  *
  */

import java.io.File

import org.apache.commons.io.FileUtils
import org.apache.log4j.{Level, LogManager}
import org.apache.spark.ml.feature.Tokenizer
import org.apache.spark.ml.feature.StopWordsRemover
import org.apache.spark.ml.feature.CountVectorizer
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.ml.feature.CountVectorizerModel
import org.apache.spark.ml.clustering.LDA
import org.apache.spark.ml.mleap.feature.WordFilter
import org.apache.spark.sql.Row
import org.apache.spark.sql.SparkSession
import scala.collection.mutable

object LDAJob {

  //Global Variables
  val spark = SparkSession
    .builder()
    .master("local[4]")
    .appName("MLeap Spark")
    .config("spark.driver.memory","4g")
    .getOrCreate()


  val sc = spark.sparkContext
  sc.setLogLevel("ERROR")

  // For implicit conversions like converting RDDs to DataFrames
  import spark.implicits._

  //Relative path for simplicity
  val sparkModelPath = "/tmp/model/spark/"
  val mleapModelFilePath = "/tmp/mleap_model.zip"

  var vocabulary : scala.Array[scala.Predef.String] = Array()

  def train(save: Boolean) = {

    val df = spark.sqlContext.read.text(getClass.getClassLoader.getResource("carroll-alice.txt").toString).
      withColumnRenamed("value", "text") //Each line is a document for us ;)

    val tokenizer = new Tokenizer().setInputCol("text").setOutputCol("words")

    val remover = new StopWordsRemover()
      .setInputCol(tokenizer.getOutputCol)
      .setOutputCol("words_filtered")

    val filterWords = new WordFilter().setInputCol("words_filtered").setOutputCol("filteredWords").setWordLength(3)

    val cv = new CountVectorizer().setInputCol("filteredWords").setOutputCol("features").setVocabSize(50000)

    val lda = new LDA().setK(3).setMaxIter(2)

    val ldaModel = new Pipeline().setStages(Array(tokenizer, remover, filterWords, cv, lda)).fit(df)

    val transformedDF = ldaModel.transform(df)
    println(transformedDF.show())
    println(transformedDF.printSchema())

    if(save) ldaModel.write.overwrite().save(sparkModelPath)

    //MLeap way of saving the model
    {
      FileUtils.deleteQuietly(  new File(mleapModelFilePath))
      import org.apache.spark.ml.mleap.SparkUtil

      // MLeap/Bundle.ML Serialization Libraries
      import ml.combust.mleap.spark.SparkSupport._
      import resource._
      import ml.combust.bundle.BundleFile
      import org.apache.spark.ml.bundle.SparkBundleContext

      val mleapPipeline = SparkUtil.createPipelineModel(uid = "pipeline", Array(ldaModel))

      //      val sbc = SparkBundleContext()
      for (modelFile <- managed(BundleFile("jar:file:" + mleapModelFilePath))) {
        println(modelFile)
        mleapPipeline.writeBundle.save(modelFile)//(sbc)
      }
    }
    ldaModel
  }

  def test(ldaModel: PipelineModel) = {
    //height of laziness...lets use the same file for testing!
    val test_df = spark.sqlContext.read.text(getClass.getClassLoader.getResource("carroll-alice.txt").toString).
      withColumnRenamed("value", "text")

    val topicDistribution = ldaModel.transform(test_df)

    vocabulary = ldaModel.stages(3).asInstanceOf[CountVectorizerModel].vocabulary

    topicDistribution.printSchema()

    topicDistribution.select("filteredWords", "features", "topicDistribution").collect().foreach {
      case Row(filteredWords: scala.collection.mutable.WrappedArray[String],
                features: org.apache.spark.ml.linalg.SparseVector,
                topicDistribution: org.apache.spark.ml.linalg.DenseVector) => {
        println("filteredWords.toArray : " + filteredWords.toArray.size)
        println("Features size: " + features.toDense.size)
        println("TopicDistribution Vector: \n====================\n" + topicDistribution)
        println("TopicDistribution Sum: " + topicDistribution.values.sum)
      }
      case r => r.toSeq.foreach(i => println(i.getClass))
    }

    getTopics(ldaModel.stages(4).asInstanceOf[org.apache.spark.ml.clustering.LocalLDAModel])
  }


  def getTopics(ldaModel: org.apache.spark.ml.clustering.LocalLDAModel) = {

    // Describe topics.
    val describeTopics = ldaModel.describeTopics(5)
    val rowSums = ldaModel.topicsMatrix.rowIter.map(_.toDense.toArray.sum)
    val colSums = ldaModel.topicsMatrix.colIter.map(_.toDense.toArray.sum)
    println("topicsMatrix rows: " + ldaModel.topicsMatrix.numRows)
    println("topicsMatrix cols: " + ldaModel.topicsMatrix.numCols)
    println("vocabulary size: " + vocabulary.size)
    println("The topics described by their top-weighted terms:")
    describeTopics.show(false)

    val topicIndices = ldaModel.describeTopics(maxTermsPerTopic = 5).select("topic", "termIndices", "termWeights").collect()

    val topics = topicIndices.map { case Row(topic: Int, terms: mutable.WrappedArray[Int], termWeights: mutable.WrappedArray[Double]) =>
      terms.zip(termWeights).map { case (term, weight) => (vocabulary(term.toInt), weight) }
    }

    def getTopicStrings() = {
      var str = ""
      topics.zipWithIndex.filter(x => x._2 >= 0 && x._2 < 21).foreach { case (topic, i) =>
        str = str + (s"TOPIC $i") + " \n"
        topic.foreach { case (term, weight) =>
          str = str + (s"$term\t$weight") + " \n"
        }
        str = str + " \n"
      }

      println(str)
    }

    getTopicStrings()
  }

  def trainAndTest(): Unit = {
    val model = train(false)
    println("-----------------------------------------------------------------------")
    test(model)
  }

  def main(args: Array[String]): Unit = {
    LogManager.getRootLogger.setLevel(Level.ERROR)
    trainAndTest()
  }
}