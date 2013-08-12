package vCloud

import play.api.libs.ws.WS
import xml.{Node, NodeSeq, XML}
import com.ning.http.client.Realm
import play.api.libs.concurrent.Promise

/**
 * Created with IntelliJ IDEA.
 * User: antoine
 * Date: 27/09/12
 * Time: 13:42
 */

case class VCloudAuthenticated(token:String,orgUrl:String)
case class VCloudOrgDetail(vdc:String,network:String,catalogs:Seq[String])
case class VCloudVMCreation(vmName:String,vmURI:String,taskURI:String)
case class VCloudVMInfo(powerOnURI:String,customizationURI:String,internalIPAddress:String)

class VCloudClient (username:String, password:String, organization:String){


  private val serverUrl:String="https://vdc.ebrc.com"

  // PUBLIC methods.

  def versions=
    WS.url(serverUrl+"/api/versions")
      .get().map{resp=>
        XML.loadString(resp.body)
      }

    def createVM(vmName:String, templateName:String)=
    for(
      token<-createToken;
      orgDetail<-getOrgDetail(token.get);
      template<-findTemplate(token.get,orgDetail.get.catalogs,templateName);
      templateURI<-getTemplateURI(token.get,template.get);
      result<-instantiation(token.get,orgDetail.get,templateURI,vmName)
    ) yield result

  def isTaskSuccess(taskURI:String)=
    createToken.flatMap{
      case Some(auth)=>
        authRequest(taskURI,auth).get()
          .map{resp=>XML.loadString(resp.body)}
          .map{result=>(result \ "@status")(0).text=="success"}
      case _ => Promise.pure(false)
    }

  def customization(vmName:String, vmURI:String, rootPassword:String)=
    for (
      token<-createToken;
      vmInfos<-getVMInfos(token.get,vmURI);
      task<-customizeVM(token.get,vmInfos.customizationURI,vmName,rootPassword)
    ) yield task

  def powerOn(vmURI:String)=
    for (
      token<-createToken;
      vmInfos<-getVMInfos(token.get,vmURI);
      task<-powerOnVM(token.get,vmInfos.powerOnURI)
    ) yield task

  def getVMDetail(vmURI:String)=
    for (
      token<-createToken;
      vmInfos<-getVMInfos(token.get,vmURI)
    ) yield vmInfos

  // PRIVATE methods...

  private def authRequest(url:String,auth:VCloudAuthenticated)=WS.url(url).withHeaders("x-vcloud-authorization"->auth.token)

  private def createToken=
    WS.url(serverUrl+"/api/v1.0/login")
    //WS.url(serverUrl+"/api/sessions")
      .withAuth(username+"@"+organization,password,Realm.AuthScheme.BASIC)
      .post("").map {resp=>resp.header("x-vcloud-authorization") match{
          case Some(token)=>
            Some(VCloudAuthenticated(token,(((XML.loadString(resp.body) \\ "Org")(0)) \ "@href").text))
          case _ => None
        }
      }

  private def getOrgDetail(auth:VCloudAuthenticated)=
    authRequest(auth.orgUrl,auth).get
      .map{resp=>
        XML.loadString(resp.body) \\ "Link"
      }.map{infos=>
        Some(
          VCloudOrgDetail(
            ((findFirstNodeWithAttribute(infos,"type","application/vnd.vmware.vcloud.vdc+xml").get) \ "@href")(0).text,
            ((findFirstNodeWithAttribute(infos,"type","application/vnd.vmware.vcloud.network+xml").get) \ "@href")(0).text,
            infos.filter{value=>(value\ "@type")(0).text=="application/vnd.vmware.vcloud.catalog+xml"}.map{value=>(value \ "@href")(0).text}
          )
        )
      }

  private def instantiation(auth:VCloudAuthenticated, detail:VCloudOrgDetail, template:String, vmName:String)=
    authRequest(detail.vdc+"/action/instantiateVAppTemplate",auth)
      .withHeaders("Content-Type"->"application/vnd.vmware.vcloud.instantiateVAppTemplateParams+xml")
      .post(xmlInstantiation(vmName,template,detail.network))
      .map{resp=>XML.loadString(resp.body)}
      .map{result=>
        VCloudVMCreation(vmName,(result \ "@href")(0).text,((result \\ "Task")(0) \ "@href")(0).text)
      }


  private def findTemplate(auth:VCloudAuthenticated, catalogs:Seq[String], templateName:String)=
    catalogs.map{catalog=>
      authRequest(catalog,auth).get()
        .map{resp=>XML.loadString(resp.body)}
        .map(_ \\ "CatalogItem")
        .map(findFirstNodeWithAttribute(_,"name",templateName))
    }
    .map(_.await.get)
    .map{
      case Some(template)=>Some((template \ "@href")(0).text)
      case _ => None
    }
    .filter{result=>
      result.isDefined
    }
    .map(Promise.pure(_))
    .head

  private def getTemplateURI(auth:VCloudAuthenticated, templateUrl:String)=
    authRequest(templateUrl,auth).get()
      .map{resp=>XML.loadString(resp.body)}
      .map{template=>((template \\ "Entity")(0) \\ "@href")(0).text}

  private def getVMInfos(auth:VCloudAuthenticated, vmURI:String)={
    authRequest(vmURI,auth).get
      .map{resp=>XML.loadString(resp.body)}
      .map{data=>
        VCloudVMInfo(
          findFirstNodeWithAttribute(data \ "Link","rel","power:powerOn").map{node=>(node \ "@href")(0).text}.getOrElse(""),
          ((data \\ "Vm")(0) \\ "GuestCustomizationSection" \ "@href").text,
          ((data \\ "Vm")(0) \\ "NetworkConnection" \ "IpAddress").text
        )
    }
  }

  private def customizeVM(auth:VCloudAuthenticated, customizationURI:String, vmName:String, password:String)=
    authRequest(customizationURI,auth)
      .withHeaders("Content-Type"->"application/vnd.vmware.vcloud.guestcustomizationsection+xml")
      .put(xmlCustomization(customizationURI,vmName,password))
      .map{resp=>XML.loadString(resp.body)}
      .map{data=>
        ((data \\ "Task")(0) \ "@href")(0).text
      }

  private def powerOnVM(auth:VCloudAuthenticated,powerOnURI:String)=
    authRequest(powerOnURI,auth).post("")
      .map{resp=>XML.loadString(resp.body)}
      .map{data=>(data \ "@href")(0).text}

  // UTILS methods...

  private def findFirstNodeWithAttribute(data:NodeSeq,attributeName:String, attributeValue:String)=data.find{node=>
    (node \ ("@"+attributeName))(0).text match {
      case str:String if str==attributeValue => true
      case _ => false
    }
  }

  // XML templates

  private def xmlInstantiation(vmName:String,template:String,network:String)=
    <InstantiateVAppTemplateParams name={vmName} xmlns="http://www.vmware.com/vcloud/v1" xmlns:ovf="http://schemas.dmtf.org/ovf/envelope/1">
      <Description>Example app instantiated via REST API</Description>
      <InstantiationParams>
        <NetworkConfigSection>
          <ovf:Info>Configuration parameters for vAppNetwork</ovf:Info>
          <NetworkConfig networkName="OrgNet-Intech-Routed-1">
            <Configuration>
              <ParentNetwork href={network}/>
              <FenceMode>bridged</FenceMode>
            </Configuration>
          </NetworkConfig>
        </NetworkConfigSection>
      </InstantiationParams>
      <Source href={template}/>
    </InstantiateVAppTemplateParams>


  private def xmlCustomization(customizationURI:String, vmName:String, vmPassword:String)=
    <GuestCustomizationSection type="application/vnd.vmware.vcloud.guestCustomizationSection+xml" href={customizationURI} ovf:required="false" xmlns="http://www.vmware.com/vcloud/v1" xmlns:ovf="http://schemas.dmtf.org/ovf/envelope/1">
      <ovf:Info>Specifies Guest OS Customization Settings</ovf:Info>
      <Enabled>true</Enabled>
      <ChangeSid>false</ChangeSid>
      <JoinDomainEnabled>false</JoinDomainEnabled>
      <UseOrgSettings>false</UseOrgSettings>
      <AdminPasswordEnabled>true</AdminPasswordEnabled>
      <AdminPasswordAuto>false</AdminPasswordAuto>
      <AdminPassword>{vmPassword}</AdminPassword>
      <ResetPasswordRequired>false</ResetPasswordRequired>
      <ComputerName>{vmName}</ComputerName>
    </GuestCustomizationSection>

}
