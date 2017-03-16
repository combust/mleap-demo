package org.mleap.demo

import java.io.File

import ml.combust.bundle.BundleFile
import ml.combust.mleap.runtime.DefaultLeapFrame
import ml.combust.mleap.runtime.MleapSupport._
import ml.combust.mleap.tensor.DenseTensor
import models.UserModelZipFile
import org.apache.log4j.{Level, LogManager}
import resource._
import services.UploadModelZipFile

// create a simple LeapFrame to transform
import ml.combust.mleap.runtime.transformer.Pipeline
import ml.combust.mleap.runtime.types._
import ml.combust.mleap.runtime.{LeapFrame, LocalDataset}

/**
  * Created by mageswarand on 14/2/17.
  */
object LeapFrameSample {

  def getMLeapPipeline(filePath: String = "/tmp/mleap_model.zip"): Pipeline = {

    println(this.getClass.getClassLoader)

    // load the Spark pipeline we saved in the previous section
    val bundle = (for(bundleFile <- managed(BundleFile("jar:file:"+ filePath))) yield {
      bundleFile.loadMleapBundle().get
    }).opt.get

    bundle.root.asInstanceOf[Pipeline]
  }


  def getLDAModel(mleapPipeline: Pipeline): Option[ml.combust.mleap.core.clustering.LocalLDAModel] = {

    var localLDAModel: Option[ml.combust.mleap.core.clustering.LocalLDAModel] = None

    mleapPipeline.transformers.foreach{ case (pipeline: Pipeline) =>
      pipeline.transformers.foreach{ model => { model match {
        case lda: ml.combust.mleap.runtime.transformer.clustering.LDAModel => {
          localLDAModel = Some(lda.model)
        }
        case other => localLDAModel = None
      }
      }
      }
    }

    localLDAModel
  }

  def getVocabulary(mleapPipeline: Pipeline): Option[Array[String]] = {
    var vocabulary : Option[scala.Array[String]] = None

    mleapPipeline.transformers.foreach{ case (pipeline: Pipeline) =>
      pipeline.transformers.foreach{ model => { model match {
        case cntVec: ml.combust.mleap.runtime.transformer.feature.CountVectorizer => {
          vocabulary = Some(cntVec.model.vocabulary)
        }
        case other => println("Not interested in " + other.getClass)
      }
      }
      }
    }

    vocabulary
  }

  def getTopics(ldaModel: ml.combust.mleap.core.clustering.LocalLDAModel, vocabulary : scala.Array[String]) = {

    val matrix = ldaModel.topicsMatrix.toDenseMatrix
    val topicIndices = ldaModel.describeTopics(maxTermsPerTopic = 5)

    val topics = topicIndices.map { case (terms: Array[Int], termWeights: Array[Double]) =>
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


  def getLeapFrame(filePath: String = "/tmp/some_file.txt"): DefaultLeapFrame = {
    val file = new File(filePath)

    val source = scala.io.Source.fromFile(file).mkString
    val bytes = source.getBytes("UTF-8")

    import spray.json._
    case class SampleData(text: String,  id: Long)

    object MyJsonProtocol extends DefaultJsonProtocol {
      implicit val converter = jsonFormat2(SampleData.apply) //2 -> denotes two constructor parameters
    }

    import MyJsonProtocol._

    val parsedJson = source.parseJson
//        println(parsedJson.convertTo[Patent])
    val pat = parsedJson.convertTo[SampleData]

    val schema = StructType(Array(
      StructField("text",StringType()),
      StructField("id",LongType())))

    val data = LocalDataset(ml.combust.mleap.runtime.Row(pat.text, pat.id))
    LeapFrame(schema.get, data)
  }

  def getTopicDistribution(mleapPipeline: Pipeline, frame: DefaultLeapFrame): Array[Double] = {

    val frame2 = mleapPipeline.transform(frame).get

    val localDataSet = frame2.select("topicDistribution").get.dataset
    val topicDistribution = localDataSet(0)(0).asInstanceOf[DenseTensor[Double]]
    topicDistribution.values
  }

  def getTopicDistribution(modelFilePath:String, jsonFilePath: String): Array[Double] = {
    println("UploadModelZipFile.getUploadedPath: " + UploadModelZipFile.getUploadedPath)
    getTopicDistribution(getMLeapPipeline(modelFilePath), getLeapFrame(jsonFilePath))
  }

  def main(args: Array[String]): Unit = {

    LogManager.getRootLogger.setLevel(Level.ERROR)

    val mleapPipeline = getMLeapPipeline(this.getClass.getClassLoader.getResource("mleap_model.zip").getFile)

    val patentFrame = getLeapFrame(this.getClass.getClassLoader.getResource("test.json").getFile)

    println(getTopicDistribution(mleapPipeline, patentFrame).mkString(","))

    val localLDAModel = getLDAModel(mleapPipeline)
    val vocabulary = getVocabulary(mleapPipeline)

    if(localLDAModel.isDefined && vocabulary.isDefined){
      getTopics(localLDAModel.get, vocabulary.get)
    } else {
      println("Something had gone bad with the model file or a wrong model file!!!")
    }
  }

}