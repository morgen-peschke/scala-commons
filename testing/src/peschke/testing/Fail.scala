package peschke.testing

/** A typeclass that encodes the way a test framework can fail a test.
  *
  * @tparam Loc
  *   A framework-specific breadcrumb equivalent-ish to [[At]]
  */
trait Fail[Loc] {

  /** Fail a test with a message
    * @param msg
    *   the failure message
    * @param sourceInfo
    *   the source info of relevant input
    * @param at
    *   where the test failed
    */
  def apply[A](msg: String, sourceInfo: sourcecode.Text[A], at: Loc): Nothing

  /** Fail a test with a message, capturing an exception
    *
    * @param msg
    *   the failure message
    * @param sourceInfo
    *   the source info of relevant input
    * @param cause
    *   an exception that should be reported to the test framework
    * @param at
    *   where the test failed
    */
  def apply[A]
    (msg: String, sourceInfo: sourcecode.Text[A], cause: Throwable, at: Loc)
    : Nothing
}
