package pl.newicom.jobman.test
import pl.newicom.jobman.notification.{NotificationMsg, ScalaJobNotificationHandler}

import scala.concurrent.{ExecutionContext, Future}

class TestJobNotificationHandler(implicit ec: ExecutionContext) extends ScalaJobNotificationHandler {

  def apply(msgFuture: Future[NotificationMsg]): Future[Unit] = {
    msgFuture.map(msg => println(s"Notification received: $msg"))
  }

}
