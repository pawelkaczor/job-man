package pl.newicom.jobman.shared.command

trait HasExecutionJournalOffset {
  def executionJournalOffset: Long
  def jobId: String
}
