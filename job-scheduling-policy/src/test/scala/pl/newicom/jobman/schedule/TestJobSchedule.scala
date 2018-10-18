package pl.newicom.jobman.schedule
import pl.newicom.jobman.Job
import pl.newicom.jobman.schedule.JobSchedule.Entry

case class TestJobSchedule(queues: Map[Int, List[Entry]] = Map.empty, waitingList: List[Job] = List.empty) extends JobSchedule.State
