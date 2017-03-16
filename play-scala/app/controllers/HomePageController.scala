package controllers

import javax.inject._

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import models._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Flash
import services.{UploadJsonFile, UploadService}

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HomePageController @Inject()  extends Controller {

//object HomePageController extends Controller with UploadService {

  /**
    * Create an Action to render an HTML page with a welcome message.
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */

  val userForm = Form(
    mapping(
      "File: " -> nonEmptyText
    )(UserJsonFile.apply)(UserJsonFile.unapply)
  )

  def index = Action { implicit request =>
    Ok(views.html.index("Imaginea"))
  }

//  def submit = Action { implicit request =>
//    userForm.bindFromRequest().fold(
//      formWithErrors => BadRequest(views.html.index(formWithErrors)),
//      fileInfo => Ok(views.html.parsedpdf.render(fileInfo.userFileLocation))
////      fileInfo => Ok(s"${fileInfo.userFileLocation} is under processing...")
//    )
//  }


  val uploadService: UploadService = UploadJsonFile

  def upload = Action(parse.multipartFormData) { implicit request =>
    implicit val myFlash = Flash
    val result = uploadService.uploadFile(request)
    Redirect(routes.HomePageController.index).flashing("message" -> result)
  }

}


/*
index.scala.html
@(userForm:Form[UserFile])(implicit messages: Messages)

<img src="@routes.Assets.at("/public/images", "Imaginea1.png")" />
    @form(action = routes.HomePageController.submit()){
@inputText(userForm("File: "))
        <input type="submit" class = "btn primary" value="Extract Info">
    }
  */