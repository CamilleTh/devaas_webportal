package dimensionData

import play.api.libs.ws.WS
import xml.{Node, NodeSeq, XML}
import com.ning.http.client.Realm
import play.api.libs.concurrent.Promise

/**
 * Created with IntelliJ IDEA.
 * User: baptiste
 * Date: 12/08/13
 * Time:
 */

class DimensionDataClient(username:String, password:String, organization:String) {

  private val serverUrl:String="https://api-eu.dimensiondata.com/oec/0.9"

  private val orgId = "44ca852a-6a63-4b99-b6c3-fee83015b068"
  private val networkId = "2662bb0e-6550-11e2-84e5-180373fb68df"
  private val imageId = "447c141e-dc7d-40d1-96fb-6ae75d4cc6b6"


  // public

  def createServer(name:String):Promise[Boolean] =
    WS.url(serverUrl+"/"+orgId+"/server")
      .withHeaders("Content-Type"->"application/xml")
      .withAuth(username,password,Realm.AuthScheme.BASIC)
      .post(
        getCreateServerXML(name)
      )
      .map(_.status == 200)

  def getVmDetails(name:String):Promise[Option[Node]] =
    WS.url(serverUrl+"/"+orgId+"/serverWithState?name="+name)
      .withAuth(username,password,Realm.AuthScheme.BASIC)
      .get
      .map{resp=>XML.loadString(resp.body)}
      .map{data=>
        (data \\ "serverWithState").headOption
      }

  // private

  //
  //
  //
  //
  // ATTENTION MOT DE PASSE EN DUR
  //
  //
  //
  private def getCreateServerXML(vmName:String) =
    <Server xmlns='http://oec.api.opsource.net/schemas/server'>
      <name>{vmName}</name>
      <description>My Server Description</description>
      <vlanResourcePath>/oec/{orgId}/network/{networkId}</vlanResourcePath>
      <imageResourcePath>/oec/base/image/{imageId}</imageResourcePath>
      <administratorPassword>intechdevaas</administratorPassword>
      <isStarted>true</isStarted>
    </Server>



}
