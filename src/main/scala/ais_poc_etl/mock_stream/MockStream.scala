package ais_poc_etl.mock_stream

import java.net.InetAddress

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.alpakka.ftp.scaladsl.Ftp
import akka.stream.alpakka.ftp.{FtpFile, FtpSettings}
import akka.stream.scaladsl.{Flow, Framing, Sink, Source}
import akka.util.ByteString

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class MockStream(implicit system: ActorSystem) {

  /**
   * Mocks a stream of data by iterating through all the files
   * on the FTP server then streaming each file, then splitting out
   * each CSV row, leaves in header rows to simulate corrupted records.
   * */

  val host = "ftp.ais.dk"
  val ftpSettings: FtpSettings = FtpSettings
    .create(InetAddress.getByName(host))

  // Split file contents out to single lines and map from bytes to text
  val line_splitter: Flow[ByteString, String, NotUsed] = Framing.delimiter(
    ByteString("\n"),
    maximumFrameLength = 1024,
    allowTruncation = true
  ).map(_.utf8String)

  //  Get all the.csv files we want to use to simulate our "stream"
  val retrieve_files: Future[Seq[FtpFile]] = Ftp.ls("/ais_data/", ftpSettings)
    .filter(ftpfile => ftpfile.name.contains(".csv"))
    .runWith(Sink.seq)

  Await.result(retrieve_files, 5.seconds)

  //  Order the files by their last modified
  // time to simulate a stream running-
  val sort_files: Seq[FtpFile] = Await
    .result(retrieve_files, 2.seconds)
    .sortBy(_.lastModified)

  // Stream each files contents in the collection
  val source =
    Source(sort_files)
      .flatMapConcat(file => {
        println("-- Processing File --")
        println(file.name)
        Ftp.fromPath(host, file.path)
          .via(line_splitter)
      })
}

object MockStream {
  def apply(implicit system: ActorSystem):
  MockStream = new MockStream()
}
