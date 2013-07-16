package chef

import play.api.libs.json.Json._
import play.api.Logger
import java.security.MessageDigest
import org.apache.commons.codec.binary.{Hex}
import play.api.libs.concurrent.Promise
import play.api.libs.json.{Json, JsArray, JsObject}
import scala.Some
import play.api.libs.ws.WS

/**
 * Created with IntelliJ IDEA.
 * User: antoine
 * Date: 23/09/12
 * Time: 11:32
 */

case class App(appName:String,appType:String,storageType:String)
case class User(userName:String, sshPubKey:String)

class InTechCloudClient(val chefServerURL:String, val chefServerBasePath:String, val chefUserId:String, val chefUserKey:String) {

  private val digester=MessageDigest.getInstance("MD5")
  private val encoder=new Hex()

  private def encode(data:Array[Byte])=new String(encoder.encode(data))
  private def hashAndEncode(data:Array[Byte])=encode(digester.digest(data))


  private val logger=Logger("cloud-client")

  private val chefClient=new ChefClient(chefServerURL,chefUserId,chefUserKey)

  // TODO refactor ?
  private def searchNode(query:String)=
    chefClient.doGet(chefServerBasePath+"/search/node",Some(Map("q"->query)))

  def isRepositoryCreated(appName:String)=
    searchNode("ci-server_repositories:"+appName).map(_.isDefined)

  def findAllApplications()=
    chefClient.doGet(chefServerBasePath+"/data/apps").map{resp=>
      resp match {
        case Some(data)=>
          Some(JsArray(data.asInstanceOf[JsObject].keys.map{app=>
            toJson(Map(
              "id"->toJson(app)
            ))
          }.toList))
        case _ => None
      }
    }

  def getApplication(appId:String)=
    chefClient.doGet(chefServerBasePath+"/data/apps/"+appId)

  def getStorageMySQL(appId:String)=
    chefClient.searchUniqueResult(chefServerBasePath+"/search/node","storage_mysql:%s".format(appId))

  def getStorageMongo(appId:String)=
    chefClient.searchUniqueResult(chefServerBasePath+"/search/node","storage_mongodb:%s".format(appId))

  def getRepository(appId:String)=
    chefClient.searchUniqueResult(chefServerBasePath+"/search/node","ci-server_repositories:%s".format(appId))

  def getJob(appId:String)=
    chefClient.searchUniqueResult(chefServerBasePath+"/search/node","ci-server_jobs:%s".format(appId))

  def existsAppId(appId:String)=
    chefClient.doGet(chefServerBasePath+"/search/apps",Some(Map("q"->"id:%s".format(appId)))).map{
      case Some(result) if (result \ "total").as[Int]>=1 => true
      case _ => false
    }

  def existsUsername(username:String)=
    chefClient.doGet(chefServerBasePath+"/search/users",Some(Map("q"->"id:%s".format(username)))).map{
      case Some(result) if (result \ "total").as[Int]>=1 => true
      case _ => false
    }

  def existsUserKey(userKey:String)=
    chefClient.doGet(chefServerBasePath+"/search/users",Some(Map("q"->"keyhash:%s".format(hashAndEncode(userKey.getBytes()))))).map{
      case Some(result) if (result \ "total").as[Int]>=1 => true
      case _ => false
    }

  def createUser(username:String,userKey:String)=
    existsUsername(username).flatMap{result=> result match {
      case false =>
        chefClient.doPost(chefServerBasePath+"/data/users", Some(
          toJson(Map(
            "id"->toJson(username),
            "ssh-pub-key"->toJson(userKey),
            "keyhash"->toJson(hashAndEncode(userKey.getBytes()))
          ))
        )).map{
          case Some(x)=>true
          case _ =>false
        }
      case true => Promise.pure(true)
    }
   }

  def createApp(appId:String, appType:String, storageType: String, users:List[(String,String)])=
    chefClient.doPost(chefServerBasePath+"/data/apps",Some(
      toJson(Map(
        "id"->toJson(appId),
        "type"->toJson(appType),
        "storageType"->toJson(storageType),
        "users"->toJson(users.map{user=>
          toJson(Map(
            "username"->toJson(user._1),
            "ssh-pub-key"->toJson(user._2),
            "keyhash"->toJson(hashAndEncode(user._2.getBytes()))
          ))
        })
      ))
    ))


  def findAllUsers()=
    chefClient.doGet(chefServerBasePath+"/data/users").map{
      case Some(user)=>
          Some(JsArray(user.asInstanceOf[JsObject].keys.map{app=>
            toJson(app)
          }.toList))
       case _ => None
    }

  def getUser(username:String)=
    chefClient.doGet(chefServerBasePath+"/data/users/"+username)

  def getRuntimeServer(appId:String)=
    chefClient.doGet(chefServerBasePath+"/data/apps/"+appId).flatMap{
      case Some(appDetail)=>
        (appDetail \ "type").as[String] match{
          case "typesafe"=>
            chefClient.searchUniqueResult(chefServerBasePath+"/search/node","typesafe_playserver_appname:%s".format(appId)).map{
              case Some(detail)=>
                // Add specific attributes for typesafe stack
                Some(detail.as[JsObject]
                  ++
                  toJson(
                    Map(
                      "type"->(appDetail \ "type"),
                      "lastDeploy"->(detail \ "normal" \ "typesafe" \ "playserver" \ "lastDeploy")
                    )
                  ).as[JsObject])
              case _ => None
            }
          case _ => Promise.pure(None)
        }
      case _ => Promise.pure(None)
    }

  def getHostingProxy(appId:String)=
    chefClient.searchUniqueResult(chefServerBasePath+"/search/node","proxy_hosts:%s".format(appId))

  def getLogviewerURL()=
    chefClient.searchUniqueResult(chefServerBasePath+"/search/node","logspace:logviewerURL").map{
      case Some(data)=>Some((data \ "normal" \ "logspace" \ "logviewerURL").as[String])
      case _ => None
    }

  def getLastBuildStatus(jobURL:String)=
    WS.url(jobURL+"/lastBuild/api/json").get.map{resp=>resp.status match{
        case 200 => Some(Json.parse(resp.body))
        case _ => None
      }
    }
}
