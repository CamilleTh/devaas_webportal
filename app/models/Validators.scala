package models

import play.api.libs.concurrent.Promise
import chef.InTechCloudClient

/**
 * Created with IntelliJ IDEA.
 * User: antoine
 * Date: 25/09/12
 * Time: 22:03
 */

case class ValidationResult(valid:Boolean,error:Option[String])

object Validators {

  def validateAppName(appName:String,client:InTechCloudClient)=appName.trim match{
    case "" =>
      Promise.pure(ValidationResult(false,Some("Name cannot be empty")))
    case str:String if str.length>15 || str.length<5 =>
      Promise.pure(ValidationResult(false,Some("Name length must be between 5 and 15 characters")))
    case str:String if str.matches("[a-zA-Z0-9]*")=>
      client.existsAppId(appName).map{
        case true => ValidationResult(false,Some("Application name already exists"))
        case _ => ValidationResult(true,None)
      }
    case _ =>
      Promise.pure(ValidationResult(false,Some("Valid characters are : [0-9], [a-Z] and [A-Z]")))
  }

  def validateUsername(username:String,client:InTechCloudClient)=username.trim match{
    case ""=>
      Promise.pure(ValidationResult(false,Some("Username cannot be empty")))
    case str:String if str.length>15 || str.length<3 =>
      Promise.pure(ValidationResult(false,Some("Username length must be between 3 and 15 characters")))
    case str:String if str.matches("[a-zA-Z0-9]*")=>
      client.existsUsername(username).map{
        case true => ValidationResult(false,Some("Username already exists"))
        case _ => ValidationResult(true,None)
      }
    case _ =>
      Promise.pure(ValidationResult(false,Some("Valid characters are : [0-9], [a-Z] and [A-Z]")))
  }

  def validateUserKey(userkey:String,client:InTechCloudClient)=userkey.trim match{
    case ""=>
      Promise.pure(ValidationResult(false,Some("User key cannot be empty")))
    case _ =>
      client.existsUserKey(userkey.trim).map{
        case true => ValidationResult(false,Some("User key already exists"))
        case _ => ValidationResult(true,None)
      }
  }

}
