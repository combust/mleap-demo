package controllers

import javax.inject.Inject

import play.api.mvc._
import com.google.inject.Singleton
import models.{SparkConfig, UserJsonFile, UserModelZipFile}
import play.api.data.Form
import play.api.data.Forms._
import services.{UploadModelZipFile, UploadService}


/**
  * Created by mageswarand on 25/1/17.
  */

@Singleton
class ModelUploader@Inject()  extends Controller {


  /*@import helper._
  @import play.mvc.Http.Context.Implicit
  @(sparkConfigForm:Form[SparkConfig])(implicit messages: Messages)


  val configForm = Form(
    mapping (
      "Input File" -> nonEmptyText,
      "Output Folder" -> nonEmptyText
    )(SparkConfig.apply)(SparkConfig.unapply)
  )

  def data = Action { implicit request =>
    import play.api.Play.current
    import play.api.i18n.Messages.Implicits._
    Ok(views.html.modelfile(configForm))
  }

    def submit = Action { implicit request =>
      import play.api.Play.current
      import play.api.i18n.Messages.Implicits._

      configForm.bindFromRequest().fold(
        formWithErrors => BadRequest(views.html.modelfile(formWithErrors)),
        fileInfo => Ok("12345"))
  //      fileInfo => Ok(s"${fileInfo.userFileLocation} is under processing...")
    }


        <div>
        @helper.form(action = routes.ModelUploader.submit, 'id -> "myForm") {
            @helper.inputText(sparkConfigForm("Input Folder").copy(value=Some("Test")))

            @helper.inputText(sparkConfigForm("Output Folder"))
        }
    </div>

    */

  ///////

  def index = Action { implicit request =>
    Ok(views.html.modelfile("Imaginea"))
  }

  val userForm = Form(
    mapping(
      "File: " -> nonEmptyText
    )(UserModelZipFile.apply)(UserModelZipFile.unapply)
  )

  val uploadService: UploadService = UploadModelZipFile

  def upload = Action(parse.multipartFormData) { implicit request =>
    implicit val myFlash = Flash
    val result = uploadService.uploadFile(request)
    Redirect(routes.ModelUploader.index).flashing("message" -> result)
  }

}
