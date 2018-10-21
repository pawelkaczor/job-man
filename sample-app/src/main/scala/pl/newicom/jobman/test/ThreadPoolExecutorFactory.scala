package pl.newicom.jobman.test

import java.util.concurrent.Executors.newFixedThreadPool
import java.util.concurrent.ThreadPoolExecutor

object ThreadPoolExecutorFactory {

  def threadPoolExecutor(nThreads: Int, threadNameFormat: String): ThreadPoolExecutor = {
    val threadFactory = new ThreadFactoryBuilder().setNameFormat(threadNameFormat).setPriority(Thread.MAX_PRIORITY).build
    newFixedThreadPool(nThreads, threadFactory).asInstanceOf[ThreadPoolExecutor]
  }

}
