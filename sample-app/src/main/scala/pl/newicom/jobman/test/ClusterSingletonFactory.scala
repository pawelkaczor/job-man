package pl.newicom.jobman.test

import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Props}
import akka.cluster.typed.{ClusterSingleton, ClusterSingletonSettings}
import pl.newicom.jobman.JobMan
import pl.newicom.jobman.shared.command.Stop

object ClusterSingletonFactory {

  def clusterSingleton[C](as: ActorSystem[_], serviceName: String, behavior: Behavior[C]): ActorRef[C] =
    ClusterSingleton(as).spawn(behavior, s"Global${serviceName}service", Props.empty, clusterSingletonSettings(as), Stop.asInstanceOf[C])

  private def clusterSingletonSettings(as: ActorSystem[_]): ClusterSingletonSettings =
    ClusterSingletonSettings.create(as).withRole(JobMan.Role.backend)

}
