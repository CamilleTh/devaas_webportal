package chef

import play.api.libs.ws.{Response, SignatureCalculator, WS}
import play.api.libs.ws.WS.WSRequest
import org.joda.time.{DateTimeZone, DateTime}
import org.joda.time.format.{DateTimeFormatter, DateTimeFormat, ISODateTimeFormat}
import java.security.{PrivateKey, KeyFactory, MessageDigest}
import java.security.spec.PKCS8EncodedKeySpec
import java.io.{FileInputStream, File}
import javax.crypto.Cipher
import org.apache.commons.codec.binary.Base64
import play.api.libs.json.{JsNull, JsValue, Json}
import play.api.Logger

/**
 * Created with IntelliJ IDEA.
 * User: antoine
 * Date: 18/09/12
 * Time: 16:42
 */
class ChefClient(val chefServerURL:String,val userId:String, val userKey: String) {

  private val logger=Logger("chef-client")


  private val CHEF_VERSION = "10.14.2"
  private val SIGN_VERSION = "algorithm=sha1;version=1.0;"

  private val digester=MessageDigest.getInstance("SHA-1")
  private val encoder=new Base64(0)

  private def encode(data:Array[Byte])=new String(encoder.encode(data))
  private def hashAndEncode(data:Array[Byte])=encode(digester.digest(data))

  private val emptyHash=hashAndEncode("".getBytes())

  private def signRequest(path:String)=new SignatureCalculator {
    def sign(request: WSRequest) {
      val timestamp=new DateTime().withZone(DateTimeZone.forID("UTC")).toString(DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"))
      val hashBody=request.build().getByteData match{
        case data:Array[Byte]=>hashAndEncode(data)
        case _=>emptyHash
      }
      val dataToSign=createStringToSign(request.method.toUpperCase(),hashAndEncode(path.getBytes),hashBody,timestamp,userId)
      val signature=encode(encrypt(dataToSign.getBytes(),getPrivateKey(userKey)))
      request.addHeader("X-Ops-Timestamp",timestamp)
      request.addHeader("X-Ops-UserId",userId)
      request.addHeader("X-Ops-Content-Hash",hashBody)
      request.addHeader("X-Chef-Version",CHEF_VERSION)
      request.addHeader("X-Ops-Sign",SIGN_VERSION)
      addAuthorizationHeaders(request,signature)
    }
  }

  private def addAuthorizationHeaders(request:WSRequest,signature:String)=
    signature.grouped(60).zipWithIndex.foreach{case (s,i)=>
      request.addHeader("X-Ops-Authorization-"+(i+1),s)
    }

  private def getPrivateKey(filename:String)={
    val file=new File(filename)
    val pKey=new Array[Byte](file.length().toInt)
    new FileInputStream(file).read(pKey)
    KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(pKey))
  }

  private def encrypt(data:Array[Byte],key:PrivateKey)={
    val cipher=Cipher.getInstance("RSA");
    cipher.init(Cipher.ENCRYPT_MODE, key);
    cipher.doFinal(data)
  }

  private def createStringToSign(method:String,hashedPath:String,hashedBody:String,timestamp:String, userId:String)=
    "Method:"+method.toUpperCase+"\nHashed Path:"+hashedPath+"\nX-Ops-Content-Hash:"+hashedBody+"\nX-Ops-Timestamp:"+timestamp+"\nX-Ops-UserId:"+userId

  private def createSignableRequest(path:String,parameters:Option[Map[String,String]])=
    WS.url(
      chefServerURL+path+(parameters match{
        case Some(params)=>params.foldLeft("?"){(query,pair)=>query+"&"+pair._1+"="+pair._2}
        case _=>""
      })
    )
    .withHeaders(("Accept","application/json"))
    .sign(signRequest(path))

  private def parseChefResponse(response:Response)=response match{
    case resp:Response if resp.status==200 => Some(Json.parse(resp.body))
    case resp =>
      logger.error("Chef API request failed : status %s, response content %s".format(resp.status,resp.body))
      None
  }

  def doGet(path:String, parameters:Option[Map[String,String]]=None)=
    createSignableRequest(path,parameters)
      .get()
      .map(parseChefResponse)

  def doPost(path:String, content:Option[JsValue]=None)=
    createSignableRequest(path,None)
      .post(content.getOrElse(JsNull))
      .map(parseChefResponse)

  def doPut(path:String, content:Option[JsValue]=None)=
    createSignableRequest(path,None)
      .put(content.getOrElse(JsNull))
      .map(parseChefResponse)

  def searchUniqueResult(searchPath:String,query:String)=
    createSignableRequest(searchPath,Some(Map("q"->query)))
      .get()
      .map(parseChefResponse)
      .map{
        // If at least one result, return the first one
        case Some(searchResult) if (searchResult \ "rows").as[List[JsValue]].size>0 => Some(((searchResult \ "rows")(0)))
        case _=> None
      }

}
