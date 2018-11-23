package com.example

import java.io.ByteArrayInputStream

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.{complete, get, pathSingleSlash}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.Future

class Test extends WordSpec with Matchers with ScalatestRouteTest {

  private val testData = Array.fill(100)("a")
  private val testDataStr = testData.mkString("")

  //something is bad
  private def streamUsingOutputStream: Source[ByteString, _] = StreamConverters.asOutputStream()
    .mapMaterializedValue { outputStream =>
      Future {
        outputStream.write(testDataStr.getBytes())
        outputStream.close()
      }
    }


  //this works
  private def streamFromIterator: Source[ByteString, _] = {
    Source.fromIterator(() => testData.iterator).map(ByteString.apply)
  }


  //this works
  private def streamFromInputStream: Source[ByteString, _] = StreamConverters.fromInputStream(() => new ByteArrayInputStream(testDataStr.getBytes))

  private def route(source: Source[ByteString, _]): Route = get {
    pathSingleSlash {
      complete(HttpEntity(
        ContentTypes.`text/plain(UTF-8)`,
        source
      ))
    }
  }

  private def testUsingSource(source: Source[ByteString, _]): Unit = {
    (0 until 10).foreach { i =>
      withClue(s"Request number $i") {
        Get("/") ~> route(source) ~> check {
          responseAs[String] shouldEqual testDataStr
        }
      }
    }
  }

  "the length of responses should not be truncated" should {

    "using Source.FromIterator" in {
      testUsingSource(streamFromInputStream)
    }

    "using StreamConverters.fromInputStream" in {
      testUsingSource(streamFromInputStream)
    }

    "using StreamConverters.asOutputStream" in {
      testUsingSource(streamUsingOutputStream)
    }

  }
}
