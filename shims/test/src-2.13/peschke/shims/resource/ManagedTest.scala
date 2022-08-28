package peschke.shims.resource

import cats.Id
import cats.data.Chain
import cats.syntax.all._
import org.scalatest.TryValues
import peschke.Complete
import peschke.UnitSpec
import peschke.shims.resource.TestResourceFactory.Record
import peschke.shims.resource.TestResourceFactory.ResourceBuilder

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import scala.util.Try

// OK to throw in this test, as the code needs to behave in the presence of exceptions
// scalafix:off DisableSyntax.throw
class ManagedIdTest extends UnitSpec {
  val manage: Managed.Factory[Id, Throwable] = Managed.factory[Id, Throwable]

  "Pure[Id,Throwable,*].use" should {
    "return the same resource each time" in {
      val factory =
        TestResourceFactory.builder[Int]
          .open(_.returning(1).canClose)
          .open(_.returning(2).canClose)
          .build

      val managed = manage.pure(factory.open())

      managed.use(_.retrieve) mustBe 1
      managed.use(_.retrieve) mustBe 1

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        0 -> Record.SuccessfullyRetrieved(1),
        0 -> Record.SuccessfullyRetrieved(1)
      )
    }

    "never close the resource" in {
      val factory =
        TestResourceFactory.builder[Int]
          .open(_.returning(1).unClosable)
          .build

      val managed = manage.pure(factory.open())

      managed.use(_.retrieve) mustBe 1

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        0 -> Record.SuccessfullyRetrieved(1)
      )
    }

    "throw open errors during creation" in {
      val factory =
        TestResourceFactory.builder[Int]
          .failToOpen(new NoSuchElementException("Nope!"))
          .build

      intercept[NoSuchElementException] {
        manage.pure(factory.open())
      }.getMessage mustBe "Nope!"

      factory.history mustBe List(
        0 -> Record.FailedToOpen("Nope!")
      )
    }

    "throw retrieve errors" in {
      val factory =
        TestResourceFactory.builder[Int]
          .open(_.throwing(new NoSuchElementException("Nope!")).canClose)
          .build

      val managed = manage.pure(factory.open())

      intercept[NoSuchElementException] {
        managed.use(_.retrieve)
      }.getMessage mustBe "Nope!"

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        0 -> Record.FailedToRetrieve("Nope!")
      )
    }

    "throw errors during use" in {
      val factory =
        TestResourceFactory.builder[Int]
          .open(_.returning(1).canClose)
          .build

      val managed = manage.pure(factory.open())

      intercept[NoSuchElementException] {
        managed
          .use { value =>
            throw new NoSuchElementException(s"Nope: ${value.retrieve}")
          }
      }
      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        0 -> Record.SuccessfullyRetrieved(1)
      )
    }
  }

  "Resource[Id,Throwable,_].use" should {
    "open a new resource each time" in {
      val factory =
        TestResourceFactory.builder[Int]
          .open(_.returning(1).canClose)
          .open(_.returning(2).canClose)
          .build

      val managed = manage.always(factory.open())

      managed.use(_.retrieve) mustBe 1
      managed.use(_.retrieve) mustBe 2
    }

    "correctly sequence open -> use -> close" in {
      val factory = TestResourceFactory.builder[Int].open(_.returning(1).canClose).build

      val managed = manage.always(factory.open())

      managed
        .use { handle =>
          factory.insertMarker("Before retrieve")
          val r = handle.retrieve
          factory.insertMarker("After retrieve")
          r
        } mustBe 1

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        -1 -> Record.Marker("Before retrieve"),
        0 -> Record.SuccessfullyRetrieved(1),
        -1 -> Record.Marker("After retrieve"),
        0 -> Record.SuccessfullyClosed()
      )
    }

    "close the resource if computation fails" in {
      val factory =
        TestResourceFactory
          .builder[Int]
          .open(_.throwing(new NoSuchElementException("Nope!")).canClose)
          .build

      val managed = manage.always(factory.open())

      intercept[NoSuchElementException] {
        managed.use(_.retrieve)
      }.getMessage mustBe "Nope!"

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        0 -> Record.FailedToRetrieve("Nope!"),
        0 -> Record.SuccessfullyClosed()
      )
    }

    "don't close or use a resource that failed to open" in {
      val factory =
        TestResourceFactory
          .builder[Int]
          .failToOpen(new NoSuchElementException("Nope!"))
          .build

      val managed = manage.always(factory.open())

      intercept[NoSuchElementException] {
        managed.use(_.retrieve)
      }.getMessage mustBe "Nope!"

      factory.history mustBe List(
        0 -> Record.FailedToOpen("Nope!")
      )
    }

    "throw if a resource fails to close" in {
      val factory = TestResourceFactory.builder[Int].open(_.returning(1).unClosable).build

      val managed = manage.always(factory.open())

      intercept[UnableToCloseException] {
        managed.use(_.retrieve)
      }

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        0 -> Record.SuccessfullyRetrieved(1),
        0 -> Record.FailedToClose()
      )
    }
  }

  "Mapped[Id,Throwable,_].use" should {
    "correctly sequence open -> use -> close" in {
      val factory = TestResourceFactory.builder[Int].open(_.returning(1).canClose).build

      val managed = manage.always(factory.open()).map { handle =>
        factory.insertMarker("Map")
        handle.retrieve * 2
      }

      managed
        .use { handle =>
          factory.insertMarker("Use")
          handle
        } mustBe 2

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        -1 -> Record.Marker("Map"),
        0 -> Record.SuccessfullyRetrieved(1),
        -1 -> Record.Marker("Use"),
        0 -> Record.SuccessfullyClosed()
      )
    }
  }

  "FlatMapped[Id,Throwable,_].use" should {
    "correctly sequence open -> use -> close" in {
      val factory =
        TestResourceFactory.builder[Int]
          .open(_.returning(1).canClose)
          .open(_.returning(2).canClose)
          .build

      val managed = manage.always(factory.open()).flatMap { outerHandle =>
        factory.insertMarker("FlatMap")
        val outer = outerHandle.retrieve
        manage.always(factory.open()).map { innerHandle =>
          factory.insertMarker("Map")
          (outer, innerHandle.retrieve)
        }
      }

      managed
        .use { handle =>
          factory.insertMarker("Use")
          handle
        } mustBe (1 -> 2)

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        -1 -> Record.Marker("FlatMap"),
        0 -> Record.SuccessfullyRetrieved(1),
        1 -> Record.SuccessfullyOpened(),
        -1 -> Record.Marker("Map"),
        1 -> Record.SuccessfullyRetrieved(2),
        -1 -> Record.Marker("Use"),
        1 -> Record.SuccessfullyClosed(),
        0 -> Record.SuccessfullyClosed()
      )
    }

    "close the outer resource, even if the inner resource fails to open" in {
      val factory =
        TestResourceFactory.builder[Int]
          .open(_.returning(1).canClose)
          .failToOpen(new NoSuchElementException("Nope!"))
          .build

      val managed = manage.always(factory.open()).flatMap { outerHandle =>
        val outer = outerHandle.retrieve
        manage.always(factory.open()).map { innerHandle =>
          (outer, innerHandle.retrieve)
        }
      }

      intercept[NoSuchElementException] {
        managed.use(identity)
      }.getMessage mustBe "Nope!"

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        0 -> Record.SuccessfullyRetrieved(1),
        1 -> Record.FailedToOpen("Nope!"),
        0 -> Record.SuccessfullyClosed()
      )
    }

    "close the outer resource, even if the inner resource fails during use" in {
      val factory =
        TestResourceFactory.builder[Int]
          .open(_.returning(1).canClose)
          .open(_.throwing(new NoSuchElementException("Nope!")).canClose)
          .build

      val managed = manage.always(factory.open()).flatMap { outerHandle =>
        val outer = outerHandle.retrieve
        manage.always(factory.open()).map { innerHandle =>
          (outer, innerHandle.retrieve)
        }
      }

      intercept[NoSuchElementException] {
        managed.use(identity)
      }

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        0 -> Record.SuccessfullyRetrieved(1),
        1 -> Record.SuccessfullyOpened(),
        1 -> Record.FailedToRetrieve("Nope!"),
        1 -> Record.SuccessfullyClosed(),
        0 -> Record.SuccessfullyClosed()
      )
    }

    "close the outer resource, even if the inner resource fails during close" in {
      val factory =
        TestResourceFactory.builder[Int]
          .open(_.returning(1).canClose)
          .open(_.returning(2).unClosable)
          .build

      val managed = manage.always(factory.open()).flatMap { outerHandle =>
        val outer = outerHandle.retrieve
        manage.always(factory.open()).map { innerHandle =>
          (outer, innerHandle.retrieve)
        }
      }

      intercept[UnableToCloseException] {
        managed.use(identity)
      }

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        0 -> Record.SuccessfullyRetrieved(1),
        1 -> Record.SuccessfullyOpened(),
        1 -> Record.SuccessfullyRetrieved(2),
        1 -> Record.FailedToClose(),
        0 -> Record.SuccessfullyClosed()
      )
    }
  }

  "Managed[Id,Throwable,_]" should {
    "behave correctly inside a for-comprehension" in {
      val factory =
        TestResourceFactory.builder[Int]
          .open(_.returning(1).canClose)
          .open(_.returning(2).canClose)
          .build

      val managed =
        for {
          r0 <- manage.always(factory.open())
          r1 <- manage.always(factory.open())
        } yield (r0.retrieve, r1.retrieve)

      val actual = managed.use {
        case (a, b) => a + b
      }

      actual mustBe 3

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        1 -> Record.SuccessfullyOpened(),
        0 -> Record.SuccessfullyRetrieved(1),
        1 -> Record.SuccessfullyRetrieved(2),
        1 -> Record.SuccessfullyClosed(),
        0 -> Record.SuccessfullyClosed()
      )
    }
  }
}

class ManagedEitherTest extends UnitSpec {
  implicit val mapExceptionsUsingMessage: ExceptionMapper[Either[String, *], String] =
    ExceptionMapper.usingApplicativeError[Either[String, *], String](_.getMessage.some)

  val manage: Managed.Factory[Either[String, *], String] = Managed.factory[Either[String, *], String]

  "Pure[Either[String,_],_].use" should {
    "return the same resource each time" in {
      val factory =
        TestResourceFactory.builder[Int]
          .open(_.returning(1).canClose)
          .open(_.returning(2).canClose)
          .build

      val managed = manage.pure(factory.open())

      managed.use(_.retrieve.asRight) mustBe 1.asRight
      managed.use(_.retrieve.asRight) mustBe 1.asRight

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        0 -> Record.SuccessfullyRetrieved(1),
        0 -> Record.SuccessfullyRetrieved(1)
      )
    }

    "never close the resource" in {
      val factory =
        TestResourceFactory.builder[Int]
          .open(_.returning(1).unClosable)
          .build

      val managed = manage.pure(factory.open())

      managed.use(_.retrieve.asRight) mustBe 1.asRight

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        0 -> Record.SuccessfullyRetrieved(1)
      )
    }

    "throw open errors during creation" in {
      val factory =
        TestResourceFactory.builder[Int]
          .failToOpen(new NoSuchElementException("Nope!"))
          .build

      intercept[NoSuchElementException] {
        manage.pure(factory.open())
      }.getMessage mustBe "Nope!"

      factory.history mustBe List(
        0 -> Record.FailedToOpen("Nope!")
      )
    }

    "return retrieve errors" in {
      val factory =
        TestResourceFactory.builder[Int]
          .open(_.throwing(new NoSuchElementException("Nope!")).canClose)
          .build

      val managed = manage.pure(factory.open())

      managed.use(_.retrieve.asRight) mustBe "Nope!".asLeft

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        0 -> Record.FailedToRetrieve("Nope!")
      )
    }

    "return errors during use" in {
      val factory =
        TestResourceFactory.builder[Int]
          .open(_.returning(1).canClose)
          .build

      val managed = manage.pure(factory.open())

      managed
        .use { value =>
          throw new NoSuchElementException(s"Nope: ${value.retrieve}")
        } mustBe "Nope: 1".asLeft

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        0 -> Record.SuccessfullyRetrieved(1)
      )
    }
  }

  "Resource[Either[E,_],E,_].use" should {
    "open a new resource each time" in {
      val factory =
        TestResourceFactory.builder[Int]
          .open(_.returning(1).canClose)
          .open(_.returning(2).canClose)
          .build

      val managed = manage.always(factory.open())

      managed.use(_.retrieve.asRight) mustBe 1.asRight
      managed.use(_.retrieve.asRight) mustBe 2.asRight
    }

    "correctly sequence open -> use -> close" in {
      val factory = TestResourceFactory.builder[Int].open(_.returning(1).canClose).build

      val managed = manage.always(factory.open())

      managed
        .use { handle =>
          factory.insertMarker("Before retrieve")
          val r = handle.retrieve.asRight
          factory.insertMarker("After retrieve")
          r
        } mustBe 1.asRight

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        -1 -> Record.Marker("Before retrieve"),
        0 -> Record.SuccessfullyRetrieved(1),
        -1 -> Record.Marker("After retrieve"),
        0 -> Record.SuccessfullyClosed()
      )
    }

    "close the resource if computation throws an exception" in {
      val factory =
        TestResourceFactory
          .builder[Int]
          .open(_.throwing(new NoSuchElementException("Nope!")).canClose)
          .build

      val managed = manage.always(factory.open())

      managed.use(_.retrieve.asRight) mustBe "Nope!".asLeft

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        0 -> Record.FailedToRetrieve("Nope!"),
        0 -> Record.SuccessfullyClosed()
      )
    }

    "close the resource if computation fails" in {
      val factory =
        TestResourceFactory
          .builder[Int]
          .open(_.returning(1).canClose)
          .build

      val managed = manage.always(factory.open())

      managed.use(r => s"${r.retrieve}".asLeft) mustBe "1".asLeft

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        0 -> Record.SuccessfullyRetrieved(1),
        0 -> Record.SuccessfullyClosed()
      )
    }

    "don't close or use a resource that failed to open" in {
      val factory =
        TestResourceFactory
          .builder[Int]
          .failToOpen(new NoSuchElementException("Nope!"))
          .build

      val managed = manage.always(factory.open())

      managed.use(_.retrieve.asRight) mustBe "Nope!".asLeft

      factory.history mustBe List(
        0 -> Record.FailedToOpen("Nope!")
      )
    }
  }

  "Mapped[Either[E,_],E,_].use" should {
    "correctly sequence open -> use -> close" in {
      val factory = TestResourceFactory.builder[Int].open(_.returning(1).canClose).build

      val managed = manage.always(factory.open()).map { handle =>
        factory.insertMarker("Map")
        handle.retrieve * 2
      }

      managed
        .use { handle =>
          factory.insertMarker("Use")
          handle.asRight
        } mustBe 2.asRight

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        -1 -> Record.Marker("Map"),
        0 -> Record.SuccessfullyRetrieved(1),
        -1 -> Record.Marker("Use"),
        0 -> Record.SuccessfullyClosed()
      )
    }
  }

  "FlatMapped[Either[E,_],E,_].use" should {
    "correctly sequence open -> use -> close" in {
      val factory =
        TestResourceFactory.builder[Int]
          .open(_.returning(1).canClose)
          .open(_.returning(2).canClose)
          .build

      val managed = manage.always(factory.open()).flatMap { outerHandle =>
        factory.insertMarker("FlatMap")
        val outer = outerHandle.retrieve
        manage.always(factory.open()).map { innerHandle =>
          factory.insertMarker("Map")
          (outer, innerHandle.retrieve)
        }
      }

      managed
        .use { handle =>
          factory.insertMarker("Use")
          handle.asRight
        } mustBe (1 -> 2).asRight

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        -1 -> Record.Marker("FlatMap"),
        0 -> Record.SuccessfullyRetrieved(1),
        1 -> Record.SuccessfullyOpened(),
        -1 -> Record.Marker("Map"),
        1 -> Record.SuccessfullyRetrieved(2),
        -1 -> Record.Marker("Use"),
        1 -> Record.SuccessfullyClosed(),
        0 -> Record.SuccessfullyClosed()
      )
    }

    "close the outer resource, even if the inner resource fails to open" in {
      val factory =
        TestResourceFactory.builder[Int]
          .open(_.returning(1).canClose)
          .failToOpen(new NoSuchElementException("Nope!"))
          .build

      val managed = manage.always(factory.open()).flatMap { outerHandle =>
        val outer = outerHandle.retrieve
        manage.always(factory.open()).map { innerHandle =>
          (outer, innerHandle.retrieve)
        }
      }

        managed.use(_.asRight) mustBe "Nope!".asLeft

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        0 -> Record.SuccessfullyRetrieved(1),
        1 -> Record.FailedToOpen("Nope!"),
        0 -> Record.SuccessfullyClosed()
      )
    }

    "close the outer resource, even if the inner resource fails during use" in {
      val factory =
        TestResourceFactory.builder[Int]
          .open(_.returning(1).canClose)
          .open(_.throwing(new NoSuchElementException("Nope!")).canClose)
          .build

      val managed = manage.always(factory.open()).flatMap { outerHandle =>
        val outer = outerHandle.retrieve
        manage.always(factory.open()).map { innerHandle =>
          (outer, innerHandle.retrieve)
        }
      }

      managed.use(_.asRight) mustBe "Nope!".asLeft

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        0 -> Record.SuccessfullyRetrieved(1),
        1 -> Record.SuccessfullyOpened(),
        1 -> Record.FailedToRetrieve("Nope!"),
        1 -> Record.SuccessfullyClosed(),
        0 -> Record.SuccessfullyClosed()
      )
    }

    "close the outer resource, even if the inner resource fails during close" in {
      val factory =
        TestResourceFactory.builder[Int]
          .open(_.returning(1).canClose)
          .open(_.returning(2).unClosable)
          .build

      val managed = manage.always(factory.open()).flatMap { outerHandle =>
        val outer = outerHandle.retrieve
        manage.always(factory.open()).map { innerHandle =>
          (outer, innerHandle.retrieve)
        }
      }

      managed.use(_.asRight) mustBe "Resource cannot be closed".asLeft

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        0 -> Record.SuccessfullyRetrieved(1),
        1 -> Record.SuccessfullyOpened(),
        1 -> Record.SuccessfullyRetrieved(2),
        1 -> Record.FailedToClose(),
        0 -> Record.SuccessfullyClosed()
      )
    }
  }

  "Managed[Either[E,*], _]" should {
    "behave correctly with mapN" in {
      val factory =
        TestResourceFactory.builder[Int]
          .open(_.returning(1).canClose)
          .open(_.returning(2).canClose)
          .build

      val managed = (manage.always(factory.open()), manage.always(factory.open())).mapN {
        (r0, r1) => (r0.retrieve, r1.retrieve)
      }

      val actual = managed.use {
        case (a, b) => (a + b).asRight
      }

      actual mustBe 3.asRight

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        1 -> Record.SuccessfullyOpened(),
        0 -> Record.SuccessfullyRetrieved(1),
        1 -> Record.SuccessfullyRetrieved(2),
        1 -> Record.SuccessfullyClosed(),
        0 -> Record.SuccessfullyClosed()
      )
    }

    "behave correctly inside a for-comprehension" in {
      val factory =
        TestResourceFactory.builder[Int]
          .open(_.returning(1).canClose)
          .open(_.returning(2).canClose)
          .build

      val managed =
        for {
          r0 <- manage.always(factory.open())
          r1 <- manage.always(factory.open())
        } yield (r0.retrieve, r1.retrieve)

      val actual = managed.use {
        case (a, b) => (a + b).asRight
      }

      actual mustBe 3.asRight

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        1 -> Record.SuccessfullyOpened(),
        0 -> Record.SuccessfullyRetrieved(1),
        1 -> Record.SuccessfullyRetrieved(2),
        1 -> Record.SuccessfullyClosed(),
        0 -> Record.SuccessfullyClosed()
      )
    }
  }
}

class ManagedTryTest extends UnitSpec with TryValues {
  val manage: Managed.Factory[Try, Throwable] = Managed.factory[Try, Throwable]
  
  "Pure[Try,Throwable,*].use" should {
    "return the same resource each time" in {
      val factory =
        TestResourceFactory.builder[Int]
          .open(_.returning(1).canClose)
          .open(_.returning(2).canClose)
          .build

      val managed = manage.pure(factory.open())

      managed.use(r => Try(r.retrieve)).success.value mustBe 1
      managed.use(r => Try(r.retrieve)).success.value mustBe 1

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        0 -> Record.SuccessfullyRetrieved(1),
        0 -> Record.SuccessfullyRetrieved(1)
      )
    }

    "never close the resource" in {
      val factory =
        TestResourceFactory.builder[Int]
          .open(_.returning(1).unClosable)
          .build

      val managed = manage.pure(factory.open())

      managed.use(r => Try(r.retrieve)).success.value mustBe 1

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        0 -> Record.SuccessfullyRetrieved(1)
      )
    }

    "throw open errors during creation" in {
      val factory =
        TestResourceFactory.builder[Int]
          .failToOpen(new NoSuchElementException("Nope!"))
          .build

      intercept[NoSuchElementException] {
        manage.pure(factory.open())
      }.getMessage mustBe "Nope!"

      factory.history mustBe List(
        0 -> Record.FailedToOpen("Nope!")
      )
    }

    "close over retrieve errors" in {
      val factory =
        TestResourceFactory.builder[Int]
          .open(_.throwing(new NoSuchElementException("Nope!")).canClose)
          .build

      val managed = manage.pure(factory.open())

      managed.use(r => Try(r.retrieve)).failure.exception.getMessage mustBe "Nope!"

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        0 -> Record.FailedToRetrieve("Nope!")
      )
    }

    "throw errors during use" in {
      val factory =
        TestResourceFactory.builder[Int]
          .open(_.returning(1).canClose)
          .build

      val managed = manage.pure(factory.open())

      managed
        .use { value =>
            throw new NoSuchElementException(s"Nope: ${value.retrieve}")
        }
        .failure
        .exception
        .getMessage mustBe "Nope: 1"

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        0 -> Record.SuccessfullyRetrieved(1)
      )
    }
  }

  "Resource[Future,Throwable,_].use" should {
    "open a new resource each time" in {
      val factory =
        TestResourceFactory.builder[Int]
          .open(_.returning(1).canClose)
          .open(_.returning(2).canClose)
          .build

      val managed = manage.always(factory.open())

      managed.use(r => Try(r.retrieve)).success.value mustBe 1
      managed.use(r => Try(r.retrieve)).success.value mustBe 2
    }

    "correctly sequence open -> use -> close" in {
      val factory = TestResourceFactory.builder[Int].open(_.returning(1).canClose).build

      val managed = manage.always(factory.open())

      managed
        .use { handle =>
          factory.insertMarker("Before retrieve")
          val r = Try(handle.retrieve)
          factory.insertMarker("After retrieve")
          r
        }.success.value mustBe 1

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        -1 -> Record.Marker("Before retrieve"),
        0 -> Record.SuccessfullyRetrieved(1),
        -1 -> Record.Marker("After retrieve"),
        0 -> Record.SuccessfullyClosed()
      )
    }

    "close the resource if computation fails" in {
      val factory =
        TestResourceFactory
          .builder[Int]
          .open(_.throwing(new NoSuchElementException("Nope!")).canClose)
          .build

      val managed = manage.always(factory.open())

        managed
          .use(r => Try(r.retrieve))
          .failure
          .exception
          .getMessage mustBe "Nope!"

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        0 -> Record.FailedToRetrieve("Nope!"),
        0 -> Record.SuccessfullyClosed()
      )
    }

    "don't close or use a resource that failed to open" in {
      val factory =
        TestResourceFactory
          .builder[Int]
          .failToOpen(new NoSuchElementException("Nope!"))
          .build

      val managed = manage.always(factory.open())

      managed.use(r => Try(r.retrieve))
        .failure
        .exception
        .getMessage mustBe "Nope!"

      factory.history mustBe List(
        0 -> Record.FailedToOpen("Nope!")
      )
    }

    "throw if a resource fails to close" in {
      val factory = TestResourceFactory.builder[Int].open(_.returning(1).unClosable).build

      val managed = manage.always(factory.open())

      managed
        .use(r => Try(r.retrieve))
        .failure
        .exception
        .getMessage mustBe "Resource cannot be closed"

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        0 -> Record.SuccessfullyRetrieved(1),
        0 -> Record.FailedToClose()
      )
    }
  }

  "Mapped[Try,Throwable,_].use" should {
    "correctly sequence open -> use -> close" in {
      val factory = TestResourceFactory.builder[Int].open(_.returning(1).canClose).build

      val managed = manage.always(factory.open()).map { handle =>
        factory.insertMarker("Map")
        handle.retrieve * 2
      }

      managed
        .use { handle =>
          factory.insertMarker("Use")
          Try(handle)
        }.success.value mustBe 2

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        -1 -> Record.Marker("Map"),
        0 -> Record.SuccessfullyRetrieved(1),
        -1 -> Record.Marker("Use"),
        0 -> Record.SuccessfullyClosed()
      )
    }
  }

  "FlatMapped[Try,Throwable,_].use" should {
    "correctly sequence open -> use -> close" in {
      val factory =
        TestResourceFactory.builder[Int]
          .open(_.returning(1).canClose)
          .open(_.returning(2).canClose)
          .build

      val managed = manage.always(factory.open()).flatMap { outerHandle =>
        factory.insertMarker("FlatMap")
        val outer = outerHandle.retrieve
        manage.always(factory.open()).map { innerHandle =>
          factory.insertMarker("Map")
          (outer, innerHandle.retrieve)
        }
      }

      managed
        .use { handle =>
          factory.insertMarker("Use")
          Try(handle)
        }
        .success
        .value mustBe (1 -> 2)

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        -1 -> Record.Marker("FlatMap"),
        0 -> Record.SuccessfullyRetrieved(1),
        1 -> Record.SuccessfullyOpened(),
        -1 -> Record.Marker("Map"),
        1 -> Record.SuccessfullyRetrieved(2),
        -1 -> Record.Marker("Use"),
        1 -> Record.SuccessfullyClosed(),
        0 -> Record.SuccessfullyClosed()
      )
    }

    "close the outer resource, even if the inner resource fails to open" in {
      val factory =
        TestResourceFactory.builder[Int]
          .open(_.returning(1).canClose)
          .failToOpen(new NoSuchElementException("Nope!"))
          .build

      val managed = manage.always(factory.open()).flatMap { outerHandle =>
        val outer = outerHandle.retrieve
        manage.always(factory.open()).map { innerHandle =>
          (outer, innerHandle.retrieve)
        }
      }

      managed.use(Try(_)).failure.exception.getMessage mustBe "Nope!"

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        0 -> Record.SuccessfullyRetrieved(1),
        1 -> Record.FailedToOpen("Nope!"),
        0 -> Record.SuccessfullyClosed()
      )
    }

    "close the outer resource, even if the inner resource fails during use" in {
      val factory =
        TestResourceFactory.builder[Int]
          .open(_.returning(1).canClose)
          .open(_.throwing(new NoSuchElementException("Nope!")).canClose)
          .build

      val managed = manage.always(factory.open()).flatMap { outerHandle =>
        val outer = outerHandle.retrieve
        manage.always(factory.open()).map { innerHandle =>
          (outer, innerHandle.retrieve)
        }
      }

      managed.use(Try(_)).failure.exception.getMessage mustBe "Nope!"

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        0 -> Record.SuccessfullyRetrieved(1),
        1 -> Record.SuccessfullyOpened(),
        1 -> Record.FailedToRetrieve("Nope!"),
        1 -> Record.SuccessfullyClosed(),
        0 -> Record.SuccessfullyClosed()
      )
    }

    "close the outer resource, even if the inner resource fails during close" in {
      val factory =
        TestResourceFactory.builder[Int]
          .open(_.returning(1).canClose)
          .open(_.returning(2).unClosable)
          .build

      val managed = manage.always(factory.open()).flatMap { outerHandle =>
        val outer = outerHandle.retrieve
        manage.always(factory.open()).map { innerHandle =>
          (outer, innerHandle.retrieve)
        }
      }

      managed.use(Try(_)).failure.exception.getMessage mustBe "Resource cannot be closed"

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        0 -> Record.SuccessfullyRetrieved(1),
        1 -> Record.SuccessfullyOpened(),
        1 -> Record.SuccessfullyRetrieved(2),
        1 -> Record.FailedToClose(),
        0 -> Record.SuccessfullyClosed()
      )
    }
  }

  "Managed[Try,Throwable,_]" should {
    "behave correctly inside a for-comprehension" in {
      val factory =
        TestResourceFactory.builder[Int]
          .open(_.returning(1).canClose)
          .open(_.returning(2).canClose)
          .build

      val managed =
        for {
          r0 <- manage.always(factory.open())
          r1 <- manage.always(factory.open())
        } yield (r0.retrieve, r1.retrieve)

      val actual = managed.use {
        case (a, b) => Try(a + b)
      }

      actual.success.value mustBe 3

      factory.history mustBe List(
        0 -> Record.SuccessfullyOpened(),
        1 -> Record.SuccessfullyOpened(),
        0 -> Record.SuccessfullyRetrieved(1),
        1 -> Record.SuccessfullyRetrieved(2),
        1 -> Record.SuccessfullyClosed(),
        0 -> Record.SuccessfullyClosed()
      )
    }
  }
}

/**
 * A closeable test resource that reports it's history back to it's [[TestResourceFactory]]
 *
 * This could be mocks, but despite the initial cost, this should be much easier to work with.
 */
class TestResource[A](behavior: Either[Throwable, A],
                      canClose: Boolean,
                      reportAction: Record[A] => Complete) {
  private val closed: AtomicBoolean = new AtomicBoolean(false)

  /**
   * Depending on the configured behavior, return a value or throw an exception.
   *
   * Will always throw an exception if called after [[close()]]
   */
  def retrieve: A =
    if (closed.get()) {
      val ex = new AlreadyClosedException
      reportAction(Record.RetrieveAttemptedAfterClose())
      throw ex
    } else
      behavior match {
        case Right(value) =>
          reportAction(Record.SuccessfullyRetrieved(value))
          value
        case Left(error) =>
          reportAction(Record.FailedToRetrieve(error.getMessage))
          throw error
      }

  /**
   * Depending on the configured behavior, return a [[Complete]] or throw an exception.
   *
   * Will always throw an exception if called more than once.
   */
  def close(): Complete =
    if (closed.getAndSet(true)) {
      reportAction(Record.CloseAttemptedAfterClose())
      throw new AlreadyClosedException
    } else if (canClose) {
      reportAction(Record.SuccessfullyClosed())
      Complete
    } else {
      val ex = new UnableToCloseException
      reportAction(Record.FailedToClose())
      throw ex
    }
}
object TestResource {
  implicit def idCloser[A]: Closer[Id, TestResource[A]] = _.close()
  implicit def eitherCloser[A](implicit EM: ExceptionMapper[Either[String,*], String])
  : Closer[Either[String, *], TestResource[A]] = r => EM.catchNonFatal(r.close())
  implicit def tryCloser[A]: Closer[Try, TestResource[A]] = r => Try(r.close())
}

/**
 * A configurable factory which returns [[TestResource]] instances
 */
class TestResourceFactory[A](builders: Vector[Either[Throwable, ResourceBuilder.Ready[A]]]) {
  private val operationHistory: AtomicReference[Chain[(Int, Record[A])]] =
    new AtomicReference(Chain.empty)

  private def record(resourceId: Int, record: Record[A]): Complete = {
    operationHistory.updateAndGet(_.append(resourceId -> record))
    Complete
  }

  private val resources = builders.map(_.map(_.behavior)).zipWithIndex.iterator

  /**
   * Depending on the behavior specified during building, return a [[TestResource]] or throw an exception.
   *
   * If [[open()]] is called more times than resources were specified, throws an [[OutOfElementsException]]
   */
  def open(): TestResource[A] = resources.nextOption() match {
    case Some((Right((behavior, canClose)), resourceId)) =>
      record(resourceId, Record.SuccessfullyOpened())
      new TestResource[A](
        behavior,
        canClose,
        record(resourceId, _)
      )
    case Some((Left(openFailure), resourceId)) =>
      record(resourceId, Record.FailedToOpen(openFailure.getMessage))
      throw openFailure
    case None =>
      record(Int.MaxValue, Record.RanOutOfResources())
      throw new OutOfElementsException
  }

  /**
   * Retrieve a list of the recorded history records
   *
   * The [[Int]] values are the [[TestResource]] ids, which are monotonically increasing indexes.
   */
  def history: List[(Int, Record[A])] = operationHistory.get.toList

  /**
   * Insert a record in the history independent of any [[TestResource]].
   *
   * This can be handy for checking the sequence of actions related to [[Managed.use()]]
   */
  def insertMarker(tag: String): Complete = record(-1, Record.Marker(tag))
}
object TestResourceFactory {
  /**
   * Initialize a builder
   *
   * Note: methods called on each instance are ordered, and will produce [[TestResource]] instances in the same
   * order they were specified.
   */
  def builder[A]: FactoryBuilder[A] = new FactoryBuilder[A](Chain.empty)

  final class FactoryBuilder[A](private val resources: Chain[Either[Throwable, ResourceBuilder.Ready[A]]]) extends AnyVal {
    /**
     * Specify a [[TestResource]] which will be successfully opened.
     *
     * The compiler will complain if the [[ResourceBuilder]] is not fully configured
     */
    def open(f: ResourceBuilder.Uninitialized[A] => ResourceBuilder.Ready[A]): FactoryBuilder[A] =
      new FactoryBuilder[A](resources.append(f(ResourceBuilder.builder[A]).asRight))

    /**
     * Specify an attempt to open a resource which will fail.
     */
    def failToOpen(throwable: Throwable): FactoryBuilder[A] =
      new FactoryBuilder[A](resources.append(throwable.asLeft))

    /**
     * Build and return the [[TestResourceFactory]]
     */
    def build: TestResourceFactory[A] = new TestResourceFactory[A](resources.toVector)
  }

  /**
   * A configurable builder for [[TestResource]] instances.
   */
  final class ResourceBuilder[A, B <: Option[Either[Throwable, A]], C <: Option[Boolean]](private[TestResourceFactory] val valueOrException: B,
                                                                                          private[TestResourceFactory] val canClose: C)
  object ResourceBuilder {
    def builder[A]: Uninitialized[A] = new ResourceBuilder[A, None.type, None.type](None, None)

    type Uninitialized[A] = ResourceBuilder[A, None.type, None.type ]
    type Ready[A] = ResourceBuilder[A, Some[Either[Throwable, A]], Some[Boolean]]

    implicit final class RetrieveBehaviorOps[A, C <: Option[Boolean]](private val builder: ResourceBuilder[A, None.type, C]) extends AnyVal {
      /**
       * Indicates that calling [[TestResource.retrieve]] should return a value
       */
      def returning(value: A): ResourceBuilder[A, Some[Either[Throwable, A]], C] =
        new ResourceBuilder[A, Some[Either[Throwable, A]], C](Some(value.asRight), builder.canClose)

      /**
       * Indicates that calling [[TestResource.retrieve]] should throw an exception
       */
      def throwing(failure: Exception): ResourceBuilder[A, Some[Either[Throwable, A]], C] =
        new ResourceBuilder[A, Some[Either[Throwable, A]], C](Some(failure.asLeft), builder.canClose)
    }

    implicit final class CloseBehaviorOps[A, B <: Option[Either[Throwable, A]]](private val builder: ResourceBuilder[A, B, None.type]) extends AnyVal {
      /**
       * Indicates that calling [[TestResource.close]] should succeed
       */
      def canClose: ResourceBuilder[A, B, Some[Boolean]] =
        new ResourceBuilder[A, B, Some[Boolean]](builder.valueOrException, Some(true))

      /**
       * Indicates that calling [[TestResource.close()]] should fail
       */
      def unClosable: ResourceBuilder[A, B, Some[Boolean]] =
        new ResourceBuilder[A, B, Some[Boolean]](builder.valueOrException, Some(false))
    }

    implicit final class ReadyOps[A](private val builder: Ready[A]) extends AnyVal {
      /**
       * Return the configured behavior for the [[TestResource]]
       * @return
       */
      def behavior: (Either[Throwable, A], Boolean) = (builder.valueOrException.value, builder.canClose.value)
    }
  }

  /**
   * A record of what happened inside a [[TestResource]]
   */
  sealed abstract class Record[A] extends Product with Serializable
  object Record {
    // 'ignored: Boolean = true' is a workaround for https://github.com/scalameta/scalafmt/issues/3304
    final case class SuccessfullyOpened[A](ignored: Boolean = true) extends Record[A]
    final case class FailedToOpen[A](error: String) extends Record[A]
    final case class SuccessfullyRetrieved[A](value: A) extends Record[A]
    final case class FailedToRetrieve[A](error: String) extends Record[A]
    final case class RetrieveAttemptedAfterClose[A](ignored: Boolean = true) extends Record[A]
    final case class SuccessfullyClosed[A](ignored: Boolean = true) extends Record[A]
    final case class FailedToClose[A](ignored: Boolean = true) extends Record[A]
    final case class CloseAttemptedAfterClose[A](ignored: Boolean = true) extends Record[A]
    final case class RanOutOfResources[A](ignored: Boolean = true) extends Record[A]
    final case class Marker[A](tag: String) extends Record[A]
  }
}

class OutOfElementsException extends IllegalStateException("Ran out of elements")
class AlreadyClosedException extends IllegalStateException("Resource already closed")
class UnableToCloseException extends IllegalStateException("Resource cannot be closed")