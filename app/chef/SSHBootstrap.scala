package chef

import com.jcraft.jsch.{Logger, ChannelExec, JSch}
import io.Source
import play.api
import play.api.Play.current
import play.api.libs.concurrent.{Akka}

/**
 * Created with IntelliJ IDEA.
 * User: antoine
 * Date: 28/09/12
 * Time: 07:29
 */
case class BootstrapResult(success:Boolean,statusCode:Int)

class SSHBootstrap(sshServer:String, sshPort:Int, sshUser:String, sshKey:String) {

  private val loggerSSH=api.Logger("ssh")
  private val loggerBootstrap=api.Logger("bootstrap")

  private val jsch=new JSch()
  jsch.addIdentity(sshKey)
  JSch.setConfig("StrictHostKeyChecking", "no")

  // Bind JSCH logger to Play logger
  JSch.setLogger(new Logger {
    def log(p1: Int, p2: String) {
      p1 match{
        case Logger.DEBUG => loggerSSH.debug(p2)
        case Logger.INFO => loggerSSH.info(p2)
        case Logger.WARN => loggerSSH.warn(p2)
        case _ => loggerSSH.error(p2)
      }
    }
    def isEnabled(p1: Int) = p1 match{
      case Logger.DEBUG => loggerSSH.isDebugEnabled
      case Logger.INFO => loggerSSH.isInfoEnabled
      case Logger.WARN => loggerSSH.isWarnEnabled
      case _ => loggerSSH.isErrorEnabled
    }
  })

  def bootstrapHost(destHost:String, rootPassword:String, appId:String, appStack:String, appParams:Map[String,String]=Map())={
    Akka.future{
      loggerBootstrap.info("Bootstrap host "+destHost+" with application "+appId+" of type "+appStack)
      val chefInfos=
        appStack match {
          case "typesafe"=>
            Some(getTypesafeChefRoles(appId,appParams),getTypesafeChefAttributes(appId,appParams))
          case _ =>
            loggerBootstrap.error("Application stack "+appStack+" is not supported")
            None
        }
      chefInfos match {
        case Some((chefRoles,chefAttributes))=>
          val knifeCommand="knife bootstrap %s -r '%s' -j '%s' -x root -P %s".format(destHost,chefRoles,chefAttributes,rootPassword)
          loggerBootstrap.debug("Bootstrap host with command "+knifeCommand)
          val session=jsch.getSession(sshUser,sshServer,sshPort)
          session.connect()
          val channel=session.openChannel("exec")
          channel.asInstanceOf[ChannelExec].setCommand("cd /opt/bootstrap;"+knifeCommand)
          channel.setInputStream(null)
          channel.asInstanceOf[ChannelExec].setErrStream(System.err)
          val outSource=Source.fromInputStream(channel.getInputStream)
          channel.connect()
          while(!channel.isClosed){
            outSource.getLines().foreach(loggerBootstrap.debug(_))
          }
          loggerBootstrap.debug("Exit status : "+channel.getExitStatus)
          channel.disconnect()
          session.disconnect()
          BootstrapResult(channel.getExitStatus==0,channel.getExitStatus)

      }
    }
  }

  def bootstrapHost2(destHost:String, rootPassword:String, appStack:String)={
    Akka.future{
      loggerBootstrap.info("Bootstrap host "+destHost+" of type "+appStack)
      val chefInfos=
        appStack match {
          case "J2EE"=>
            Some("role[runtime]")
          case _ =>
            loggerBootstrap.error("Application stack "+appStack+" is not supported")
            None
        }
      chefInfos match {
        case Some(chefRoles)=>
          loggerBootstrap.info("sshServer:"+sshServer+" sshPort:"+sshPort+ " sshUser:"+sshUser+ " sshKey:"+sshKey)
          loggerBootstrap.info("chef role : "+chefRoles)
          val knifeCommand="knife bootstrap %s -r '%s' -x root -P %s".format(destHost,chefRoles,rootPassword)
          loggerBootstrap.info("Bootstrap host with command ("+knifeCommand+")")
          val session=jsch.getSession(sshUser,sshServer,sshPort)
          session.connect()
          val channel=session.openChannel("exec")
          //channel.asInstanceOf[ChannelExec].setCommand("cd /usr/share/bootstrap;"+knifeCommand)
          channel.asInstanceOf[ChannelExec].setCommand("cd /usr/share/bootstrap;touch test")
          channel.setInputStream(null)
          channel.asInstanceOf[ChannelExec].setErrStream(System.err)
          val outSource=Source.fromInputStream(channel.getInputStream)
          channel.connect()
          while(!channel.isClosed){
            outSource.getLines().foreach(loggerBootstrap.debug(_))
          }
          loggerBootstrap.info("Exit status : "+channel.getExitStatus)
          channel.disconnect()
          session.disconnect()
          BootstrapResult(channel.getExitStatus==0,channel.getExitStatus)
        case _ =>
          loggerBootstrap.info("Default bug")
          BootstrapResult(false,1)
      }
    }
  }

  private def getTypesafeChefAttributes(appId:String, params:Map[String,String])=
    "{\"typesafe\":{\"playserver\":{\"appname\":\"%s\",\"buildUrl\":\"%s\"}}}".format(appId,params("buildUrl"))

  private def getTypesafeChefRoles(appId:String, params:Map[String,String])="role[play-server]"

}
