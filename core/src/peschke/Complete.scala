package peschke

/** Used as a replacement for returning Unit.
  *
  * The reason for this is to avoid certain type issues that come up in how
  * erasure interacts with the way the Scala compiler handles Unit.
  * Specifically: it transforms any method which returns Unit, but doesn't
  * actually return Unit, by inserting the unit value.
  *
  * Basically, this:
  * {{{
  * def method: Unit = 1
  * }}}
  *
  * Is actually this:
  * {{{
  * def method: Unit = {
  *   1
  *   ()
  * }
  * }}}
  *
  * The upshot of this is the compiler has a different view of the world than we
  * do.
  *
  * When refactoring something to have asynchronous side effect, care has to be
  * taken, or bad things can happen.
  * {{{
  * def getValue: Future[Int] = Future(1)
  * def getOtherValue: Future[Int] = Future(2)
  *
  * def doSideEffectSync(a: Int, b: Int): Unit = println(s"Done: \${a + b}")
  *
  * def syncVersion: Future[Unit] =
  *   for {
  *     a <- getValue
  *     b <- getOtherValue
  *   } yield doSideEffectSync(a, b)
  *
  * def doSideEffectAsync(a: Int, b: Int): Future[Unit] = Future {
  *   Thread.sleep(10000)
  *   println(s"Done: \${a + b}")
  * }
  *
  * def asyncVersion: Future[Unit] =
  *   for {
  *     a <- getValue
  *     b <- getOtherValue
  *   } yield {
  *     doSideEffectAsync(a, b)
  *     println("Returning")
  *   }
  *
  * println("Synchronous Version")
  * Await.result(syncVersion, Inf)
  *
  * println("\nAsync Version")
  * Await.result(asyncVersion, Inf)
  *
  * println("Finished")
  * }}}
  *
  * Results in this output:
  *
  * {{{
  * Synchronous Version
  * Done: 3
  *
  * Async Version
  * Returning
  * Finished
  * }}}
  *
  * The program completes before the asynchronous side effect, because it's not
  * tied to the enclosing Future.
  *
  * Replacing it with Complete avoids this issue, as the incorrect code won't
  * compile.
  */
sealed trait Complete {
  def upcast: Complete = this
}
object Complete extends Complete
