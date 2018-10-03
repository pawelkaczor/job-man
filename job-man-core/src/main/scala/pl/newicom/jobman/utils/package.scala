package pl.newicom.jobman

package object utils {
  implicit class ListMultiMap[A,B](map: Map[A, List[B]]) {
    def plus(key: A, value: B): Map[A, List[B]] =
      map + (key -> { value :: map.getOrElse(key, Nil) })

    def minus(key: A, value: B): Map[A, List[B]] = map.get(key) match {
      case None => map
      case Some(List(`value`)) => map - key
      case Some(list) => map + (key -> list.diff(List(value)))
    }

    def minus(key: A, n: Int): Map[A, List[B]] = map.get(key) match {
      case None => map
      case Some(list) => map + (key -> list.drop(n))
    }
  }
}
