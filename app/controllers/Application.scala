package controllers

import play.api._
import libs.json.JsObject
import play.api.libs.concurrent.{Akka, Promise}
import play.api.libs.EventSource
import play.api.libs.iteratee.{Enumerator, Enumeratee, Iteratee}
import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.mvc._
import play.api.libs.json.Json._
import play.api.libs.EventSource
import chef.InTechCloudClient
import scala.Some
import play.api.Play.current
import play.api.Play.configuration
import models.Validators
import akka.actor.Props
import actors.VMBootstrap.CreateNewVM
import actors.VMBootstrap
import play.api.mvc.Security.Authenticated
import scala.Predef._
import actors.VMBootstrap.CreateNewVM
import scala.Some
import actors.VMBootstrap.CreateNewVM
import scala.Some
import play.api.libs.json.JsObject
import dimensionData.DimensionDataClient

object Application extends Controller {

  val cloudClient=new InTechCloudClient(
    configuration.getString("chef.serverURL").get,
    configuration.getString("chef.serverBasePath").get,
    configuration.getString("chef.userId").get,
    play.api.Play.current.getFile(configuration.getString("chef.userKey").get).getAbsolutePath)

  val users=configuration.getString("authorizedUsers").map(_.split(",").toList).getOrElse(List())

  val bootstrapActor=Akka.system.actorOf(Props[VMBootstrap], name = "bootstrapActor")

  val dimensionDataClient = new DimensionDataClient("Sebastien_Larose","intechdevaas","notused")

  def index = Action {request=>
    Ok(views.html.home(request.session.get("user").isDefined))
  }

  def myCloud=Secured {Action{
    Ok(views.html.myCloud())
  }}

  def getAllApplication=Secured{Action{
    Async{
      cloudClient.findAllApplications().map{app=>app match{
        case Some(data) => Ok(data)
        case _ => Ok("")
      }}
    }
  }}

  def mytest=Secured{Action{
    Async{
      cloudClient.findAllClient().map{app=>app match{
        case Some(data) => Ok(data)
        case _ => Ok("")
      }
      }
    }
  }}

  def getEnvsByApplication(appId:String)=Secured{Action{
    Async{
      cloudClient.findEnvsByApplication(appId).map{app=>app match{
        case Some(data) => Ok(data)
        case _ => Ok("")
      }}
    }
  }}

  def getApplicationDetail(appId:String)=Secured{Action{
    Async{
      cloudClient.getApplication(appId).flatMap{
          case Some(detail)=>
              cloudClient.getHostingProxy(appId).map{
              case Some(proxy) =>
                Some(
                  detail.as[JsObject]++toJson(Map(
                    "url"->proxy \ "normal" \ "proxy" \ "hosts" \ appId
                  )).as[JsObject]
                )
              case _ =>
                Some(
                  detail.as[JsObject]++toJson(Map(
                    "url"->"none"
                  )).as[JsObject]
                )
            }
          case _ => Promise.pure(None)
      }.map{
        case Some(data) => Ok(data)
        case _ => NotFound
      }
    }
  }}

  def getStorageDetail(appId:String)={
    Secured{Action{
    Async{
      cloudClient.getApplication(appId).flatMap{resp=>resp match{
        case Some(data)=>(data \ "storageType").as[String] match{
            case "mysql" =>
              cloudClient.getStorageMySQL(appId).map{
                case Some(data) =>
                  Some(toJson(Map(
                    "storageType"->toJson("mysql"),
                    "envs" -> data \ "normal" \ "storage" \ "mysql" \ appId
                  )))
                case _ => None
              }
            case "mongo" =>
              cloudClient.getStorageMongo(appId).map{
                case Some(data) =>
                  Some(toJson(Map(
                    "storageType"->toJson("mongo"),
                    "envs" -> data \ "normal" \ "storage" \ "mongodb" \ appId
                  )))
                case _ => None
            }
            case "none" => Promise.pure(Some(toJson(Map("storageType"->toJson("none")))))
            case _ => Promise.pure(None)
          }
          case _ => Promise.pure(None)
        }
      }.map{
          case Some(data)=>Ok(data)
          case _ => NotFound
      }
    }
  }}
  }

  def getRepositoryDetail(appId:String)=Secured{Action{
    Async{
      cloudClient.getApplication(appId).flatMap{resp=>resp match{
          case Some(appData)=>
            cloudClient.getRepository(appId).map{
              case Some(repoData) =>
                Some(toJson(Map(
                  "path"->repoData \ "normal" \ "repositories" \ appId,
                  "users"->appData \ "users"
                )))
              case _ => None
            }
          case _ => Promise.pure(None)
        }
      }.map{
          case Some(data)=>Ok(data)
          case _ => NotFound
      }
    }
  }}

  def getBuildDetail(appId:String,env:String)=Secured{Action{
    Async{
      cloudClient.getJob(appId).map{
        case Some(data)=>
          Some(toJson(Map(
            "jobUrl"->data \ "normal" \ "ci-server" \ "jobs" \ appId \ env,
            "jobInternalUrl"-> toJson("http://"+(data \ "automatic" \ "ipaddress").as[String]+":8080/job/"+appId)
          )))
        case _ => None
      }.flatMap{
        case Some(jobInfo)=>
          cloudClient.getLastBuildStatus((jobInfo \ "jobUrl").as[String]).map{
            case Some(lastBuild)=>
              Some(jobInfo.as[JsObject]++toJson(Map(
                  "lastBuildStatus"->lastBuild \ "result",
                  "lastBuildDate"->lastBuild \ "timestamp",
                  "lastBuildUrl"->lastBuild \ "url"
                )).as[JsObject]
              )
            case _ =>
              Some(jobInfo.as[JsObject]++toJson(Map(
              "lastBuildStatus"->toJson("UNKNOWN")
            )).as[JsObject]
            )
          }
        case _ => Promise.pure(None)
      }.map{
        case Some(data)=>Ok(data)
        case _ => NotFound
      }
    }
  }}


  def validateAppId=Secured{Action{request=>
    Async{
      Validators.validateAppName(request.queryString("appId").head,cloudClient).map{result=>
        Ok(toJson(Map(
          "valid"->toJson(result.valid),
          "error"->result.error.map(toJson(_)).getOrElse(JsNull)
        )))
      }
    }
  }}

  def validateUsername=Secured{Action{request=>
    Async{
      Validators.validateUsername(request.queryString("username").head,cloudClient).map{result=>
        Ok(toJson(Map(
          "valid"->toJson(result.valid),
          "error"->result.error.map(toJson(_)).getOrElse(JsNull)
        )))
      }
    }
  }}

  def validateUserKey=Action(parse.text){request=>
    Async{
      Validators.validateUserKey(request.body.trim,cloudClient).map{result=>
        Ok(toJson(Map(
          "valid"->toJson(result.valid),
          "error"->result.error.map(toJson(_)).getOrElse(JsNull)
        )))
      }
    }
  }

  def createApplication=Secured{Action{request=>
    Async {
      request.body.asJson.map{json=>

        //val (appId,appType,storageType,users)=(
        val (appId,appType,groupid,storageType,users,envs)=(
          (json \ "id").as[String].trim,
          (json \ "type").as[String].trim,
          (json \ "groupid").as[String].trim,
          (json \ "storageType").as[String].trim,
          (json \ "users").as[List[JsValue]].map{user=>
            ((user \ "username").as[String],(user \ "userkey").as[String])
          },
          (json \ "envs").as[List[JsValue]].map{env=>
            ((env \ "name").as[String],(env \ "version").as[String])
          }
         )
        users.foreach{user=>
            cloudClient.createUser(user._1,user._2)
        }
          //cloudClient.createApp(appId,appType,storageType,users).map{
          cloudClient.createApp2(appId, groupid, appType, storageType, users, envs).map{
            case Some(json)=>
            //bootstrapActor ! CreateNewVM(appId,appType)
              envs.foreach{
                case (name,version) =>
                  dimensionDataClient.createServer(appId+"_"+name+"_001")
                  dimensionDataClient.getProgression(appId+"_"+name+"_001")
              }
              Ok(toJson(true))
            case _ =>BadRequest("Invalid data")
        }
      }.getOrElse(Promise.pure(BadRequest("Invalid JSON")))
    }
  }}

  def getAllUsers=Secured{Action{
    Async{
      cloudClient.findAllUsers().flatMap{
        case Some(users)=>
          // TODO : flatten ?
          Promise.pure(Some(
            toJson(
              users.as[List[String]].map{
                cloudClient.getUser(_).await.get
              }.map{
                case Some(userDetail)=>
                  toJson(Map(
                    "username"->userDetail \ "id",
                    "userkey"->userDetail \ "ssh-pub-key"
                  ))
                case _ => JsNull
              }
            )
         ))
        case _ => Promise.pure(None)
      }.map{
        case Some(users)=> Ok(users)
        case _ => NotFound
      }
    }
  }}

  def getKibanaUrl(appId:String,env:String)=Secured{Action{result=>
    Async{
      cloudClient.getKibana().map{
        case Some(data) =>
          Ok(toJson(Map(
            "kibanaUrl"->data \ "normal" \ "log" \ "kibana" \ "url",
            "envName"-> toJson(env),
            "appId"-> toJson(appId)
          )))
        case _ => NotFound
      }
    }
  }}

  def getRuntimeServer(appId:String): Action[(Action[AnyContent], AnyContent)] =Secured{Action{
    Async{
      cloudClient.getRuntimeServer(appId).map{
        case Some(data) =>
          Ok(toJson(Map(
            "server"->data \ "automatic" \ "ipaddress",
            "uptime"->data \ "automatic" \ "uptime",
            "type"->data \ "type",
            "lastDeploy"->data \ "lastDeploy",
            "appId"->toJson(appId)
          )))
        case _ => NotFound
      }
    }
  }}

  def runApplicationBuild(id:String,env:String): Action[(Action[AnyContent], AnyContent)] =Secured{Action{
    Async{
      cloudClient.getJob(id).flatMap{
        case Some(data)=>
          WS.url((data \ "normal" \ "ci-server" \ "jobs" \ id \ env).as[String]+"/build").get().map{resp=>
            resp.status match {
              case 200 => true
              case _ => false
            }
          }
        case _ => Promise.pure(false)
      }.map{success=>
        Ok(toJson(Map(
          "result"->toJson(success)
        )))
      }
    }
  }}

  def login=Action{
    Ok(views.html.login())
  }

  def doLogin=Action(parse.urlFormEncoded){request=>
    (request.body("username").headOption,request.body("password").headOption) match {
      case (Some(username),Some(password))=>
        if(username==password && users.contains(username))
          Redirect("/mycloud").withSession(("user",username))
        else
          Ok(views.html.login(Some("Invalid credentials")))
      case _ => Ok(views.html.login(Some("Invalid credentials")))
    }
  }

  def doLogout=Action{
    Redirect("/").withNewSession
  }

  def Secured(action:Action[AnyContent])=Authenticated(
    req => req.session.get("user"),
    _ => Forbidden("You have to be authenticated to access this service"))(username=> action)

  def streamLog(appId:String)=Secured{Action{Async{
    cloudClient.getLogviewerURL().map{
      case Some(url)=>
        Console.println("OK :" + url)
        Ok.stream{res:Iteratee[Array[Byte], Unit]=>
          WS.url(url+"/"+appId).get(r=>res)
        }.as("text/event-stream")
      case _ =>
        NotFound("Logviewer server not found")
    }
  }}}

  def test(appId:String,appType:String)=Action{
    bootstrapActor ! CreateNewVM(appId,appType)
    Ok("OK")
  }
}