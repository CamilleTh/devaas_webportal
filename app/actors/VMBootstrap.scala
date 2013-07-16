package actors

import akka.actor.Actor
import play.api.Logger
import vCloud.VCloudClient
import akka.util.duration._
import play.api.Play.current
import play.api.Play._
import actors.VMBootstrap._
import chef.{SSHBootstrap, InTechCloudClient, BootstrapResult}

/**
 * Created with IntelliJ IDEA.
 * User: antoine
 * Date: 27/09/12
 * Time: 15:42
 */

object VMBootstrap{
  case class CreateNewVM(vmName:String,appStack:String)
  case class BootstrapVM(vmName:String,ipAddress:String,rootPassword:String, appStack:String, bootstrapParams:Map[String,String])
}

class VMBootstrap extends Actor{

  import actors.VMBootstrap.CreateNewVM

  private val logger=Logger("actors.VMBoostrap")

  // TODO parameters
  private val vCloudClient=new VCloudClient("api","apiapi","Org_temp_Intech")

  val cloudClient=new InTechCloudClient(
    configuration.getString("chef.serverURL").get,
    configuration.getString("chef.serverBasePath").get,
    configuration.getString("chef.userId").get,
    play.api.Play.current.getFile(configuration.getString("chef.userKey").get).getAbsolutePath)

  val sshClient=new SSHBootstrap(
    configuration.getString("chef.bootstrap.server").get,
    configuration.getInt("chef.bootstrap.port").get,
    configuration.getString("chef.bootstrap.user").get,
    play.api.Play.current.getFile(configuration.getString("chef.bootstrap.userKey").get).getAbsolutePath
  )

  private val templateName="template_PAAS"

  private val vCloudPollingDelay=15 seconds

  private case class CheckState(state:String,taskURI:String, vmName:String, vmURI:String, appStack:String)

  protected def receive = {

    // Request for creating a new VM
    case CreateNewVM(vmName,appStack)=>
      logger.debug("Create new VM with name "+vmName)
      vCloudClient.createVM(vmName,templateName).onRedeem{vm=>
        logger.info("VM "+vmName+" creation triggerd, in progress")
        context.self ! CheckState("created",vm.taskURI,vm.vmName,vm.vmURI,appStack)
      }

    // Request for checking if VM has been created
    case CheckState("created",taskURI,vmName,vmURI,appStack)=>
      logger.debug("Check if VM "+vmName+" has been created")
      vCloudClient.isTaskSuccess(taskURI).onRedeem{
        case true=>
          // Created, do customization
          logger.info("VM "+vmName+" created!")
          // TODO : root password generation
          vCloudClient.customization(vmName,vmURI,"root").onRedeem{task=>
            logger.info("VM "+vmName+" customization triggered, in progress")
            context.self ! CheckState("customized",task,vmName,vmURI,appStack)
          }
        case _ =>
          // Not yet created, schedule next check
          logger.debug("Not yet created, check later...")
          context.system.scheduler.scheduleOnce(vCloudPollingDelay,self,CheckState("created",taskURI,vmName,vmURI,appStack))
      }

    // Request for checking if VM has been customized
    case CheckState("customized",taskURI,vmName,vmURI,appStack)=>
      logger.debug("Check if VM "+vmName+" has been customized")
      vCloudClient.isTaskSuccess(taskURI).onRedeem{
        case true=>
          logger.info("VM "+vmName+" customized!")
          vCloudClient.powerOn(vmURI).onRedeem{task=>
            logger.info("VM "+vmName+" power-on triggered, in progress")
            context.self ! CheckState("poweron",task,vmName,vmURI,appStack)
          }
        case _=>
          // Not yet customized, schedule next check
          logger.debug("Not yet customized, check later...")
          context.system.scheduler.scheduleOnce(vCloudPollingDelay,self,CheckState("customized",taskURI,vmName,vmURI,appStack))
      }

    // Request for checking if VM has been started
    case CheckState("poweron",taskURI,vmName,vmURI,appStack)=>
      logger.debug("Check if VM "+vmName+" has been started")
      vCloudClient.isTaskSuccess(taskURI).onRedeem{
        case true =>
          logger.info("VM "+vmName+" is started!")
          // Retrieve IP address
          vCloudClient.getVMDetail(vmURI).onRedeem{detail=>
            logger.info("VM IP Address is "+detail.internalIPAddress)
            // TODO : password generation ?
            context.self ! BootstrapVM(vmName,detail.internalIPAddress,"root",appStack,Map())
          }
        case _ =>
          // Not yet started, schedule next check
          logger.debug("Not yet started, check later...")
          context.system.scheduler.scheduleOnce(vCloudPollingDelay,self,CheckState("poweron",taskURI,vmName,vmURI,appStack))
      }

    case BootstrapVM(vmName,ipAddress,rootPassword,appStack,bootstrapParams)=>
      logger.debug("Bootstrap VM "+vmName+" for application stack "+appStack)
      appStack match {
        case "typesafe"=>
          // We need the build URL
          bootstrapParams.get("buildUrl") match {
            case Some(buildURL) =>
              logger.debug("Build URL is "+buildURL+", bootstrap VM")
              sshClient.bootstrapHost(ipAddress,rootPassword,vmName,appStack,Map(
                "buildUrl" -> buildURL
              )).onRedeem{
                case BootstrapResult(true,_) =>
                  logger.info("VM "+vmName+" boostraped !")
                case BootstrapResult(false,100) =>
                  logger.error("Bootstrap of VM "+vmName+" failed, because VM is not yet reachable. Will retry later...")
                  context.system.scheduler.scheduleOnce(vCloudPollingDelay,self,BootstrapVM(vmName,ipAddress,rootPassword,appStack,bootstrapParams))
                case BootstrapResult(false,status) =>
                  logger.error("Bootstrap of VM "+vmName+" failed, with status "+status)
              }
            case _ =>
              logger.debug("Retrieve application build URL")
              cloudClient.getJob(vmName).onRedeem{
                case Some(jobDetail)=>
                  logger.debug("Jenkins job detail retrieved")
                  // TODO store buildUrl in jenkins job detail
                  context.self ! BootstrapVM(vmName,ipAddress,rootPassword,appStack,
                    bootstrapParams ++ Map(("buildUrl"->("http://192.168.0.18:8080/job/"+vmName+"/ws/dist/"+vmName+"-1.0.zip")))
                  )
                case _ =>
                  logger.debug("Jenkins job not yet created, check later")
                  context.system.scheduler.scheduleOnce(vCloudPollingDelay,self,BootstrapVM(vmName,ipAddress,rootPassword,appStack,bootstrapParams))
              }
          }
        case _ => logger.error("Stack "+appStack+" not supported")
      }



  }

}
