package services

import java.io.File
import java.util.Timer

import play.api.Logger
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData
import play.api.mvc.Request
import org.apache.commons.io.FilenameUtils
import org.mleap.demo.LeapFrameSample

object UploadJsonFile extends UploadService {

  /**
    * Get file from the request and move it in your location
    *
    * @param request
    * @return
    */
  def uploadFile(request: Request[MultipartFormData[TemporaryFile]]): String = {
    log.debug("Called uploadFile function" + request)
    request.body.file("file").map { document =>
      import java.io.File
      val fileNameWithExt = document.filename
      val fileNameWithOutExt = FilenameUtils.removeExtension(fileNameWithExt);
      val uploadedFile = new File(s"/tmp/$fileNameWithExt")
      filePath = uploadedFile.getAbsolutePath
      val contentType = document.contentType
      log.debug(s"File name : $fileNameWithExt, content type : $contentType")
      document.ref.moveTo(uploadedFile)
      if(new File(UploadModelZipFile.getUploadedPath).isFile) {
        if (uploadedFile.isFile)
          "\nModel: " + UploadModelZipFile.getUploadedPath +
            "\t" + LeapFrameSample.getTopicDistribution(UploadModelZipFile.getUploadedPath, uploadedFile.toPath.toString).mkString(",")
        else
          "Please select a json file!"
      }
      else
        "Please select a model file!"

    }.getOrElse("Missing File!")

  }
}

object UploadModelZipFile extends UploadService {

  override def uploadFile(request: Request[MultipartFormData[TemporaryFile]]): String = {
    log.debug("Called uploadFile function" + request)
    request.body.file("file").map { document =>
      import java.io.File
      val fileNameWithExt = document.filename
      val fileNameWithOutExt = FilenameUtils.removeExtension(fileNameWithExt);
      val uploadedFile = new File(s"/tmp/$fileNameWithExt")
      filePath = uploadedFile.getAbsolutePath
      val contentType = document.contentType
      log.debug(s"File name : $fileNameWithExt, content type : $contentType")
      document.ref.moveTo(uploadedFile)
      "Model File uploaded @ " + filePath
    }.getOrElse("Missing File!")

  }
}

/**
  * Created by mageswarand on 20/1/17.
  */

trait UploadService {

  protected val log: Logger = Logger(this.getClass)
  protected var filePath: String = "Update me in child class!!!"

  def uploadFile(request: Request[MultipartFormData[TemporaryFile]]): String

  def getUploadedPath: String = filePath
}